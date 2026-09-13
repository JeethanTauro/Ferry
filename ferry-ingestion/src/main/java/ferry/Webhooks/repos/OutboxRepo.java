package ferry.Webhooks.repos;

import ferry.Webhooks.entities.OutboxEvent;
import ferry.Webhooks.entities.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OutboxRepo extends JpaRepository<OutboxEvent, Long> {
    @Query("""
    SELECT o
    FROM OutboxEvent o
    JOIN FETCH o.webhookEvent we
    JOIN FETCH we.webhookEndpoint
    WHERE o.status = :status
""")
    List<OutboxEvent> findByStatusWithWebhookEvent(
            @Param("status") OutboxStatus status
    );
}
