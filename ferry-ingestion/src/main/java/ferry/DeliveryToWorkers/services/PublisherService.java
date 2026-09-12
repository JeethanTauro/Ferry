package ferry.DeliveryToWorkers.services;

import ferry.DeliveryToWorkers.config.RabbitmqConfig;
import ferry.Webhooks.entities.OutboxEvent;
import ferry.Webhooks.entities.OutboxStatus;
import ferry.Webhooks.entities.WebhookEvent;
import ferry.Webhooks.repos.OutboxRepo;
import lombok.AllArgsConstructor;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class PublisherService {
    private final OutboxRepo outboxRepo;
    private final RabbitTemplate rabbitTemplate;
    private final TopicExchange topicExchange;


    @Scheduled(fixedDelay = 5000)
    public void publish(){
        //got everything in one query , avoided the N+1 problem using JOIN Fetch
        List<OutboxEvent> outboxEvents =  outboxRepo.findByStatusWithWebhookEvent(OutboxStatus.PENDING); //all the outbox events which are pending with the webhook event too

        for (OutboxEvent outboxEvent : outboxEvents) {

            //from each of the outbox event get the webhook event
            WebhookEvent webhookEvent = outboxEvent.getWebhookEvent();

            // Used to identify which OutboxEvent this RabbitMQ confirmation belongs to
            CorrelationData correlationData =
                    new CorrelationData(outboxEvent.getId().toString());

            rabbitTemplate.convertAndSend(
                    RabbitmqConfig.EXCHANGE_NAME,
                    RabbitmqConfig.ROUTING_KEY,
                    webhookEvent,
                    correlationData
            );
        }

    }
}
