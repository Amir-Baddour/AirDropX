package org.example.Core.Claim.Model;

/** Returned once when a claim is created. The token is shown only here; the database keeps its hash. */
public record ClaimSubmission(
        Claim claim,
        String claimToken
) {
}
