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

import java.time.Instant;
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

        log.info("Received webhook event with eventId={} for endpointId={}",message.getEventId(),message.getEndpointId());
        Optional<WebhookEvent> maybeEvent = webhookEventRepo.findById(message.getEventId());

        if (maybeEvent.isEmpty()) {

            log.error("Webhook event with eventId={} not found", message.getEventId());

            /*
             * We cannot process this message because the corresponding
             * DB record does not exist.
             *
             * For now, return so the message is ACKed rather than
             * endlessly redelivered.
             */
            return;
        }

        WebhookEvent event = maybeEvent.get();


        PayloadDto payloadDto =
                PayloadDto.builder()
                        .payload(message.getPayload())
                        .headers(message.getHeaders())
                        .eventId(message.getEventId())
                        .build();


        ResponseEntity<String> response = null;

        boolean success = false;
        boolean retryable = false;


        // ATTEMPT DELIVERY
        try {

            response = postService.post(message.getDestinationUrl(), payloadDto, message.getHeaders());
            success = failureClassifier.isSuccess(response);
            retryable = !success && failureClassifier.isRetryable(response);

        } catch (Exception e) {

            /*
             * Network-level failure:
             *
             * timeout
             * connection refused
             * DNS failure
             * etc.
             */

            log.warn("Delivery failed for webhook eventId={} and endpointId={}, due to some error", event.getEventId(),message.getEndpointId(), e);
            retryable = true;
        }
        // SUCCESS
        if (success) {

            event.setStatus(WebhookEventStatus.DELIVERED);
            event.setLastAttemptAt(Instant.now());

            webhookUsageService.incrementDelivered(
                    event.getEndpointId()
            );
            log.info("Delivered webhook event with eventId={} for endpointId={}",event.getEventId(), event.getEndpointId());
            webhookEventRepo.save(event);
            /*
             * Listener completed successfully.
             *
             * Spring ACKs the RabbitMQ message.
             */
            return;
        }
        // RETRYABLE FAILURE
        if (retryable) {

            handleRetry(message, event);
            /*
             * If handleRetry() succeeds:
             *
             * retry/DLQ message has been published and confirmed.
             *
             * Listener returns.
             *
             * Spring ACKs the original message.
             */
            return;
        }
        // PERMANENT FAILURE
        event.setStatus(WebhookEventStatus.FAILED);
        log.warn("Webhook event with eventId={} for endpointId={} permanently FAILED",event.getEventId(),event.getEndpointId());
        event.setLastAttemptAt(Instant.now());
        webhookUsageService.incrementFailed(event.getEndpointId());
        webhookEventRepo.save(event);
        /*
         * Permanent failure has been recorded.
         *
         * Listener returns.
         *
         * Spring ACKs the original message.
         */
    }


    private void handleRetry(WebhookEventMessage message, WebhookEvent event) {

        int retryCount = event.getRetryCount() + 1;
        boolean exhausted = retryCount >= RetryConfig.MAX_ATTEMPTS;

        if (exhausted) {
            dlqPublisher.publish(message);
            log.warn("Published webhook event with eventId={}, endpointId={} into DLQ after {} attempts",
                    message.getEventId(), message.getEndpointId(), retryCount);

            event.setRetryCount(retryCount);
            event.setStatus(WebhookEventStatus.DLQ);
            event.setLastAttemptAt(Instant.now());
            webhookEventRepo.save(event);
            webhookUsageService.incrementFailed(event.getEndpointId());

        } else {
            retryPublisher.publish(message, retryCount);
            log.warn("Retrying webhook event with eventId={}, endpointId={}, retryCount={}",
                    message.getEventId(), message.getEndpointId(), retryCount);

            event.setRetryCount(retryCount);
            event.setStatus(WebhookEventStatus.PENDING);
            event.setLastAttemptAt(Instant.now());
            webhookEventRepo.save(event);
        }
    }
}