package ferry_delivery_worker.entity;

public enum WebhookEventStatus {
    PENDING,
    DELIVERED,
    FAILED,
    DLQ,
    IN_FLIGHT
}