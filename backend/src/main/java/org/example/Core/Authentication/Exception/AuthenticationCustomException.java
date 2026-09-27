package org.example.Core.Authentication.Exception;

public class AuthenticationCustomException extends Exception {
    public AuthenticationCustomException(String message) {
        super(message);
    }
    public AuthenticationCustomException(String message, Throwable cause) {
        super(message, cause);
    }
}
