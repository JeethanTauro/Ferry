package ferry.Webhooks.entities;

public enum WebhookEventStatus {
    PENDING,
    DELIVERED,
    FAILED,
    DLQ,
    IN_FLIGHT
}
