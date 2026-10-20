package ferry.exceptions;

public class WebhookEventNotFound extends RuntimeException {
    public WebhookEventNotFound(String message) {
        super(message);
    }
}
