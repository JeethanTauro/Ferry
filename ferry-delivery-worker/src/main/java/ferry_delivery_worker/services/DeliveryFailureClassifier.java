package ferry_delivery_worker.services;


import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class DeliveryFailureClassifier {

    public boolean isSuccess(ResponseEntity<?> response) {
        return response.getStatusCode().is2xxSuccessful();
    }

    public boolean isRetryable(ResponseEntity<?> response) {

        int status = response.getStatusCode().value();

        return status == 429 || status >= 500;
    }
}