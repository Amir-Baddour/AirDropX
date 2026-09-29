package org.example.Core.Claim.Model;

public enum ClaimStatus {
    /** All tasks passed; the claimant is (or will be) a recipient. */
    APPROVED,
    /** Waiting for the company to review manual proof. */
    NEEDS_REVIEW,
    /** Rejected by the company. */
    REJECTED
}
