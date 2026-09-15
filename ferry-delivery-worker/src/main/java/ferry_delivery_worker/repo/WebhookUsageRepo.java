package ferry_delivery_worker.repo;

import ferry_delivery_worker.entity.WebhookUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WebhookUsageRepo extends JpaRepository<WebhookUsage, Long> {
    Optional<WebhookUsage> findByEndpointId(String endpointId);
}
