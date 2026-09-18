package ferry.UserWebHookInteraction.controllers;

import ferry.Auth.services.AuthService;
import ferry.UserWebHookInteraction.dtos.WebhookEndpointCreateRequest;
import ferry.UserWebHookInteraction.dtos.WebhookEndpointCreatedResponse;
import ferry.UserWebHookInteraction.dtos.WebhookResponseWithUsage;
import ferry.UserWebHookInteraction.dtos.WebhooksResponse;
import ferry.UserWebHookInteraction.services.WebhookService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/endpoints")
public class UserInteractionWebhookController {

    private final WebhookService webhookService;
    private final AuthService authService;

    @GetMapping
    public ResponseEntity<WebhooksResponse> getEndpoints(Authentication authentication) {
        Long userId = authService.getUserId(authentication);
        WebhooksResponse response = webhookService.readWebhooks(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WebhookResponseWithUsage> getEndpoint(@PathVariable String id, Authentication authentication) {

        Long userId = authService.getUserId(authentication);
        WebhookResponseWithUsage response = webhookService.readWebhook(id, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<WebhookEndpointCreatedResponse> createEndpoint(@RequestBody WebhookEndpointCreateRequest request, Authentication authentication) throws Exception {
        Long userId = authService.getUserId(authentication);
        WebhookEndpointCreatedResponse response = webhookService.createWebhook(
                        userId,
                        request.getName(),
                        request.getDestinationUrl(),
                        request.getProvider()
                );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}/disable")
    public ResponseEntity<Void> disableEndpoint(@PathVariable String id, Authentication authentication) {

        Long userId = authService.getUserId(authentication);
        webhookService.disableWebhook(id, userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/enable")
    public ResponseEntity<Void> enableEndpoint(@PathVariable String id, Authentication authentication) {

        Long userId = authService.getUserId(authentication);
        webhookService.enableWebhook(id, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEndpoint(@PathVariable String id, Authentication authentication) {
        Long userId = authService.getUserId(authentication);
        webhookService.deleteWebhook(id, userId);
        return ResponseEntity.noContent().build();
    }
}