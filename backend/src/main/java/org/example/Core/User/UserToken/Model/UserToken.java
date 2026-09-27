package org.example.Core.User.UserToken.Model;

import org.example.Core.Authentication.Model.Token;

import java.sql.Timestamp;

public record UserToken(
        String id,
        String userId,
        String refreshToken,
        Timestamp expiresAt,
        Timestamp createdAt,
        Timestamp updatedAt
) {
}