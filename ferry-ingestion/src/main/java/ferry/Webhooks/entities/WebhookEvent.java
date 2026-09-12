package ferry.Webhooks.entities;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpHeaders;

import java.time.Instant;


@Entity
@Data
@Table(name = "webhook_events")
@Builder
public class WebhookEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long eventId; //unique id

    private String endpointId; //which ferry endpoint received it
    private String payload; //the webhook body
    private Instant receivedAt; //time at which the event was received

    @Lob
    private String headers; //headers of the webhook event (mainly for debugging)
    @Enumerated(EnumType.STRING)
    private WebhookEventStatus status; //status PENDING, FAILED, DELIVERED, DLQ
}
