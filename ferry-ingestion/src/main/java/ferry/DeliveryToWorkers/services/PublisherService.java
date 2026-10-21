package ferry.DeliveryToWorkers.services;

import ferry.DeliveryToWorkers.config.RabbitmqConfig;
import ferry.DeliveryToWorkers.dto.WebhookEventMessage;
import ferry.Webhooks.entities.OutboxEvent;
import ferry.Webhooks.entities.OutboxStatus;
import ferry.Webhooks.entities.WebhookEvent;
import ferry.Webhooks.repos.OutboxRepo;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@AllArgsConstructor
@Slf4j
public class PublisherService {

    private final OutboxRepo outboxRepo;
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(fixedDelay = 5000)
    public void publish() {

        Pageable limit = PageRequest.of(0, 100);

        List<OutboxEvent> outboxEvents = outboxRepo.findByStatusWithWebhookEvent(OutboxStatus.PENDING, limit);
        for (OutboxEvent outboxEvent : outboxEvents) {
            publishEvent(outboxEvent);
        }
    }

    private void publishEvent(OutboxEvent outboxEvent) {

        Instant now = Instant.now();

        // Atomically claim this event. (basically only claim the events that other publishers have not claimed)
        int claimed = outboxRepo.claimOutboxEvent(outboxEvent.getId(), now, now.plusSeconds(60));

        if (claimed == 0) {
            // Another publisher owns this event, or it is no longer pending.
            return;
        }

        try {
            WebhookEvent event = outboxEvent.getWebhookEvent();
            WebhookEventMessage message = WebhookEventMessage.builder()
                    .eventId(event.getEventId())
                    .endpointId(event.getEndpointId())
                    .destinationUrl(event.getWebhookEndpoint().getDestinationUrl())
                    .headers(event.getHeaders())
                    .payload(event.getPayload())
                    .build();

            CorrelationData correlationData = new CorrelationData(outboxEvent.getId().toString());

            rabbitTemplate.convertAndSend(
                    RabbitmqConfig.EXCHANGE_NAME,
                    RabbitmqConfig.ROUTING_KEY,
                    message,
                    correlationData
            );

            // Wait for RabbitMQ's publisher confirmation.
            CorrelationData.Confirm confirm = correlationData.getFuture().get(5, TimeUnit.SECONDS);

            boolean routed = correlationData.getReturned() == null;

            if (confirm.ack() && routed) {

                outboxEvent.setStatus(OutboxStatus.PUBLISHED);
                outboxEvent.setPublishedAt(Instant.now());
                outboxEvent.setLockedUntil(null);

                outboxRepo.save(outboxEvent);

                log.info(
                        "Published outboxId={}, eventId={}",
                        outboxEvent.getId(),
                        event.getEventId()
                );

            } else {
                // Keep PENDING so it can be retried.
                releaseForRetry(outboxEvent);

                log.warn(
                        "RabbitMQ rejected or could not route outboxId={}, "
                                + "ack={}, reason={}",
                        outboxEvent.getId(),
                        confirm.ack(),
                        confirm.reason()
                );
            }

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
            releaseForRetry(outboxEvent);

            log.error(
                    "Interrupted while publishing outboxId={}",
                    outboxEvent.getId(), e);

        } catch (Exception e) {

            // Includes timeout and publishing/connection failures.
            releaseForRetry(outboxEvent);

            log.error(
                    "Failed to publish outboxId={}",
                    outboxEvent.getId(), e);
        }
    }

    private void releaseForRetry(OutboxEvent outboxEvent) {
        try {
            outboxEvent.setStatus(OutboxStatus.PENDING);
            outboxEvent.setLockedUntil(null);
            outboxRepo.save(outboxEvent);
        } catch (Exception e) {
            // If release fails, the lease will eventually expire.
            log.error(
                    "Could not release outboxId={}",
                    outboxEvent.getId(), e);
        }
    }
}