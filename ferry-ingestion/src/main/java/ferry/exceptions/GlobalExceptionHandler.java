package ferry.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EndpointNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEndpointNotFound(
            EndpointNotFoundException ex
    ) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
    }

    @ExceptionHandler(WebhookEndpointInactiveException.class)
    public ResponseEntity<Map<String, Object>> handleEndpointInactive(
            WebhookEndpointInactiveException ex
    ) {
        return buildResponse(
                HttpStatus.GONE,
                ex.getMessage()
        );
    }

    @ExceptionHandler(InvalidWebhookSignatureException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidSignature(
            InvalidWebhookSignatureException ex
    ) {
        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                ex.getMessage()
        );
    }

    @ExceptionHandler(UnsupportedWebhookProviderException.class)
    public ResponseEntity<Map<String, Object>> handleUnsupportedProvider(
            UnsupportedWebhookProviderException ex
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
    }

    @ExceptionHandler(WebhookUsageNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleWebhookUsageNotFound(
            WebhookUsageNotFoundException ex
    ) {
        log.error("Webhook usage record not found", ex);

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getMessage()
        );
    }

    // Catch unexpected exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpectedException(
            Exception ex
    ) {
        log.error("Unexpected exception occurred", ex);

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred"
        );
    }

    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status,
            String message
    ) {

        Map<String, Object> response = Map.of(
                "timestamp", Instant.now(),
                "status", status.value(),
                "message", message
        );

        return ResponseEntity
                .status(status)
                .body(response);
    }
}