package ferry.UserWebHookInteraction.dtos;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class WebhookResponse {
    private String endpointId;
    private String name;
    private String endpoint;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean active;
    private Long rateLimit;
    private String destinationUrl;
}
