package ferry.DeliveryToWorkers.services;

import ferry.DeliveryToWorkers.config.RabbitmqConfig;
import ferry.DeliveryToWorkers.dto.WebhookEventMessage;
import ferry.Webhooks.entities.OutboxEvent;
import ferry.Webhooks.entities.OutboxStatus;
import ferry.Webhooks.entities.WebhookEvent;
import ferry.Webhooks.repos.OutboxRepo;
import lombok.AllArgsConstructor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class PublisherService {
    private final OutboxRepo outboxRepo;
    private final RabbitTemplate rabbitTemplate;


    @Scheduled(fixedDelay = 5000)
    public void publish(){
        //got everything in one query , avoided the N+1 problem using JOIN Fetch
        List<OutboxEvent> outboxEvents =  outboxRepo.findByStatusWithWebhookEvent(OutboxStatus.PENDING); //all the outbox events which are pending with the webhook event too

        for (OutboxEvent outboxEvent : outboxEvents) {

            //from each of the outbox event get the webhook event
            WebhookEvent webhookEvent = outboxEvent.getWebhookEvent();
            WebhookEventMessage webhookEventMessage = WebhookEventMessage.builder()
                    .eventId(webhookEvent.getEventId())
                    .endpointId(webhookEvent.getEndpointId())
                    .destinationUrl(webhookEvent.getWebhookEndpoint().getDestinationUrl())
                    .headers(webhookEvent.getHeaders())
                    .payload(webhookEvent.getPayload())
                    .build();

            // Used to identify which OutboxEvent this RabbitMQ confirmation belongs to
            CorrelationData correlationData =
                    new CorrelationData(outboxEvent.getId().toString());


            System.out.println("========== PUBLISHING ==========");
            System.out.println("Event ID: " + webhookEventMessage.getEventId());
            System.out.println("Endpoint ID: " + webhookEventMessage.getEndpointId());
            System.out.println("Destination URL: " + webhookEventMessage.getDestinationUrl());
            System.out.println("Payload: " + webhookEventMessage.getPayload());
            System.out.println("Headers: " + webhookEventMessage.getHeaders());

            rabbitTemplate.convertAndSend(
                    RabbitmqConfig.EXCHANGE_NAME,
                    RabbitmqConfig.ROUTING_KEY,
                    webhookEventMessage,
                    correlationData
            );
        }

    }
}
