# Ferry

Ferry is an asynchronous webhook ingestion and delivery platform built with **Java and Spring Boot**. It provides users with unique webhook endpoints, authenticates incoming provider webhooks, durably persists events, publishes them through RabbitMQ, and reliably delivers them to configured destination URLs with retries and DLQ handling.

## Architecture

```text
                    ┌──────────────────────┐
                    │ External Provider    │
                    │ GitHub (current)     │
                    └──────────┬───────────┘
                               │ HTTP POST
                               ▼
                 ┌─────────────────────────────┐
                 │ Ferry Ingestion Service     │
                 │                             │
                 │ Webhook Controller          │
                 │        ↓                    │
                 │ WebhookEventService         │
                 │  • endpoint lookup          │
                 │  • active-state check       │
                 │  • secret decryption       │
                 │  • HMAC verification        │
                 │  • event persistence        │
                 │  • usage accounting         │
                 │  • outbox creation          │
                 └──────────────┬──────────────┘
                                │
                         PostgreSQL transaction
                                │
                ┌───────────────┴────────────────┐
                │                                │
                ▼                                ▼
        webhook_events                    outbox_events
                                                 │
                                                 ▼
                                      Scheduled Publisher
                                                 │
                                                 ▼
                                      ┌──────────────────┐
                                      │ RabbitMQ         │
                                      │ webhook_exchange │
                                      └────────┬─────────┘
                                               │
                                               ▼
                                         webhook queue
                                               │
                                               ▼
                                  ┌────────────────────────┐
                                  │ Delivery Worker        │
                                  │                        │
                                  │ Consume → HTTP POST   │
                                  │       ↓                │
                                  │ classify result        │
                                  │       ↓                │
                                  │ Delivered / Retry /    │
                                  │ Failed / DLQ           │
                                  └───────┬────────┬───────┘
                                          │        │
                              retry       │        │ terminal failure
                                          ▼        ▼
                                  TTL retry queues  DLQ
                                          │
                                          ▼
                                   webhook exchange
                                          │
                                          └──► worker
```

### Services

**Ferry Ingestion Service**
- User-facing endpoint management API.
- Receives provider webhooks.
- Authenticates and persists events.
- Maintains the transactional outbox.
- Publishes durable events to RabbitMQ.

**Ferry Delivery Worker**
- Consumes events asynchronously.
- Performs outbound HTTP delivery.
- Classifies destination responses.
- Handles bounded retries with exponential backoff.
- Sends exhausted events to the DLQ.
- Updates delivery status and usage counters.

## Request Flow

### 1. Endpoint creation

```text
User
 ↓
POST /api/v1/endpoints
 ↓
Generate endpoint ID + secret
 ↓
Encrypt secret
 ↓
Persist endpoint + usage counters
 ↓
Return endpoint URL + plaintext secret
```

The generated secret is returned to the user **only during creation**. The persisted value is encrypted.

### 2. Incoming webhook

```text
Provider
 ↓
POST /webhooks/{endpointId}
 ↓
Find endpoint
 ↓
Check endpoint is active
 ↓
Decrypt stored secret
 ↓
Verify provider signature
 ↓
Persist WebhookEvent = PENDING
 ↓
events_received++
 ↓
Create OutboxEvent = PENDING
 ↓
COMMIT
 ↓
Return HTTP success to provider
```

The event and outbox record are persisted transactionally so an accepted webhook is not lost between database persistence and message publication.

### 3. Asynchronous publishing

```text
Scheduled Publisher
 ↓
Find PENDING outbox events
 ↓
Build delivery message
 ↓
RabbitMQ exchange
 ↓
webhook queue
```

RabbitMQ publisher confirms are used to determine whether the broker accepted the published message. The outbox pattern allows pending records to be republished if publishing does not complete successfully.

### 4. Delivery

```text
webhook queue
 ↓
Delivery Worker
 ↓
POST destinationUrl
 ↓
Classify response
 ↓
Update event state / retry / DLQ
```

The worker uses the event ID as the primary traceability key across the delivery lifecycle.

---

# API

Base path:

```text
/api/v1/endpoints
```

The current implementation uses a placeholder user ID for development; production authentication/authorization will supply the actual user identity.

## Create endpoint

```http
POST /api/v1/endpoints
```

### Input

```json
{
  "name": "GitHub Webhook",
  "destinationUrl": "https://example.com/webhook",
  "provider": "GITHUB"
}
```

### Output

```http
201 Created
```

```json
{
  "endpoint": "https://api.ferry.com/webhooks/<endpointId>",
  "secretToken": "<secret>"
}
```

Ferry generates:
- A cryptographically secure endpoint ID.
- A cryptographically secure 32-character secret.
- An active endpoint.
- Initial usage counters.

Current provider support:

```text
GITHUB
```

## Get all endpoints

```http
GET /api/v1/endpoints
```

Returns the authenticated user's webhook endpoints for dashboard/listing purposes.

### Output

```http
200 OK
```

Contains endpoint information such as:
- Name
- Receiving endpoint URL
- Destination URL
- Creation/update timestamps
- Rate limit
- Active state

Secrets are not exposed.

## Get endpoint

```http
GET /api/v1/endpoints/{id}
```

Returns a single endpoint and its usage statistics.

### Output

```http
200 OK
```

Includes:

```text
Endpoint information
+
eventsReceived
eventsDelivered
eventsFailed
```

## Disable endpoint

```http
PUT /api/v1/endpoints/{id}/disable
```

Disables the receiving endpoint.

### Output

```http
200 OK
```

Subsequent incoming webhooks are rejected because the endpoint is inactive.

## Enable endpoint

```http
PUT /api/v1/endpoints/{id}/enable
```

Re-enables the receiving endpoint.

### Output

```http
200 OK
```

## Delete endpoint

```http
DELETE /api/v1/endpoints/{id}
```

Permanently deletes the endpoint.

### Output

```http
204 No Content
```

Deletion is intended to be irreversible.

---

# Webhook Authentication

Ferry currently supports **GitHub webhook signatures**.
The endpoint secret is generated using `SecureRandom` and contains 32 cryptographically random alphanumeric characters.
Incoming GitHub requests are verified using the configured endpoint secret and the provider signature.
The secret is **encrypted at rest**, not hashed, because Ferry must recover the plaintext secret to verify incoming HMAC signatures.

## Secret Encryption

Algorithm:

```text
AES-256-GCM
```

Implementation:

```text
AES/GCM/NoPadding
GCM authentication tag: 128-bit
IV: 12 bytes
```

The encryption key is supplied externally through:
```text
FERRY_ENCRYPTION_KEY
```

The IV is randomly generated for every encryption operation using `SecureRandom`.
Stored value:

```text
Base64(
    IV || ciphertext || GCM authentication tag
)
```

GCM provides both confidentiality and integrity protection.
The encryption key is never stored with the encrypted secret.

---

# Delivery Outcomes

The worker classifies destination responses into four primary outcomes.

| Destination result | Ferry action | Event state |
|---|---|---|
| `2xx` | Successful delivery | `DELIVERED` |
| `429` | Retry | `PENDING` |
| `5xx` | Retry | `PENDING` |
| Other `4xx` | Permanent failure | `FAILED` |
| Network/transport exception | Retry | `PENDING` |
| Retry limit exhausted | DLQ | `DLQ` |

## 2xx — successful delivery

```text
Destination → 200/201/204/etc.
        ↓
DELIVERED
        ↓
events_delivered++
        ↓
retry_count unchanged
```

No retry is scheduled.

## 4xx — permanent failure

Examples:

```text
400 Bad Request
401 Unauthorized
403 Forbidden
404 Not Found
```

Flow:

```text
Destination → 4xx
        ↓
FAILED
        ↓
events_failed++
```

No retry is performed because the same request is not expected to succeed without changing the destination/request configuration.

## 429 — rate limited

```text
Destination → 429
        ↓
Retry
        ↓
retry_count++
```

The event is sent through the retry queue mechanism.

## 5xx — transient server failure

Examples:

```text
500 Internal Server Error
502 Bad Gateway
503 Service Unavailable
504 Gateway Timeout
```

Flow:

```text
Destination → 5xx
        ↓
Retry
        ↓
retry_count++
```

## Network failure

Examples:

```text
Connection refused
DNS failure
Connection timeout
Read timeout
```

These are treated as transient delivery failures and therefore retryable.

---

# Retry Architecture

Ferry uses RabbitMQ TTL queues to implement delayed retries without blocking worker threads.

```text
webhook
   ↓
delivery fails
   ↓
retry.1     TTL 1s
   ↓
webhook
   ↓
retry.2     TTL 2s
   ↓
webhook
   ↓
retry.4     TTL 4s
   ↓
webhook
   ↓
retry.8     TTL 8s
   ↓
webhook
   ↓
retry.16    TTL 16s
   ↓
webhook
   ↓
retry.32    TTL 32s
   ↓
webhook
   ↓
retry.60    TTL 60s
   ↓
webhook
```

Current retry delays:

```text
1s → 2s → 4s → 8s → 16s → 32s → 60s
```

The delay is capped at 60 seconds.

Current maximum attempts:

```text
10
```

Retries do not increment `events_failed`.
Only terminal `FAILED` or `DLQ` outcomes increment the failure counter.

---

# Dead Letter Queue

When an event exhausts its retry attempts:

```text
Worker
 ↓
retry limit reached
 ↓
Publish to webhook_dlq_exchange
 ↓
webhook.dlq
 ↓
Event state = DLQ
 ↓
events_failed++
```

The DLQ prevents permanently failing events from being retried indefinitely and preserves them for future inspection/replay functionality.

---

# Usage Accounting

Usage is tracked per webhook endpoint:

```text
events_received
events_delivered
events_failed
```

Semantics:

```text
Webhook accepted        → events_received++

Successful delivery     → events_delivered++

Permanent 4xx failure   → events_failed++

Retry attempt           → no counter increment

Retries exhausted/DLQ   → events_failed++
```

`events_failed` represents **events that ultimately failed**, not individual failed delivery attempts.

---

# Persistence and Reliability

PostgreSQL stores the durable webhook state.
The ingestion service uses a transactional outbox:

```text
DB Transaction
 ├── WebhookEvent
 ├── WebhookUsage
 └── OutboxEvent
       ↓
     COMMIT
       ↓
Scheduled Publisher
       ↓
RabbitMQ
```

This separates **database persistence** from **message delivery** and avoids losing an accepted webhook if the application crashes before RabbitMQ publication.
The delivery side uses **at-least-once semantics**. A destination may receive the same event more than once if the worker crashes after the HTTP request succeeds but before the local event state is committed.

Therefore:

```text
Exactly-once delivery
        ≠
guaranteed
```

Instead, Ferry is designed around:

```text
At-least-once delivery
+
eventId-based traceability
+
future idempotency support
```

---

# Observability

Important lifecycle transitions are logged with identifiers such as:

```text
eventId
endpointId
outboxId
retryCount
destination
HTTP status
result
```

Example:

```text
INFO  Webhook received eventId=15 endpointId=...
INFO  Publishing eventId=15 outboxId=...
INFO  Delivery started eventId=15 destination=...
INFO  Delivery completed eventId=15 status=200
INFO  Event marked DELIVERED eventId=15
```

Failure:

```text
WARN  Delivery failed eventId=12 status=500 retryCount=3
INFO  Retry scheduled eventId=12 queue=webhook.retry.4
ERROR Delivery exhausted eventId=12 retryCount=10
ERROR Event moved to DLQ eventId=12
```

Secrets, authorization credentials, HMAC signatures, cookies and other sensitive data should not be logged.

---

# Technology Stack

| Layer | Technology |
|---|---|
| Language | Java |
| Framework | Spring Boot |
| API | Spring Web MVC |
| Persistence | Spring Data JPA / Hibernate |
| Database | PostgreSQL |
| Messaging | RabbitMQ / Spring AMQP |
| Containerization | Docker |

---
