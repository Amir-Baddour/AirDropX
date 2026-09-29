package org.example.Core.Claim.Model;

public record PublicClaimStatus(
        String airdropName,
        String tokenSymbol,
        String address,
        ClaimStatus status,
        String rejectReason,
        String payoutStatus,
        String txRef
) {
}
