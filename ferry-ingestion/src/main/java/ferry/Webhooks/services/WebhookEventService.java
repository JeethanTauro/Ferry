package ferry.Webhooks.services;

import ferry.UserWebHookInteraction.entities.WebhookEndpoint;
import ferry.UserWebHookInteraction.entities.WebhookUsage;
import ferry.UserWebHookInteraction.repos.WebhookEndpointRepo;
import ferry.UserWebHookInteraction.repos.WebhookUsageRepo;
import ferry.UserWebHookInteraction.services.EncryptionService;
import ferry.Webhooks.Util.GithubWebhookVerifier;
import ferry.Webhooks.Util.WebhookSignatureVerfier;
import ferry.Webhooks.entities.*;
import ferry.Webhooks.repos.OutboxRepo;
import ferry.Webhooks.repos.WebhookEventRepo;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;


//for webhook events
@Service
@AllArgsConstructor
@Slf4j
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
                .status(WebhookEventStatus.PENDING)
                .headers(httpHeaders.toSingleValueMap())
                .receivedAt(Instant.now())
                .build();
        log.info("Webhook event with eventId={} stored for webhook endpointId={}",  webhookEvent.getEventId(),webhookEvent.getEndpointId());
        webhookEvent = webhookEventRepo.save(webhookEvent);


        //increment the count of the usage of that particular endpoint
        WebhookUsage webhookUsage = webhookUsageRepo.findByEndpointId(endpointId).orElseThrow(() -> new RuntimeException("Webhook usage not found"));
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
        log.info("Webhook event with eventId={} stored in outbox for webhook endpointId={}", webhookEvent.getEventId(), webhookEvent.getEndpointId());
        outboxRepo.save(outboxEvent);
    }

}
