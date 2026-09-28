package ferry.UserWebHookInteraction.controllers;

import ferry.Auth.services.AuthService;
import ferry.UserWebHookInteraction.dtos.WebhookEndpointCreateRequest;
import ferry.UserWebHookInteraction.dtos.WebhookEndpointCreatedResponse;
import ferry.UserWebHookInteraction.dtos.WebhookResponseWithUsage;
import ferry.UserWebHookInteraction.dtos.WebhooksResponse;
import ferry.UserWebHookInteraction.services.WebhookService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/endpoints")
@Validated
public class UserInteractionWebhookController {

    private final WebhookService webhookService;
    private final AuthService authService;

    @GetMapping
    public ResponseEntity<WebhooksResponse> getEndpoints(Authentication authentication) {
        Long userId = authService.getUserId(authentication);
        WebhooksResponse response = webhookService.readWebhooks(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{endpointId}")
    public ResponseEntity<WebhookResponseWithUsage> getEndpoint(@PathVariable @NotBlank @Size(min = 25, max = 25) String endpointId, Authentication authentication) {

        Long userId = authService.getUserId(authentication);
        WebhookResponseWithUsage response = webhookService.readWebhook(endpointId, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<WebhookEndpointCreatedResponse> createEndpoint(@Valid @RequestBody WebhookEndpointCreateRequest request, Authentication authentication) throws Exception {
        Long userId = authService.getUserId(authentication);
        WebhookEndpointCreatedResponse response = webhookService.createWebhook(
                        userId,
                        request.getName(),
                        request.getDestinationUrl(),
                        request.getProvider()
                );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{endpointId}/disable")
    public ResponseEntity<Void> disableEndpoint(@PathVariable @NotBlank @Size(min = 25, max = 25) String endpointId, Authentication authentication) {

        Long userId = authService.getUserId(authentication);
        webhookService.disableWebhook(endpointId, userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{endpointId}/enable")
    public ResponseEntity<Void> enableEndpoint(@PathVariable @NotBlank @Size(min = 25, max = 25) String endpointId, Authentication authentication) {

        Long userId = authService.getUserId(authentication);
        webhookService.enableWebhook(endpointId, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{endpointId}")
    public ResponseEntity<Void> deleteEndpoint(@PathVariable @NotBlank @Size(min = 25, max = 25) String endpointId, Authentication authentication) {
        Long userId = authService.getUserId(authentication);
        webhookService.deleteWebhook(endpointId, userId);
        return ResponseEntity.noContent().build();
    }
}