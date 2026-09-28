package org.example.Core.Payout;

public record PayoutResult(
        boolean success,
        String txRef,
        String error
) {
    public static PayoutResult ok(String txRef) {
        return new PayoutResult(true, txRef, null);
    }

    public static PayoutResult failed(String error) {
        return new PayoutResult(false, null, error);
    }
}
