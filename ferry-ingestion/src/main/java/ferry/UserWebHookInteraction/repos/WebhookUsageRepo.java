package ferry.UserWebHookInteraction.repos;

import ferry.UserWebHookInteraction.entities.WebhookUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WebhookUsageRepo extends JpaRepository<WebhookUsage, Long> {
    Optional<WebhookUsage> findByEndpointId(String id);
}
