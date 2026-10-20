package ferry.Webhooks.Util;

import ferry.UserWebHookInteraction.entities.Provider;
import org.springframework.http.HttpHeaders;

public interface WebhookSignatureVerifier {
    Provider getSupportedProvider();
    boolean verify(String payload,HttpHeaders headers,String secret);
}
