package ferry.Webhooks.dtos;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ReplayResponseDto {
    private Long eventId;
    private String response;
}
