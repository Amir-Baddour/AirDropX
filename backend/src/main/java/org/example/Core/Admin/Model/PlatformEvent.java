package org.example.Core.Admin.Model;

/** An airdrop timeline event, with the company and airdrop it belongs to (platform activity feed). */
public record PlatformEvent(
        long id,
        String companyName,
        String airdropId,
        String airdropName,
        String type,
        String message,
        String createdAt
) {
}
