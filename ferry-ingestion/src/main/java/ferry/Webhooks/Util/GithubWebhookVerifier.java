package ferry.Webhooks.Util;

import org.springframework.http.HttpHeaders;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

public class GithubWebhookVerifier implements WebhookSignatureVerfier{
    //so github basically takes the secret provided by the user, takes the payload and then does hmac and sends the signature
    // we have to take our secret (after decrypting from the db), do the same hmac and check whether they match
    @Override
    public boolean verify(
            String payload,
            HttpHeaders headers,
            String secret) {

        String signature = headers.getFirst("X-Hub-Signature-256");

        if (signature == null || !signature.startsWith("sha256=")) {
            return false;
        }

        String expectedSignature = hmacSha256(payload, secret);

        return MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                signature.substring(7).getBytes(StandardCharsets.UTF_8)
        );
    }

    private String hmacSha256(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            ));

            byte[] hash = mac.doFinal(
                    payload.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
