package org.example.Core.User.Exception;

/** Wrong email or password. The message is deliberately the same for both cases. */
public class InvalidCredentialsException extends Exception {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
