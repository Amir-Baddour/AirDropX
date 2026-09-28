package org.example.Core.Airdrop.Model;

public record Airdrop(
        String id,
        String companyId,
        String name,
        String description,
        String tokenSymbol,
        AirdropStatus status,
        String createdBy,
        String createdAt,
        String updatedAt,
        String launchedAt,
        String completedAt
) {
}
