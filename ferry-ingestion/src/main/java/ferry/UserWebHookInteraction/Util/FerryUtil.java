package ferry.UserWebHookInteraction.Util;
import java.security.SecureRandom;

//utility class
public class FerryUtil {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";

    //generates cryptographically secure string
    public String generateSecureString() {

        StringBuilder sb = new StringBuilder(32);
        int charSetSize = CHARACTERS.length();

        for (int i = 0; i < 32; i++) {
            // Get a random index within the character set bounds
            int randomIndex = RANDOM.nextInt(charSetSize);
            sb.append(CHARACTERS.charAt(randomIndex));
        }
        return sb.toString();
    }
    public String generateSecureString(int size) {

        StringBuilder sb = new StringBuilder(size);
        int charSetSize = CHARACTERS.length();

        for (int i = 0; i < size; i++) {
            // Get a random index within the character set bounds
            int randomIndex = RANDOM.nextInt(charSetSize);
            sb.append(CHARACTERS.charAt(randomIndex));
        }
        return sb.toString();
    }
}
