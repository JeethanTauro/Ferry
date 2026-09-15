# Ferry Delivery Worker

The **Ferry Delivery Worker** is the asynchronous delivery service in
Ferry. It consumes persisted webhook events from RabbitMQ, delivers them
to user-configured destination URLs, classifies delivery outcomes,
performs bounded exponential backoff retries, and moves permanently
failing events to a Dead Letter Queue (DLQ).

## Architecture

``` text
                         Ferry Ingestion Service
                                  |
                           Outbox Publisher
                                  |
                                  v
                     +---------------------------+
                     | RabbitMQ                  |
                     | webhook_exchange          |
                     | routing.key.webhook       |
                     +-------------+-------------+
                                   |
                                   v
                         +------------------+
                         | webhook queue    |
                         +--------+---------+
                                  |
                         @RabbitListener
                                  |
                                  v
                    +--------------------------+
                    | WebhookConsumer          |
                    |                          |
                    | 1. Load event            |
                    | 2. Deliver HTTP request  |
                    | 3. Classify result       |
                    | 4. Update DB             |
                    | 5. Retry / DLQ           |
                    +----+-----------+---------+
                         |           |
                2xx      |           | 4xx
                         v           v
                    DELIVERED      FAILED
                         |
                         | 429 / 5xx / network error
                         v
                +----------------------+
                | Retry queues         |
                | 1s -> 2s -> 4s ->    |
                | 8s -> 16s -> 32s ->  |
                | 60s (capped)         |
                +----------+-----------+
                           |
                           v
                      webhook queue
                           |
                      max attempts
                           |
                           v
                     +-----------+
                     | DLQ       |
                     | webhook.  |
                     | dlq       |
                     +-----------+
```

## Responsibilities

  -----------------------------------------------------------------------
  Component                           Responsibility
  ----------------------------------- -----------------------------------
  `WebhookConsumer`                   Consumes events and orchestrates
                                      delivery

  `PostService`                       Performs outbound HTTP POST
                                      requests

  `DeliveryFailureClassifier`         Classifies HTTP responses as
                                      success/retry/permanent failure

  `RetryPublisher`                    Publishes failed deliveries to
                                      TTL-based retry queues

  `DlqPublisher`                      Publishes exhausted events to the
                                      DLQ

  `WebhookEventRepo`                  Reads and updates delivery state

  `WebhookUsageService`               Updates delivered/failed usage
                                      counters

  `RabbitmqConfig`                    Declares exchanges, queues,
                                      bindings and JSON conversion
  -----------------------------------------------------------------------

## Delivery State 

``` text
                 +------+
                 | POST |
                 +--+---+
                    |
          +---------+---------+
          |         |         |
         2xx      429/5xx    4xx
          |         |         |
          v         v         v
     DELIVERED    RETRY      FAILED
                    |
              attempts left?
                /       \
              yes       no
               |         |
               v         v
             RETRY      DLQ
```


## Retry Strategy

Retry attempts use TTL queues:

    Retry attempt   Delay Queue
  --------------- ------- --------------------
                1      1s `webhook.retry.1`
                2      2s `webhook.retry.2`
                3      4s `webhook.retry.4`
                4      8s `webhook.retry.8`
                5     16s `webhook.retry.16`
                6     32s `webhook.retry.32`
               7+     60s `webhook.retry.60`

Each retry queue has a TTL and dead-letters expired messages back to the
main webhook exchange.

The current policy is bounded by `MAX_ATTEMPTS = 10`. Once exhausted,
the event is published to `webhook.dlq` and marked `DLQ` in PostgreSQL.

## Delivery Classification

``` text
HTTP 2xx                  -> DELIVERED
HTTP 429                  -> RETRY
HTTP 5xx                  -> RETRY
Other HTTP 4xx            -> FAILED
Connection / DNS / timeout -> RETRY
```


## Reliability Model

Ferry uses **at-least-once delivery semantics**.

A worker crash can occur after the destination receives the HTTP request
but before the event state is committed. The same event may therefore be
delivered more than once.

The system should eventually use `eventId` as an idempotency key where
destination-side idempotency is supported.

The outbox pattern on the ingestion side provides durable handoff from
PostgreSQL to RabbitMQ, while the worker provides bounded retry and DLQ
handling.

