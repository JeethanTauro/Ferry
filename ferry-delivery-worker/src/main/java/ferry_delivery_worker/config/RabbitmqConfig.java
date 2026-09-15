package ferry_delivery_worker.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitmqConfig {

    public static final String QUEUE_NAME = "webhook";

    public static final String EXCHANGE_NAME = "webhook_exchange";
    public static final String ROUTING_KEY = "routing.key.webhook";

    public static final String RETRY_1_QUEUE = "webhook.retry.1";
    public static final String RETRY_2_QUEUE = "webhook.retry.2";
    public static final String RETRY_4_QUEUE = "webhook.retry.4";
    public static final String RETRY_8_QUEUE = "webhook.retry.8";
    public static final String RETRY_16_QUEUE = "webhook.retry.16";
    public static final String RETRY_32_QUEUE = "webhook.retry.32";
    public static final String RETRY_60_QUEUE = "webhook.retry.60";

    public static final String DLQ_NAME = "webhook.dlq";
    public static final String DLQ_EXCHANGE = "webhook_dlq_exchange";
    public static final String DLQ_ROUTING_KEY = "webhook.dlq";


    //main queue
    @Bean
    public Queue webhookQueue() {
        return QueueBuilder
                .durable(QUEUE_NAME)
                .build();
    }

    @Bean
    public TopicExchange webhookExchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public Binding webhookBinding() {
        return BindingBuilder
                .bind(webhookQueue())
                .to(webhookExchange())
                .with(ROUTING_KEY);
    }


    //retry queues
    @Bean
    public Queue retry1Queue() {
        return retryQueue(RETRY_1_QUEUE, 1000);
    }

    @Bean
    public Queue retry2Queue() {
        return retryQueue(RETRY_2_QUEUE, 2000);
    }

    @Bean
    public Queue retry4Queue() {
        return retryQueue(RETRY_4_QUEUE, 4000);
    }

    @Bean
    public Queue retry8Queue() {
        return retryQueue(RETRY_8_QUEUE, 8000);
    }

    @Bean
    public Queue retry16Queue() {
        return retryQueue(RETRY_16_QUEUE, 16000);
    }

    @Bean
    public Queue retry32Queue() {
        return retryQueue(RETRY_32_QUEUE, 32000);
    }

    @Bean
    public Queue retry60Queue() {
        return retryQueue(RETRY_60_QUEUE, 60000);
    }


    private Queue retryQueue(String queueName, int ttl) {

        return QueueBuilder
                .durable(queueName)
                .ttl(ttl) //message lives here for ttl milliseconds
                .deadLetterExchange(EXCHANGE_NAME) //after ttl, send message to main exchange
                .deadLetterRoutingKey(ROUTING_KEY) //send it back to main queue
                .build();
    }


    //dlq
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


    //json message converter
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }


    //rabbitmq listener configuration
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter
    ) {

        SimpleRabbitListenerContainerFactory factory =
                new SimpleRabbitListenerContainerFactory();

        factory.setConnectionFactory(connectionFactory);

        //convert incoming json message into WebhookEventMessage
        factory.setMessageConverter(jsonMessageConverter);

        //don't requeue failed messages automatically
        factory.setDefaultRequeueRejected(false);

        return factory;
    }
}