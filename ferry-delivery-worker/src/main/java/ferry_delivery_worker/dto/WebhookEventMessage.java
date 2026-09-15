package ferry_delivery_worker.dto;


import lombok.Data;

import java.util.Map;

@Data
public class WebhookEventMessage {
    private Long eventId;
    private String endpointId;
    private String payload;
    private String destinationUrl;
    private Map<String, String> headers;
}
