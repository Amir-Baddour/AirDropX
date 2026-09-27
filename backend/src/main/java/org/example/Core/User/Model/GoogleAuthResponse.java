package org.example.Core.User.Model;
import org.json.JSONObject;
public record GoogleAuthResponse(
        JSONObject userInfo,
        String refreshToken,
        long refreshTokenExpiresIn
) { }