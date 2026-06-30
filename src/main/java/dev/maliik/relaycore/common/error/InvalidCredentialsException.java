package dev.maliik.relaycore.common.error;

/**
 * Thrown on a failed login. Deliberately does not reveal whether the username or the password was
 * wrong. Mapped to HTTP 401.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("invalid username or password");
    }
}
