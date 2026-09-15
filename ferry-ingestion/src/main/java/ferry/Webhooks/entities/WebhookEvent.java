package ferry.Webhooks.entities;

import ferry.UserWebHookInteraction.entities.WebhookEndpoint;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.http.HttpHeaders;

import java.time.Instant;
import java.util.Map;


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

    @Column(columnDefinition = "text")
    private String payload; //the webhook body
    private Instant receivedAt; //time at which the event was received


    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> headers; //headers of the webhook event (mainly for debugging)

    @Enumerated(EnumType.STRING)
    private WebhookEventStatus status; //status PENDING, FAILED, DELIVERED, DLQ

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "endpointId",
            referencedColumnName = "endpointId",
            insertable = false,
            updatable = false
    )
    private WebhookEndpoint webhookEndpoint; //many webhook events belong to a single endpoint

    private int retryCount; //total attempts
    private Instant lastAttemptAt; //last attempt


}
