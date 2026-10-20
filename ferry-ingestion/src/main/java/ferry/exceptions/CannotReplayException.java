package ferry.exceptions;

public class CannotReplayException extends RuntimeException {
    public CannotReplayException(String message) {
        super(message);
    }
}
