package ferry.UserWebHookInteraction.entities;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Entity
@Data
@Table(name="webhook_endpoints")
@Builder
public class WebhookEndpoint {
    @Id
    private String endpointId; //the endpoint id

    private Long userId; //the owner id
    private String endpoint;// the complete endpoint
    @Enumerated(EnumType.STRING)
    private Provider provider; //this is provided by the user either GITHUB, SWIGGY etc (for now only Github)
    private String name; //the name of the endpoint eg : github production endpoint
    private String destinationUrl; //the destination where ferry has to send the event
    private boolean active; //enable or disable the endpoint
    private Long rateLimitPerSecond; // certain rate limit (higher rate limits for premium)
    private Instant createdAt; // created at timestamp
    private Instant updatedAt; //last updated at (maybe disabled, or enabled or name changed etc)
    private String secret; //this secre is to validate the webhook events (must be encrypted and stored, because ferry needs the original secret key to sign too)
}
