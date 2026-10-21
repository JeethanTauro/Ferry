package ferry_delivery_worker.repo;

import ferry_delivery_worker.entity.WebhookEvent;
import ferry_delivery_worker.entity.WebhookEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;

public interface WebhookEventRepo extends JpaRepository<WebhookEvent, Long> {
    @Modifying
    @Transactional
    @Query("""
    UPDATE WebhookEvent e
    SET e.status = :inFlight,
        e.inFlightAt = :now
    WHERE e.eventId = :eventId
      AND e.status IN :allowedStatuses
    """)
    int claimEvent(
            @Param("eventId") Long eventId,
            @Param("allowedStatuses") Collection<WebhookEventStatus> allowedStatuses,
            @Param("inFlight") WebhookEventStatus inFlight,
            @Param("now") Instant now
    );
}