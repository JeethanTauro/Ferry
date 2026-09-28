package ferry_delivery_worker.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Builder
@Data
public class PayloadDto {
    private String payload;
    private Map<String, String> headers;
    private Long eventId;  //for the consumer to implement idempotency
}
