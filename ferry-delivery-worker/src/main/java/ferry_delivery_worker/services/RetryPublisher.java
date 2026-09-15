package ferry_delivery_worker.services;

import ferry_delivery_worker.config.RabbitmqConfig;
import ferry_delivery_worker.dto.WebhookEventMessage;
import lombok.AllArgsConstructor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class RetryPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(
            WebhookEventMessage message,
            int retryCount
    ) {

        String queue = getRetryQueue(retryCount);

        CorrelationData correlationData =
                new CorrelationData();

        rabbitTemplate.convertAndSend(
                "",
                queue,
                message,
                correlationData
        );

        waitForConfirmation(correlationData);
    }


    private void waitForConfirmation(
            CorrelationData correlationData
    ) {

        try {

            CorrelationData.Confirm confirm =
                    correlationData.getFuture().join();

            if (!confirm.ack()) {

                throw new RuntimeException(
                        "RabbitMQ rejected retry message: "
                                + confirm.reason()
                );
            }

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to publish retry message to RabbitMQ",
                    e
            );
        }
    }


    private String getRetryQueue(int retryCount) {

        return switch (retryCount) {

            case 1 -> RabbitmqConfig.RETRY_1_QUEUE;
            case 2 -> RabbitmqConfig.RETRY_2_QUEUE;
            case 3 -> RabbitmqConfig.RETRY_4_QUEUE;
            case 4 -> RabbitmqConfig.RETRY_8_QUEUE;
            case 5 -> RabbitmqConfig.RETRY_16_QUEUE;
            case 6 -> RabbitmqConfig.RETRY_32_QUEUE;
            default -> RabbitmqConfig.RETRY_60_QUEUE;
        };
    }
}