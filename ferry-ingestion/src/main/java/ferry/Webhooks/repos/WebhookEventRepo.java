package ferry.Webhooks.repos;

import ferry.Webhooks.dtos.WebhookEventResponseDto;
import ferry.Webhooks.entities.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface WebhookEventRepo extends JpaRepository<WebhookEvent, Long> {
    List<WebhookEvent> getAllByEndpointId(String endpointId);
}
