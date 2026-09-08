package ferry.UserWebHookInteraction.entities;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "webhook_usage")
@Data
public class WebhookUsage {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id; //just a primary key

    private Long endpointId; //this is the webhook endpoint id
    private Long eventsReceived; //count of events received
    private Long eventsDelivered; //count of events delivered
    private Long eventsFailed;//counts of event failed
}
