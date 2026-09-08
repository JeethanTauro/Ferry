package ferry.UserWebHookInteraction.dtos;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WebhookEndpointCreateRequest {
    private String name;
    private String destinationUrl;
}
