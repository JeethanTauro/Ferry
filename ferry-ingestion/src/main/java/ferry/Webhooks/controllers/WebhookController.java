package ferry.Webhooks.controllers;


import ferry.Webhooks.services.WebhookEventService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

//so the full endpoint is going to be http://ferry.com/webhooks/{id}
@RequestMapping("/webhooks")
@AllArgsConstructor
@RestController
public class WebhookController {

    private final WebhookEventService webhookEventService;
    @PostMapping("/{endpointId}")
    public ResponseEntity<?> createWebhooks(@PathVariable String endpointId , @RequestHeader HttpHeaders httpHeaders, @RequestBody String payload) throws Exception {
        webhookEventService.storeWebhookEvent(endpointId,httpHeaders,payload);
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }
}
