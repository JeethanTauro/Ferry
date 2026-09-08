package ferry.UserWebHookInteraction.services;

import ferry.UserWebHookInteraction.Util.FerryUtil;
import ferry.UserWebHookInteraction.dtos.*;
import ferry.UserWebHookInteraction.entities.Provider;
import ferry.UserWebHookInteraction.entities.WebhookEndpoint;
import ferry.UserWebHookInteraction.entities.WebhookUsage;
import ferry.UserWebHookInteraction.repos.WebhookEndpointRepo;
import ferry.UserWebHookInteraction.repos.WebhookUsageRepo;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
//user facing services
public class WebhookService {
    //create webhook : Done
    //delete webhook : Done
    //disable webhook : Done
    //update webhook
    //read a single webhook :Done
    //read all the webhooks for a specific user (so that the dashboard can show the data) : Done

    @Value("${FERRY_PUBLIC_URL}")
    String baseUrl;

    private final PasswordEncoder passwordEncoder;
    private final WebhookEndpointRepo webhookEndpointRepo;
    private final WebhookUsageRepo webhookUsageRepo;
    private final EncryptionService encryptionService;


    //creating the webhook and saving in the db
    public WebhookEndpointCreatedResponse createWebhook(Long userId, String name, String destinationUrl, Provider provider) throws Exception {
        //create a random unique id for the endpoint
        //create a random secret
        FerryUtil util = new FerryUtil();
        String secretToken = util.generateSecureString(); //by default the len is 32
        String endpointId = util.generateSecureString(25);
        WebhookEndpoint webhookEndpoint = WebhookEndpoint.builder()
                .endpointId(endpointId)
                .name(name)
                .provider(provider)
                .active(true)
                .endpoint(baseUrl + "/webhooks/" + endpointId)
                .userId(userId)
                .destinationUrl(destinationUrl)
                .secret(encryptionService.encrypt(secretToken))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        webhookEndpointRepo.save(webhookEndpoint);

        WebhookEndpointCreatedResponse response = WebhookEndpointCreatedResponse.builder()
                .endpoint(webhookEndpoint.getEndpoint())
                .secretToken(secretToken)
                .build();

        return response;
        //return the dto as a response
    }


    //disable webhook
    public void disableWebhook(String id, Long userId) {
        WebhookEndpoint webhookEndpoint = webhookEndpointRepo
                .findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Webhook endpoint not found"));
        webhookEndpoint.setActive(false);
        webhookEndpoint.setUpdatedAt(Instant.now());
        webhookEndpointRepo.save(webhookEndpoint);
    }


    //delete webhook (when the user click delete, we wil give a warning saying that this action cannot be redone)
    public void deleteWebhook(String id, Long userId){
        WebhookEndpoint webhookEndpoint = webhookEndpointRepo
                .findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Webhook endpoint not found"));
        webhookEndpointRepo.delete(webhookEndpoint);
        //return success
    }


    //read all the webhooks
    public WebhooksResponse readWebhooks(Long userId){
        //here we would have to do a join with the endpointUsage table so that when the list
        //of the endpoints are on the dashboard we can see the usage
        List<WebhookEndpoint> webhookEndpointList = webhookEndpointRepo.getAllByUserId(userId);
        List<WebhookResponse> webhookResponseList = new ArrayList<>();

        //creating the dto so we dont expose the secret hash
        for(WebhookEndpoint wb : webhookEndpointList){
            WebhookResponse webhookResponse = WebhookResponse.builder()
                    .name(wb.getName())
                    .endpoint(wb.getEndpoint())
                    .createdAt(wb.getCreatedAt())
                    .updatedAt(wb.getUpdatedAt())
                    .destinationUrl(wb.getDestinationUrl())
                    .rateLimit(wb.getRateLimitPerSecond())
                    .active(wb.isActive())
                    .build();
            webhookResponseList.add(webhookResponse);
        }

        //list of all the web hooks
        WebhooksResponse webhooksResponse = WebhooksResponse.builder()
                .webhookResponseList(webhookResponseList)
                .build();

        return webhooksResponse;
    }

    public WebhookResponseWithUsage readWebhook(String id, Long userId) {

        WebhookEndpoint endpoint = webhookEndpointRepo
                .findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Webhook endpoint not found"));

        Optional<WebhookUsage> u = webhookUsageRepo.findByEndpointId(id);
        WebhookUsage webhookUsage = new WebhookUsage();
        if(u.isPresent()){
            webhookUsage = u.get();
        }
        else{
            webhookUsage = null;
        }



        WebhookResponse response = WebhookResponse.builder()
                .name(endpoint.getName())
                .endpoint(endpoint.getEndpoint())
                .createdAt(endpoint.getCreatedAt())
                .updatedAt(endpoint.getUpdatedAt())
                .destinationUrl(endpoint.getDestinationUrl())
                .rateLimit(endpoint.getRateLimitPerSecond())
                .active(endpoint.isActive())
                .build();

        WebhookResponseUsage responseUsage = WebhookResponseUsage.builder()
                .eventsReceived(webhookUsage != null ? webhookUsage.getEventsReceived() : 0L)
                .eventsDelivered(webhookUsage != null ? webhookUsage.getEventsDelivered() : 0L)
                .eventsFailed(webhookUsage != null ? webhookUsage.getEventsFailed() : 0L)
                .build();

        return WebhookResponseWithUsage.builder()
                .webhookResponse(response)
                .webhookResponseUsage(responseUsage)
                .build();
    }
}
