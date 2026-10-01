package org.example.Core.User.Service;

import org.example.Config.Config;
import org.example.Core.User.Model.GoogleAuthResponse;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class GoogleAuthService {
    private static final String GOOGLE_USER_INFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final Logger logger = LoggerFactory.getLogger(GoogleAuthService.class.getName());
    private final String clientId = Config.getClientId();
    private final String clientSecret = Config.getClientSecret();
    private final Map<String, String> redirectUri = Config.getOAuthRedirectUris();
    public GoogleAuthResponse fetchGoogleUserInfo(String code, String host) throws Exception {
        JSONObject tokens = null;
        Exception lastException = null;
        try {
            if (host == null) tokens = exchangeCodeForTokens(code, redirectUri.get("prod"));
            else if (host.contains("localhost")) tokens = exchangeCodeForTokens(code, redirectUri.get("local"));
            else if (host.contains("l1-portal-d.vercel.app")) tokens = exchangeCodeForTokens(code, redirectUri.get("dev"));
            else if (host.contains("l1-portal.vercel.app")) tokens = exchangeCodeForTokens(code, redirectUri.get("prod"));
        } catch (Exception e) {
            lastException = e;
        }
        if (tokens == null) {
            String errorMessage = "Failed to exchange code for tokens: " +
                    (lastException != null ? lastException.getMessage() : "Unknown error");
            throw new RuntimeException(errorMessage, lastException);
        }
        String accessToken = tokens.getString("access_token");
        String refreshToken = tokens.optString("refresh_token");
        long expiresIn = tokens.getLong("expires_in");
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(GOOGLE_USER_INFO_URL))
                .GET()
                .header("Authorization", "Bearer " + accessToken)
                .build();
        HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString());
        int responseCode = response.statusCode();
        if (responseCode != 200) {
            throw new RuntimeException("Failed to fetch user info from Google. Response code: " + responseCode);
        }
        JSONObject userInfo = new JSONObject(response.body());
        return new GoogleAuthResponse(
                userInfo,
                refreshToken,
                expiresIn
        );
    }
    private JSONObject exchangeCodeForTokens(String code, String redirectUri) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        String postData = String.format(
                "code=%s&client_id=%s&client_secret=%s&redirect_uri=%s&grant_type=authorization_code",
                URLEncoder.encode(code, StandardCharsets.UTF_8),
                URLEncoder.encode(clientId, StandardCharsets.UTF_8),
                URLEncoder.encode(clientSecret, StandardCharsets.UTF_8),
                URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
        );
        logger.info("Exchanging Google authorization code for tokens (redirect URI: {})", redirectUri);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GOOGLE_TOKEN_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(postData))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            logger.error("Token exchange error: {}", response.body());
            throw new RuntimeException("Failed to exchange code for tokens. Response code: " + response.statusCode());
        }
        return new JSONObject(response.body());
    }
}