package ferry.exceptions;

public class WebhookUsageNotFoundException extends RuntimeException {
    public WebhookUsageNotFoundException(String message) {
        super(message);
    }
}
