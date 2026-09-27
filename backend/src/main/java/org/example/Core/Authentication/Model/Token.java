package org.example.Core.Authentication.Model;
import java.sql.Timestamp;
public record Token(
        String access_token,
        long expires_at
) {
}
