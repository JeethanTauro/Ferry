package ferry.Webhooks.controllers;


import ferry.Auth.services.AuthService;
import ferry.Webhooks.dtos.ReplayResponseDto;
import ferry.Webhooks.dtos.WebhookEventResponseDto;
import ferry.Webhooks.services.ReplayService;
import ferry.Webhooks.services.WebhookEventService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

//so the full endpoint is going to be http://ferry.com/webhooks/{id}
@RequestMapping("/webhooks")
@AllArgsConstructor
@RestController
@Validated
public class WebhookController {

    private final WebhookEventService webhookEventService;
    private final AuthService authService;
    private final ReplayService replayService;
    @PostMapping("/{endpointId}")
    public ResponseEntity<?> createWebhookEvents(@PathVariable @NotBlank @Size(min = 25, max = 25) String endpointId , @RequestHeader HttpHeaders httpHeaders, @RequestBody String payload) throws Exception {
        webhookEventService.storeWebhookEvent(endpointId,httpHeaders,payload);
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    @GetMapping("/{endpointId}/events")
    public ResponseEntity<?> getAllWebhookEvents(@PathVariable @NotBlank @Size(min = 25, max = 25) String endpointId, Authentication authentication){
        Long userId = authService.getUserId(authentication);
        List<WebhookEventResponseDto> webhookEventResponseDtoList = webhookEventService.getWebhookEvents(userId,endpointId);
        return ResponseEntity.ok(webhookEventResponseDtoList);
    }

    @PostMapping("/{eventId}/replay")
    public ResponseEntity<?> replay(@PathVariable Long eventId, Authentication authentication){
        Long userId = authService.getUserId(authentication);
        replayService.replayWebhook(eventId, userId);
        ReplayResponseDto replayResponseDto = ReplayResponseDto
                .builder()
                .eventId(eventId)
                .response("Successfully replayed the event")
                .build();
        return ResponseEntity.ok(replayResponseDto);
    }

}
