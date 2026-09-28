package ferry.Webhooks.dtos;


import ferry.Webhooks.entities.WebhookEventStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class WebhookEventResponseDto {
    private String payload;
    private Instant receivedAt;
    private Map<String, String> headers;
    private WebhookEventStatus status;
}
