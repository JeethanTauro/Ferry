package ferry_delivery_worker.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@Entity
@Table(name = "webhook_usage")
public class WebhookUsage {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id; //just a primary key

    private String endpointId; //this is the webhook endpoint id
    private Long eventsReceived; //count of events received
    private Long eventsDelivered; //count of events delivered
    private Long eventsFailed;//counts of event failed
}
