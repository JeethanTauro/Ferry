package ferry.exceptions;

public class UnsupportedWebhookProviderException extends RuntimeException {
    public UnsupportedWebhookProviderException(String message) {
        super(message);
    }
}
