package ferry.Webhooks.services;



import ferry.DeliveryToWorkers.config.RabbitmqConfig;
import ferry.DeliveryToWorkers.dto.WebhookEventMessage;
import ferry.UserWebHookInteraction.entities.WebhookEndpoint;
import ferry.UserWebHookInteraction.entities.WebhookUsage;
import ferry.UserWebHookInteraction.repos.WebhookUsageRepo;
import ferry.Webhooks.entities.WebhookEvent;
import ferry.Webhooks.entities.WebhookEventStatus;
import ferry.Webhooks.repos.WebhookEventRepo;
import ferry.exceptions.CannotReplayException;
import ferry.exceptions.EndpointNotFoundException;
import ferry.exceptions.UnauthorizedException;
import ferry.exceptions.WebhookEventNotFound;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class ReplayService {
    private final WebhookEventRepo webhookEventRepo;
    private final RabbitTemplate rabbitTemplate;
    private final WebhookUsageRepo webhookUsageRepo;

    @Transactional
    public void replayWebhook(Long eventId, Long userId){
        // 1. Fetch event and its endpoint together in one query
        WebhookEvent webhookEvent = webhookEventRepo.findByIdWithEndpoint(eventId)
                .orElseThrow(() -> new WebhookEventNotFound("Event not found: " + eventId));

        // 2. Null check: Ensure the parent endpoint wasn't deleted
        WebhookEndpoint endpoint = webhookEvent.getWebhookEndpoint();
        if (endpoint == null) {
            throw new EndpointNotFoundException("Cannot replay: Webhook endpoint no longer exists");
        }

        // 3. Security check: Ensure the requesting user owns this endpoint
        if (!endpoint.getUserId().equals(userId)) {
            throw new UnauthorizedException("You do not have permission to replay this event");
        }

        if(webhookEvent.getStatus() != WebhookEventStatus.DLQ && webhookEvent.getStatus() != WebhookEventStatus.FAILED){
            throw new CannotReplayException("Cannot Replay this Event");
        }

        // 4. Construct message and publish to RabbitMQ
        WebhookEventMessage webhookEventMessage = WebhookEventMessage.builder()
                .eventId(webhookEvent.getEventId())
                .endpointId(webhookEvent.getEndpointId())
                .destinationUrl(endpoint.getDestinationUrl())
                .headers(webhookEvent.getHeaders())
                .payload(webhookEvent.getPayload())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitmqConfig.EXCHANGE_NAME,
                RabbitmqConfig.ROUTING_KEY,
                webhookEventMessage
        );

        //when we replay we have to reduce the number of events failed for the particular endpoint
        int rowsAffected = webhookUsageRepo.decrementEventFailed(endpoint.getEndpointId());
        if(rowsAffected == 0){
            throw new EndpointNotFoundException("Endpoint not found");
        }

        webhookEvent.setStatus(WebhookEventStatus.PENDING);
        webhookEvent.setRetryCount(0);
        webhookEventRepo.save(webhookEvent);


        log.info("Webhook event with eventId={} replayed", webhookEvent.getEventId());
    }
}
