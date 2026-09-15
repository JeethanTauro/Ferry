package ferry_delivery_worker.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "webhook_events")
@Data
@NoArgsConstructor
public class WebhookEvent {

    @Id
    private Long eventId;

    @Enumerated(EnumType.STRING)
    private WebhookEventStatus status;

    private String endpointId;

    private int retryCount;

    private Instant lastAttemptAt;
}