package ferry.exceptions;

public class WebhookEndpointInactiveException extends RuntimeException {
    public WebhookEndpointInactiveException(String message) {
        super(message);
    }
}
