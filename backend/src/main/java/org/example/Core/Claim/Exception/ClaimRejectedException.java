package org.example.Core.Claim.Exception;

import java.util.List;

/** A submitted claim failed automatic checks. Nothing is stored, so the claimant can try again. */
public class ClaimRejectedException extends Exception {
    private final List<String> errors;

    public ClaimRejectedException(String message, List<String> errors) {
        super(message);
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}
