package ferry.UserWebHookInteraction.dtos;


import ferry.UserWebHookInteraction.entities.Provider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

@Data
@NoArgsConstructor
public class WebhookEndpointCreateRequest {

    @NotBlank
    private String name;

    @NotBlank
    @URL
    private String destinationUrl;

    @NotNull
    private Provider provider;
}
