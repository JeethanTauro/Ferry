package ferry.UserWebHookInteraction.repos;

import ferry.UserWebHookInteraction.entities.WebhookEndpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WebhookEndpointRepo extends JpaRepository<WebhookEndpoint, String> {
    List<WebhookEndpoint> getAllByUserId(Long userId);
    Optional<WebhookEndpoint> findByIdAndUserId(String id, Long userId);

}
