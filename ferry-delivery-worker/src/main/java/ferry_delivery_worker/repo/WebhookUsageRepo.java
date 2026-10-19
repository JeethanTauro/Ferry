package ferry_delivery_worker.repo;

import ferry_delivery_worker.entity.WebhookUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WebhookUsageRepo extends JpaRepository<WebhookUsage, Long> {
    Optional<WebhookUsage> findByEndpointId(String endpointId);

    @Modifying
    @Query("UPDATE WebhookUsage u SET u.eventsDelivered = COALESCE(u.eventsDelivered, 0) + 1 WHERE u.endpointId = :endpointId")
    int incrementDeliveredAtomic(@Param("endpointId") String endpointId);

    @Modifying
    @Query("UPDATE WebhookUsage u SET u.eventsFailed = COALESCE(u.eventsFailed, 0) + 1 WHERE u.endpointId = :endpointId")
    int incrementFailedAtomic(@Param("endpointId") String endpointId);
}
