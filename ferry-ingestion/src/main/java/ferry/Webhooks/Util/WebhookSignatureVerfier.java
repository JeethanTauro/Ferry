package ferry.Webhooks.Util;

import org.springframework.http.HttpHeaders;

public interface WebhookSignatureVerfier {
    boolean verify(
            String payload,
            HttpHeaders headers,
            String secret
    );
}
