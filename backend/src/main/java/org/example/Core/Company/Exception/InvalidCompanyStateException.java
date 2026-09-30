package org.example.Core.Company.Exception;

/** The company is not in the status the action expects (e.g. suspending an already suspended company). */
public class InvalidCompanyStateException extends Exception {
    public InvalidCompanyStateException(String message) {
        super(message);
    }
}
