package ferry_delivery_worker.services;

import ferry_delivery_worker.entity.WebhookUsage;
import ferry_delivery_worker.repo.WebhookUsageRepo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
//need to remember that here the increment is not atomic, we are doing get(), there could be a situation
//where just before updating another concurrent thread updates, or both read at the same time,so need to make it atomic later
public class WebhookUsageService {

    private final WebhookUsageRepo webhookUsageRepo;

    public void incrementDelivered(String endpointId) {

        WebhookUsage usage =
                webhookUsageRepo.findByEndpointId(endpointId)
                        .orElseThrow();

        usage.setEventsDelivered(
                usage.getEventsDelivered() + 1
        );

        webhookUsageRepo.save(usage);
    }

    public void incrementFailed(String endpointId) {

        WebhookUsage usage =
                webhookUsageRepo.findByEndpointId(endpointId)
                        .orElseThrow();

        usage.setEventsFailed(
                usage.getEventsFailed() + 1
        );

        webhookUsageRepo.save(usage);
    }
}