package ferry_delivery_worker.consumer;

import ferry_delivery_worker.config.RabbitmqConfig;
import ferry_delivery_worker.config.RetryConfig;
import ferry_delivery_worker.dto.PayloadDto;
import ferry_delivery_worker.dto.WebhookEventMessage;
import ferry_delivery_worker.entity.WebhookEvent;
import ferry_delivery_worker.entity.WebhookEventStatus;
import ferry_delivery_worker.repo.WebhookEventRepo;
import ferry_delivery_worker.services.*;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@AllArgsConstructor
@Service
public class WebhookConsumer {

    private final PostService postService;
    private final WebhookEventRepo webhookEventRepo;
    private final DeliveryFailureClassifier failureClassifier;
    private final RetryPublisher retryPublisher;
    private final DlqPublisher dlqPublisher;
    private final WebhookUsageService webhookUsageService;

    @RabbitListener(queues = RabbitmqConfig.QUEUE_NAME)
    public void consume(WebhookEventMessage message) {

        Long eventId = message.getEventId();

        log.info("Received webhook event eventId={}, endpointId={}",
                eventId, message.getEndpointId());

        // Claim first, before loading the entity.
        int claimed = webhookEventRepo.claimEvent(
                eventId,
                List.of(
                        WebhookEventStatus.PENDING,
                        WebhookEventStatus.FAILED,
                        WebhookEventStatus.DLQ
                ),
                WebhookEventStatus.IN_FLIGHT,
                Instant.now()
        );

        log.info("Claim result: eventId={}, claimed={}", eventId, claimed);

        if (claimed == 0) {
            log.info(
                    "Event {} is not PENDING; skipping duplicate or concurrent delivery",
                    eventId
            );
            return;
        }

        Optional<WebhookEvent> maybeEvent =
                webhookEventRepo.findById(eventId);


        if (maybeEvent.isEmpty()) {
            // The claim succeeded, but the row is unexpectedly missing.
            log.error("Claimed event {} no longer exists", eventId);
            return;
        }

        WebhookEvent event = maybeEvent.get();

        try {
            processEvent(message, event);

        } catch (RuntimeException e) {

            /*
             * An unexpected exception must not silently leave an event
             * IN_FLIGHT. Reset it so it can be retried, then propagate
             * the exception to Spring AMQP.
             */
            if (event.getStatus() == WebhookEventStatus.IN_FLIGHT) {
                event.setStatus(WebhookEventStatus.PENDING);
                event.setInFlightAt(null);
                webhookEventRepo.save(event);
            }

            throw e;
        }
    }

    private void processEvent(
            WebhookEventMessage message,
            WebhookEvent event) {

        PayloadDto payloadDto = PayloadDto.builder()
                .payload(message.getPayload())
                .headers(message.getHeaders())
                .eventId(message.getEventId())
                .build();

        ResponseEntity<String> response = null;
        boolean retryable = false;

        try {
            response = postService.post(
                    message.getDestinationUrl(),
                    payloadDto,
                    message.getHeaders()
            );

        } catch (IllegalArgumentException e) {
            log.error("Invalid destination URL for eventId={}",
                    event.getEventId(), e);

        } catch (ResourceAccessException e) {
            log.warn("Transport failure for eventId={}",
                    event.getEventId(), e);

            retryable = true;
        }

        boolean success = false;

        if (response != null) {
            success = failureClassifier.isSuccess(response);
            retryable = !success
                    && failureClassifier.isRetryable(response);
        }

        if (success) {
            event.setStatus(WebhookEventStatus.DELIVERED);
            event.setLastAttemptAt(Instant.now());
            event.setInFlightAt(null);

            webhookEventRepo.save(event);
            webhookUsageService.incrementDelivered(event.getEndpointId());

            log.info("Delivered eventId={} to endpointId={}",
                    event.getEventId(), event.getEndpointId());
            return;
        }

        if (retryable) {
            handleRetry(message, event);
            return;
        }

        // Permanent failure.
        event.setStatus(WebhookEventStatus.FAILED);
        event.setLastAttemptAt(Instant.now());
        event.setInFlightAt(null);

        webhookEventRepo.save(event);
        webhookUsageService.incrementFailed(event.getEndpointId());

        log.warn("Event {} permanently failed for endpointId={}",
                event.getEventId(), event.getEndpointId());
    }

    private void handleRetry(
            WebhookEventMessage message,
            WebhookEvent event) {

        int retryCount = event.getRetryCount() + 1;
        boolean exhausted = retryCount >= RetryConfig.MAX_ATTEMPTS;

        if (exhausted) {

            // Publish/confirm to DLQ before acknowledging original message.
            dlqPublisher.publish(message);

            event.setRetryCount(retryCount);
            event.setStatus(WebhookEventStatus.DLQ);
            event.setLastAttemptAt(Instant.now());
            event.setInFlightAt(null);

            webhookEventRepo.save(event);
            webhookUsageService.incrementFailed(event.getEndpointId());

            log.warn("Event {} moved to DLQ after {} attempts",
                    event.getEventId(), retryCount);

        } else {

            // Publish/confirm retry before acknowledging original message.
            retryPublisher.publish(message, retryCount);

            event.setRetryCount(retryCount);
            event.setStatus(WebhookEventStatus.PENDING);
            event.setLastAttemptAt(Instant.now());
            event.setInFlightAt(null);

            webhookEventRepo.save(event);

            log.warn("Retry scheduled for eventId={}, retryCount={}",
                    event.getEventId(), retryCount);
        }
    }
}