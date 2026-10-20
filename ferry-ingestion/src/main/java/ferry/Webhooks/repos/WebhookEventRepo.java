package ferry.Webhooks.repos;

import ferry.Webhooks.entities.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface WebhookEventRepo extends JpaRepository<WebhookEvent, Long> {
    List<WebhookEvent> getAllByEndpointId(String endpointId);

    @Query("""
        SELECT we FROM WebhookEvent we
        JOIN FETCH we.webhookEndpoint
        WHERE we.eventId = :eventId
    """)
    Optional<WebhookEvent> findByIdWithEndpoint(@Param("eventId") Long eventId);
}
