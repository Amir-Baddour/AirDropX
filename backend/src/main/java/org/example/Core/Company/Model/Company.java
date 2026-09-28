package org.example.Core.Company.Model;

public record Company(
        String id,
        String name,
        String ownerId,
        String status,
        String createdAt
) {
}
