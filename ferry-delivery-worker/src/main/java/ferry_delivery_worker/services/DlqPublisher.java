package ferry_delivery_worker.services;

import ferry_delivery_worker.config.RabbitmqConfig;
import ferry_delivery_worker.dto.WebhookEventMessage;
import lombok.AllArgsConstructor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class DlqPublisher {

    private final RabbitTemplate rabbitTemplate;


    public void publish(
            WebhookEventMessage message
    ) {

        CorrelationData correlationData =
                new CorrelationData();

        rabbitTemplate.convertAndSend(
                RabbitmqConfig.DLQ_EXCHANGE,
                RabbitmqConfig.DLQ_ROUTING_KEY,
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
                        "RabbitMQ rejected DLQ message: "
                                + confirm.reason()
                );
            }

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to publish message to DLQ",
                    e
            );
        }
    }
}