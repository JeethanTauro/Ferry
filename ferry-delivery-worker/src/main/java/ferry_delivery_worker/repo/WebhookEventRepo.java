package ferry_delivery_worker.repo;

import ferry_delivery_worker.entity.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookEventRepo extends JpaRepository<WebhookEvent, Long> {
}