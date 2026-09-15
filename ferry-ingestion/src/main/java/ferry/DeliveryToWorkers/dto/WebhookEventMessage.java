package ferry.DeliveryToWorkers.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Builder
@Data
public class WebhookEventMessage {
    private Long eventId;
    private String endpointId;
    private String payload;
    private String destinationUrl;
    private Map<String, String> headers;
}
