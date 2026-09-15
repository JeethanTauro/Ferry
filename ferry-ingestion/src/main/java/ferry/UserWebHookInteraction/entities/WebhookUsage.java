package ferry.UserWebHookInteraction.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "webhook_usage")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WebhookUsage {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id; //just a primary key

    private String endpointId; //this is the webhook endpoint id
    private Long eventsReceived = 0L; //count of events received
    private Long eventsDelivered = 0L; //count of events delivered
    private Long eventsFailed = 0L;//counts of event failed
}
