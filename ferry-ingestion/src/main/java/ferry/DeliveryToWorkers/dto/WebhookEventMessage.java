package ferry.DeliveryToWorkers.dto;

import lombok.Builder;

@Builder
public class WebhookEventMessage {
    private Long eventId;
    private String endpointId;
    private String payload;
    private String destinationUrl;
    private String headers;
}
