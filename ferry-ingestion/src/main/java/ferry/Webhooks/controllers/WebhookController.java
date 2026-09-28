package ferry.Webhooks.controllers;


import ferry.Webhooks.services.WebhookEventService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

//so the full endpoint is going to be http://ferry.com/webhooks/{id}
@RequestMapping("/webhooks")
@AllArgsConstructor
@RestController
@Validated
public class WebhookController {

    private final WebhookEventService webhookEventService;
    @PostMapping("/{endpointId}")
    public ResponseEntity<?> createWebhooks(@PathVariable @NotBlank @Size(min = 25, max = 25) String endpointId , @RequestHeader HttpHeaders httpHeaders, @RequestBody String payload) throws Exception {
        webhookEventService.storeWebhookEvent(endpointId,httpHeaders,payload);
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }
}
