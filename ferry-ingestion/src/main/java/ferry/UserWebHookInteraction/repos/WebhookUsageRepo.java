package ferry.UserWebHookInteraction.repos;

import ferry.UserWebHookInteraction.entities.WebhookUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WebhookUsageRepo extends JpaRepository<WebhookUsage, Long> {
    Optional<WebhookUsage> findByEndpointId(String id);

    @Modifying
    @Query("UPDATE WebhookUsage u SET u.eventsReceived = u.eventsReceived + 1 WHERE u.endpointId = :endpointId")
    int incrementEventReceived(@Param("endpointId") String endpointId);

    @Modifying
    @Query("UPDATE WebhookUsage u SET u.eventsFailed = u.eventsFailed - 1 WHERE u.endpointId = :endpointId")
    int decrementEventFailed(@Param("endpointId") String endpointId);
}
