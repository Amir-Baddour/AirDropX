package org.example.Core.User.Exception;

public class UserDoesNotHaveEnoughPermissionsException extends Exception {
    public UserDoesNotHaveEnoughPermissionsException(String message) {
        super(message);
    }
    public UserDoesNotHaveEnoughPermissionsException(String message, Throwable cause) {
        super(message, cause);
    }
}