package ferry.DeliveryToWorkers.config;

import ferry.Webhooks.entities.OutboxEvent;
import ferry.Webhooks.entities.OutboxStatus;
import ferry.Webhooks.repos.OutboxRepo;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;

@Configuration
public class RabbitmqConfig {

    public static final String QUEUE_NAME = "webhook";
    public static final String EXCHANGE_NAME = "webhook_exchange";
    public static final String ROUTING_KEY = "routing.key.webhook";


    public static final String DLQ_NAME = "webhook.dlq";
    public static final String DLQ_EXCHANGE = "webhook_dlq_exchange";
    public static final String DLQ_ROUTING_KEY = "webhook.dlq";

    @Bean
    public Queue queue() {
        return QueueBuilder
                .durable(QUEUE_NAME)
                .deadLetterExchange(DLQ_EXCHANGE)
                .deadLetterRoutingKey(DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public Binding binding(Queue queue, TopicExchange exchange) {
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with("routing.key.#");
    }


    @Bean
    public Queue dlq() {
        return QueueBuilder
                .durable(DLQ_NAME)
                .build();
    }

    @Bean
    public DirectExchange dlqExchange() {
        return new DirectExchange(DLQ_EXCHANGE, true, false);
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder
                .bind(dlq())
                .to(dlqExchange())
                .with(DLQ_ROUTING_KEY);
    }


    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            OutboxRepo outboxRepo,
            MessageConverter messageConverter) {

        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);

        rabbitTemplate.setMessageConverter(messageConverter);

        rabbitTemplate.setConfirmCallback(
                (correlationData, ack, cause) -> {

                    if (correlationData == null) {
                        return;
                    }

                    Long outboxId =
                            Long.valueOf(correlationData.getId());

                    if (ack) {

                        OutboxEvent outboxEvent =
                                outboxRepo.findById(outboxId)
                                        .orElseThrow();

                        outboxEvent.setStatus(OutboxStatus.PUBLISHED);
                        outboxEvent.setPublishedAt(Instant.now());

                        outboxRepo.save(outboxEvent);

                    } else {

                        System.out.println(
                                "RabbitMQ rejected message: " + cause
                        );
                        outboxRepo.findById(outboxId).ifPresent(event -> {
                            event.setStatus(OutboxStatus.PENDING);
                            outboxRepo.save(event);
                        });
                    }
                }
        );

        return rabbitTemplate;
    }
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}