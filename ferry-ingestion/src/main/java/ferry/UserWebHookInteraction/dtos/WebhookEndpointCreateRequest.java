package ferry.UserWebHookInteraction.dtos;


import ferry.UserWebHookInteraction.entities.Provider;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WebhookEndpointCreateRequest {
    private String name;
    private String destinationUrl;
    private Provider provider;
}
