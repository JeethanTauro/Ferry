package ferry.exceptions;


public class AccountDeletionException extends RuntimeException {

    public AccountDeletionException(String message, Throwable cause) {
        super(message, cause);
    }
}
