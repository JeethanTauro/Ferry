package ferry.UserWebHookInteraction.repos;

import ferry.UserWebHookInteraction.entities.WebhookUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WebhookUsageRepo extends JpaRepository<WebhookUsage, Long> {
    Optional<WebhookUsage> findByEndpointId(String id);
}
