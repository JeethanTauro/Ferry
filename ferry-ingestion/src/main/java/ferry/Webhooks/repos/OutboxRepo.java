package ferry.Webhooks.repos;

import ferry.Webhooks.entities.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxRepo extends JpaRepository<OutboxEvent, Long> {
}
