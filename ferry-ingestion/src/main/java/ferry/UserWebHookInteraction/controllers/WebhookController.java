package ferry.UserWebHookInteraction.controllers;


import ferry.UserWebHookInteraction.dtos.WebhookEndpointCreateRequest;
import ferry.UserWebHookInteraction.dtos.WebhookEndpointCreatedResponse;
import ferry.UserWebHookInteraction.dtos.WebhookResponseWithUsage;
import ferry.UserWebHookInteraction.dtos.WebhooksResponse;
import ferry.UserWebHookInteraction.services.WebhookService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/endpoints")
public class WebhookController {

    private final WebhookService webhookService;
    //This endpoint creates a unique id for a receiving a webhook
    //input : name of the url, destination url
    //output: endpoint, secret token

    @GetMapping()
    public ResponseEntity<?> getEndpoints(){
        //1) validate the user
        Long userId = 123l;
        WebhooksResponse webhooksResponse = webhookService.readWebhooks(userId);
        return new ResponseEntity<>(webhooksResponse, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getEndpoint(@RequestParam String id){
        //1) validate the user
        Long userId = 123l;
        WebhookResponseWithUsage webhookResponseWithUsage = webhookService.readWebhook(id,userId);
        return new ResponseEntity<>(webhookResponseWithUsage, HttpStatus.OK);
    }

    @PostMapping()
    public ResponseEntity<?> createEndpoint(@RequestBody WebhookEndpointCreateRequest request) throws Exception {
        // 1) validate the user and get the user id
        Long userId = 1234l;
        WebhookEndpointCreatedResponse response = webhookService.createWebhook(userId,request.getName(), request.getDestinationUrl());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}/disable")
    public ResponseEntity<?> disableEndpoint(@PathVariable String id){
        //1) validate the user
        Long userId =123l;
        webhookService.disableWebhook(id,userId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEndpoint(@PathVariable String id){
        //1) validate the user
        Long userId = 123l;
        webhookService.deleteWebhook(id,userId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }


    //replay
//    User clicks Replay
//       ↓
//    Ferry takes stored event from DLQ
//       ↓
//    puts it back into RabbitMQ
//       ↓
//    Worker attempts delivery again
//       ↓
//    Consumer receives it


    //updating we can update the name, im not sur what else
}
