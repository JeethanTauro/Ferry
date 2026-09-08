package ferry.UserWebHookInteraction.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WebhookResponseWithUsage {
    private WebhookResponse webhookResponse;
    private WebhookResponseUsage webhookResponseUsage;
}
