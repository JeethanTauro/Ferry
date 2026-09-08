package ferry.UserWebHookInteraction.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WebhookResponseUsage {
    private Long eventsReceived; //count of events received
    private Long eventsDelivered; //count of events delivered
    private Long eventsFailed;//counts of event failed
}
