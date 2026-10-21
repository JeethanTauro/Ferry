package ferry.Webhooks.repos;

import ferry.Webhooks.entities.OutboxEvent;
import ferry.Webhooks.entities.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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
            @Param("status") OutboxStatus status,
            Pageable pageable
    );



    @Modifying
    @Transactional
    @Query("""
    UPDATE OutboxEvent o
    SET o.lockedUntil = :leaseUntil
    WHERE o.id = :id
      AND o.status = ferry.Webhooks.entities.OutboxStatus.PENDING
      AND (
          o.lockedUntil IS NULL
          OR o.lockedUntil < :now
      )
    """)
    int claimOutboxEvent(
            @Param("id") Long id,
            @Param("now") Instant now,
            @Param("leaseUntil") Instant leaseUntil
    );
}
