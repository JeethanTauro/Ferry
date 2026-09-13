# Ferry — Ingestion Pipeline

Ferry's ingestion layer receives webhook events from external providers, authenticates them, persists them reliably, and asynchronously hands them off to RabbitMQ workers for delivery.

## Architecture

```text
External Provider
      │
      │ POST /webhooks/{endpointId}
      ▼
WebhookController
      │
      ▼
WebhookEventService
      │
      ├── Find WebhookEndpoint
      ├── Check ACTIVE
      ├── Decrypt secret
      ├── Verify provider signature
      │
      ├── Save WebhookEvent ──────┐
      ├── Increment Usage         │ Transaction
      └── Save OutboxEvent ───────┘
                    │
                    ▼
             PostgreSQL
                    │
             @Scheduled Polling
                    ▼
             PublisherService
                    │
              JOIN FETCH
                    │
                    ▼
              RabbitTemplate
                    │
              JSON conversion
                    ▼
             RabbitMQ Exchange
                    │
               Routing Key
                    ▼
                 Queue
                    │
                    ▼
                Workers
```

---

## 1. Endpoint Creation

Users create a Ferry endpoint with:

* `name`
* `destinationUrl`
* `provider`

Ferry generates:

* **Endpoint ID** — cryptographically random and URL-safe.
* **Secret** — cryptographically random 32-character token.

The secret is **encrypted before being stored in PostgreSQL**. The plaintext secret is returned to the user so that the external provider can use it for webhook signing.

```text
Plain secret
    ↓
EncryptionService
    ↓
Encrypted secret → PostgreSQL
```

The corresponding `WebhookUsage` record is also created with counters initialized to `0`.

### Decision

**Encrypt rather than hash the secret** because Ferry needs to recover the original secret later to verify incoming HMAC signatures. A hash would be one-way and therefore unsuitable.

---

## 2. Webhook Reception

External providers send:

```text
POST /webhooks/{endpointId}
```

The controller extracts:

* `endpointId` from the URL
* HTTP headers
* raw request body

The raw body is important because providers such as GitHub calculate their signature over the exact payload bytes.

---

## 3. Provider-Specific Signature Verification

Ferry determines the provider from the stored `WebhookEndpoint`.

Currently:

```text
Provider
└── GITHUB
```

The corresponding verifier extracts the provider-specific signature header:

```text
X-Hub-Signature-256
```

and computes:

```text
HMAC-SHA256(payload, decryptedSecret)
```

The calculated signature is compared against the provider's signature using a **constant-time comparison**.

### Decision

Signature verification happens **before persistence** so unauthenticated webhook requests are rejected rather than becoming stored events.

The verifier is abstracted behind:

```text
WebhookSignatureVerifier
```

so additional providers can be added without changing the ingestion flow.

---

## 4. Transactional Persistence

After successful authentication, three operations happen inside one transaction:

```text
WebhookEvent
    ↓
eventsReceived++
    ↓
OutboxEvent
```

### `WebhookEvent`

Stores the actual incoming event:

```text
eventId
endpointId
payload
headers
receivedAt
status
retry information
```

Initial status:

```text
PENDING
```

### `WebhookUsage`

Tracks endpoint-level statistics such as:

```text
eventsReceived
eventsDelivered
eventsFailed
```

### `OutboxEvent`

Contains the ID of the `WebhookEvent` that needs to be published to RabbitMQ.

Initial status:

```text
PENDING
```

### Decision — Transaction

The event and outbox record must be committed atomically.

Without a transaction:

```text
WebhookEvent saved ✓
OutboxEvent failed ✗
```

The event would exist in PostgreSQL but would never reach RabbitMQ.

With a transaction:

```text
Event + Usage + Outbox
        ↓
   all commit
        OR
   all rollback
```

---

# 5. Transactional Outbox

Ferry does **not** publish directly to RabbitMQ while processing the incoming webhook.

Instead:

```text
Webhook request
      ↓
PostgreSQL
      ↓
WebhookEvent + OutboxEvent
      ↓
Scheduled Publisher
      ↓
RabbitMQ
```

The outbox acts as a durable handoff between PostgreSQL and RabbitMQ.

### Decision

This prevents the classic failure:

```text
Save event ✓
Publish RabbitMQ ✗
```

The event remains represented by a `PENDING` outbox record and can be retried by the publisher.

---

## 6. Outbox Polling

`PublisherService` periodically polls:

```text
OutboxEvent.status = PENDING
```

using a `JOIN FETCH` to retrieve the associated `WebhookEvent` in the same query.

### Decision — `JOIN FETCH`

Without it:

```text
1 query → pending outbox events
N queries → individual webhook events
```

This creates an **N+1 query problem**.

With `JOIN FETCH`:

```text
1 query → OutboxEvent + WebhookEvent
```

---

## 7. RabbitMQ Publishing

The publisher sends the `WebhookEvent` through `RabbitTemplate`.

A Jackson JSON message converter serializes the Java object into JSON:

```text
WebhookEvent
     ↓
Jackson
     ↓
JSON
     ↓
RabbitMQ
```

Messages are published to:

```text
webhook_exchange
```

using a routing key such as:

```text
routing.key.webhook
```

The exchange routes the message to the webhook queue.

---

## 8. Publisher Confirm

Ferry uses RabbitMQ **publisher confirms**.

```text
Publisher
    ↓
RabbitMQ
    ↓
ACK / NACK
```

If RabbitMQ confirms the message:

```text
OutboxEvent
PENDING → PUBLISHED
```

If RabbitMQ rejects it:

```text
OutboxEvent remains PENDING
```

and the next polling cycle can retry it.

### Important

`PUBLISHED` means:

> RabbitMQ accepted the message.

It does **not** mean:

> The consumer successfully delivered the webhook to the user's destination.

That responsibility belongs to the worker layer.

---

## Current Responsibility Boundary

```text
                 FERRY INGESTION
                       │
External webhook ──────┤
                       │
                       ▼
                Authentication
                       ↓
                  PostgreSQL
                       ↓
                Transactional
                   Outbox
                       ↓
                  RabbitMQ
                       │
═══════════════════════╪══════════════════
                       │
                 DELIVERY WORKERS
                       ↓
              HTTP destination
                       ↓
             Retry / Backoff / DLQ
```

**Ingestion's job ends when the event is durably persisted and successfully handed to RabbitMQ.** Worker-side delivery, retries, exponential backoff and DLQ handling are intentionally separated from ingestion.

