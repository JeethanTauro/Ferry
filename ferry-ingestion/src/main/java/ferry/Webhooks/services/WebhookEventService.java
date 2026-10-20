package ferry.Webhooks.services;

import ferry.UserWebHookInteraction.entities.WebhookEndpoint;
import ferry.UserWebHookInteraction.repos.WebhookEndpointRepo;
import ferry.UserWebHookInteraction.repos.WebhookUsageRepo;
import ferry.UserWebHookInteraction.services.EncryptionService;
import ferry.Webhooks.Util.GithubWebhookVerifier;
import ferry.Webhooks.Util.WebhookSignatureVerifier;
import ferry.Webhooks.Util.WebhookVerifierFactory;
import ferry.Webhooks.dtos.WebhookEventResponseDto;
import ferry.Webhooks.entities.*;
import ferry.Webhooks.repos.OutboxRepo;
import ferry.Webhooks.repos.WebhookEventRepo;
import ferry.exceptions.*;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


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
    private final WebhookVerifierFactory verifierFactory;


    //as there are multiple steps, saving into multiple tables, we need it to be transactional
    @Transactional
    public void storeWebhookEvent(String endpointId, HttpHeaders httpHeaders, String payload) throws Exception {

        //check if webhook endpoint exists (cuz it could be deleted by the user)
        WebhookEndpoint webhookEndpoint = webhookEndpointRepo
                .findById(endpointId)
                .orElseThrow(() -> new EndpointNotFoundException("Webhook endpoint not found"));

        //check if webhook endpoint is active
        if (!webhookEndpoint.isActive()) {
            throw new WebhookEndpointInactiveException("Webhook endpoint is inactive");
        }
        String secret = encryptionService.decrypt(webhookEndpoint.getSecret());


        //verify the signature
        WebhookSignatureVerifier verifier = verifierFactory.getVerifier(webhookEndpoint.getProvider());

        boolean valid = verifier.verify(payload, httpHeaders, secret);
        if (!valid) {
            throw new InvalidWebhookSignatureException("Invalid webhook signature");
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
        int rowsAffected  = webhookUsageRepo.incrementEventReceived(endpointId);
        if(rowsAffected == 0){
            throw new EndpointNotFoundException("Endpoint not found");
        }

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

    //get all the webhook events that hit the endpoint with endpointId
    public List<WebhookEventResponseDto> getWebhookEvents(Long userId,String endpointId){

        WebhookEndpoint endpoint = webhookEndpointRepo.findById(endpointId)
                .orElseThrow(() ->
                        new EndpointNotFoundException("Webhook endpoint not found"));

        if (endpoint.isDeleted() || !endpoint.getUserId().equals(userId)) {
            throw new EndpointNotFoundException("Webhook endpoint not found");
        }

        List<WebhookEvent> webhookEvents = webhookEventRepo.getAllByEndpointId(endpointId);
        List<WebhookEventResponseDto> webhookEventResponseDtos = new ArrayList<>();
        for(WebhookEvent webhookEvent :  webhookEvents){
            WebhookEventResponseDto webhookEventResponseDto = WebhookEventResponseDto.builder()
                    .eventId(webhookEvent.getEventId())
                    .headers(webhookEvent.getHeaders())
                    .payload(webhookEvent.getPayload())
                    .receivedAt(webhookEvent.getReceivedAt())
                    .status(webhookEvent.getStatus())
                    .build();
            webhookEventResponseDtos.add(webhookEventResponseDto);
        }

        return webhookEventResponseDtos;
    }

}
