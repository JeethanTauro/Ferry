package ferry.UserWebHookInteraction.dtos;


import ferry.UserWebHookInteraction.entities.Provider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class WebhookEndpointCreateRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String destinationUrl;

    @NotNull
    private Provider provider;
}
