package org.example.Core.Airdrop.Exception;

import java.util.List;

public class AirdropValidationException extends Exception {
    private final List<String> errors;

    public AirdropValidationException(String message, List<String> errors) {
        super(message);
        this.errors = List.copyOf(errors);
    }

    public AirdropValidationException(String message) {
        this(message, List.of());
    }

    public List<String> getErrors() {
        return errors;
    }
}
