package ferry.UserWebHookInteraction.repos;

import ferry.UserWebHookInteraction.entities.WebhookEndpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WebhookEndpointRepo extends JpaRepository<WebhookEndpoint, String> {
    List<WebhookEndpoint> getAllByUserIdAndDeletedFalse(Long userId);

    Optional<WebhookEndpoint> findByEndpointIdAndUserIdAndDeletedFalse(String endpointId, Long userId);

}
