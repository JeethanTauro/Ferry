package ferry.Webhooks.services;

import ferry.UserWebHookInteraction.entities.WebhookEndpoint;
import ferry.UserWebHookInteraction.entities.WebhookUsage;
import ferry.UserWebHookInteraction.repos.WebhookEndpointRepo;
import ferry.UserWebHookInteraction.repos.WebhookUsageRepo;
import ferry.UserWebHookInteraction.services.EncryptionService;
import ferry.Webhooks.Util.GithubWebhookVerifier;
import ferry.Webhooks.Util.WebhookSignatureVerfier;
import ferry.Webhooks.entities.OutboxEvent;
import ferry.Webhooks.entities.OutboxStatus;
import ferry.Webhooks.entities.Status;
import ferry.Webhooks.entities.WebhookEvent;
import ferry.Webhooks.repos.OutboxRepo;
import ferry.Webhooks.repos.WebhookEventRepo;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.time.Instant;


//for webhook events
@Service
@AllArgsConstructor
public class WebhookEventService {

    private final WebhookEndpointRepo webhookEndpointRepo;
    private final EncryptionService encryptionService;
    private final WebhookEventRepo webhookEventRepo;
    private final WebhookUsageRepo webhookUsageRepo;
    private final OutboxRepo outboxRepo;


    //as there are multiple steps, saving into multiple tables, we need it to be transactional
    @Transactional
    public void storeWebhookEvent(String endpointId, HttpHeaders httpHeaders, String payload) throws Exception {

        //check if webhook endpoint exists (cuz it could be deleted by the user)
        WebhookEndpoint webhookEndpoint = webhookEndpointRepo
                .findById(endpointId)
                .orElseThrow(() -> new RuntimeException("Webhook endpoint not found"));

        //check if webhook endpoint is active
        if (!webhookEndpoint.isActive()) {
            throw new RuntimeException("Webhook endpoint is inactive");
        }
        String secret = encryptionService.decrypt(webhookEndpoint.getSecret());
        WebhookSignatureVerfier verifier;

        //verify the signature
        switch (webhookEndpoint.getProvider()) {
            case GITHUB -> verifier = new GithubWebhookVerifier();
            default -> throw new RuntimeException(
                    "Unsupported webhook provider"
            );
        }

        boolean valid = verifier.verify(
                payload,
                httpHeaders,
                secret
        );

        if (!valid) {
            throw new RuntimeException("Invalid webhook signature");
        }

        //save the webhook event
        WebhookEvent webhookEvent = WebhookEvent
                .builder()
                .endpointId(webhookEndpoint.getEndpointId())
                .payload(payload)
                .status(Status.PENDING)
                .headers(httpHeaders)
                .receivedAt(Instant.now())
                .build();
        webhookEvent = webhookEventRepo.save(webhookEvent);

        //increment the count of the usage of that particular endpoint
        WebhookUsage webhookUsage = webhookUsageRepo.findByEndpointId(endpointId).orElseThrow(() -> new RuntimeException("Webhook endpoint not found"));
        webhookUsage.setEventsReceived(webhookUsage.getEventsReceived()+1);

        //outbox pattern
        OutboxEvent outboxEvent = OutboxEvent
                .builder()
                .createdAt(Instant.now())
                .status(OutboxStatus.PENDING)
                .publishedAt(null)
                .eventId(webhookEvent.getEventId())
                .build();

        //save it
        outboxRepo.save(outboxEvent);
    }
}
