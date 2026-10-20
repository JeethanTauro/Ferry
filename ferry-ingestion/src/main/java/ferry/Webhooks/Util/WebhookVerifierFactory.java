package ferry.Webhooks.Util;

import ferry.UserWebHookInteraction.entities.Provider;
import ferry.exceptions.UnsupportedWebhookProviderException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class WebhookVerifierFactory {

    private final Map<Provider, WebhookSignatureVerifier> verifiers;

    // Spring auto-discovers all @Component verifiers in the project!
    public WebhookVerifierFactory(List<WebhookSignatureVerifier> verifierList) {
        this.verifiers = verifierList.stream()
                .collect(Collectors.toMap(
                        WebhookSignatureVerifier::getSupportedProvider,
                        Function.identity()
                ));
    }

    public WebhookSignatureVerifier getVerifier(Provider provider) {
        WebhookSignatureVerifier verifier = verifiers.get(provider);
        if (verifier == null) {
            throw new UnsupportedWebhookProviderException("Unsupported webhook provider: " + provider);
        }
        return verifier;
    }
}