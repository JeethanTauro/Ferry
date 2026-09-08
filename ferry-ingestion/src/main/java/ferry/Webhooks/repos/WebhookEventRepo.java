package ferry.Webhooks.repos;

import ferry.Webhooks.entities.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface WebhookEventRepo extends JpaRepository<WebhookEvent, Long> {
}
