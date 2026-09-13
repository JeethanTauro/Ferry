package ferry.Webhooks.entities;

import ferry.UserWebHookInteraction.entities.WebhookEndpoint;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpHeaders;

import java.time.Instant;


@Entity
@Data
@Table(name = "webhook_events")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long eventId; //unique id

    private String endpointId; //which ferry endpoint received it
    private String payload; //the webhook body
    private Instant receivedAt; //time at which the event was received


    private String headers; //headers of the webhook event (mainly for debugging)
    @Enumerated(EnumType.STRING)
    private WebhookEventStatus status; //status PENDING, FAILED, DELIVERED, DLQ

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "endpoint_id",
            referencedColumnName = "endpoint_id"
    )
    private WebhookEndpoint webhookEndpoint; //many webhook events belong to a single endpoint
}
