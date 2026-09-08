package ferry.UserWebHookInteraction.dtos;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class WebhooksResponse {
    List<WebhookResponse> webhookResponseList;
}
