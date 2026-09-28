package org.example.Core.Airdrop.Model;

public record AirdropEvent(
        long id,
        String airdropId,
        String type,
        String message,
        String createdAt
) {
}
