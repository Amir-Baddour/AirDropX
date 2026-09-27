package org.example.Core.User.Model;
import org.example.Core.Authentication.Model.Token;
import org.example.Core.Role.Model.Role;
public record User(
        String id,
        String username,
        String provider,
        String providerId,
        String pfp,
        String address,
        Role role,
        Token token
) {}
