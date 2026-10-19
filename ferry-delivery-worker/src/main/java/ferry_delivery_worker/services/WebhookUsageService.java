package ferry_delivery_worker.services;

import ferry_delivery_worker.entity.WebhookUsage;
import ferry_delivery_worker.repo.WebhookUsageRepo;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
//need to remember that here the increment is not atomic, we are doing get(), there could be a situation
//where just before updating another concurrent thread updates, or both read at the same time,so need to make it atomic later
public class WebhookUsageService {

    private final WebhookUsageRepo webhookUsageRepo;

    @Transactional
    public void incrementDelivered(String endpointId) {
        int rowsAffected = webhookUsageRepo.incrementDeliveredAtomic(endpointId);
        if(rowsAffected == 0){
            throw new IllegalStateException("WebhookUsage record not found for endpointId: " + endpointId);
        }
    }

    @Transactional
    public void incrementFailed(String endpointId) {
       int rowsAffected =  webhookUsageRepo.incrementFailedAtomic(endpointId);
       if(rowsAffected == 0){
           throw new IllegalStateException("WebhookUsage record not found for endpointId: " + endpointId);
       }
    }
}