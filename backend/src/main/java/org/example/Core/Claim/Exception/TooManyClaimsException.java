package org.example.Core.Claim.Exception;

/** Anti-abuse limit reached (for example too many claims from one network). Maps to HTTP 429. */
public class TooManyClaimsException extends Exception {
    public TooManyClaimsException(String message) {
        super(message);
    }
}
