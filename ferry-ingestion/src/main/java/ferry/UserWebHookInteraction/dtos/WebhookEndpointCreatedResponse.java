package ferry.UserWebHookInteraction.dtos;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class WebhookEndpointCreatedResponse {
   private String endpoint;
   private String secretToken;
}
