package ferry_delivery_worker.services;

import ferry_delivery_worker.dto.PayloadDto;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@AllArgsConstructor
public class PostService {

    private final RestTemplate restTemplate;

    public ResponseEntity<String> post(
            String url,
            PayloadDto payload,
            Map<String, String> incomingHeaders
    ) {

        HttpHeaders headers = new HttpHeaders();

        //copy only application-level headers
        if (incomingHeaders != null) {

            incomingHeaders.forEach((key, value) -> {

                //don't forward transport-specific headers
                if (!key.equalsIgnoreCase("Host")
                        && !key.equalsIgnoreCase("Content-Length")
                        && !key.equalsIgnoreCase("Connection")) {

                    headers.set(key, value);
                }
            });
        }

        //create the outgoing request
        HttpEntity<PayloadDto> entity =
                new HttpEntity<>(payload, headers);

        try {

            //send webhook to destination
            return restTemplate.postForEntity(
                    url,
                    entity,
                    String.class
            );

        } catch (HttpStatusCodeException e) {

            //RestTemplate throws for 4xx/5xx
            //convert it back into ResponseEntity
            return ResponseEntity
                    .status(e.getStatusCode())
                    .body(e.getResponseBodyAsString());
        }

        //network errors such as timeout, DNS failure,
        //connection refused etc. propagate as exceptions
        //and will be treated as retryable by the consumer
    }
}