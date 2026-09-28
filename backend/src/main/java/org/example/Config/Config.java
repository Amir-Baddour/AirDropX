package org.example.Config;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.HashMap;
import java.util.Map;
public class Config {
    private static final Dotenv dotenv = Dotenv.load();
    public static String getJwtSecret() {
        String secret = dotenv.get("JWT_SECRET");
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException("JWT secret not found in environment variables");
        }
        return secret;
    }
    public static String getDbURL() {
        String dbURL = dotenv.get("DB_URL");
        if (dbURL == null || dbURL.trim().isEmpty()) {
            throw new IllegalStateException("Database URL not found in environment variables");
        }
        return dbURL;
    }
    public static String getClientId() {
        String clientId = dotenv.get("CLIENT_ID");
        if (clientId == null || clientId.trim().isEmpty()) {
            throw new IllegalStateException("Client ID not found in environment variables");
        }
        return clientId;
    }
    public static String getClientSecret() {
        String clientSecret = dotenv.get("CLIENT_SECRET");
        if (clientSecret == null || clientSecret.trim().isEmpty()) {
            throw new IllegalStateException("Client ID not found in environment variables");
        }
        return clientSecret;
    }
    public static String getCookieName() {
        String cookieName = dotenv.get("COOKIE_NAME");
        if (cookieName == null || cookieName.trim().isEmpty()) {
            throw new IllegalStateException("Cookie name not found in environment variables");
        }
        return cookieName;
    }
    public static Map<String, String> getOAuthRedirectUris() {
        Map<String, String> redirectUris = new HashMap<>();
        String prodUri = dotenv.get("OAUTH_REDIRECT_URI");
        if (prodUri == null || prodUri.trim().isEmpty()) {
            throw new IllegalStateException("Production redirect URI not found in environment variables");
        }
        redirectUris.put("prod", prodUri);
        String devUri = dotenv.get("DEV_OAUTH_REDIRECT_URI");
        if (devUri == null || devUri.trim().isEmpty()) {
            throw new IllegalStateException("Local redirect URI not found in environment variables");
        }
        redirectUris.put("dev", devUri);
        String localUri = dotenv.get("LOCAL_OAUTH_REDIRECT_URI");
        if (localUri == null || localUri.trim().isEmpty()) {
            throw new IllegalStateException("Local redirect URI not found in environment variables");
        }
        redirectUris.put("local", localUri);
        return redirectUris;
    }
    public static String getS3BucketEndpoint() {
        String cookieName = dotenv.get("S3_BUCKET_ENDPOINT");
        if (cookieName == null || cookieName.trim().isEmpty()) {
            throw new IllegalStateException("S3 bucket endpoint not found in environment variables");
        }
        return cookieName;
    }
    public static String getS3BucketName() {
        String cookieName = dotenv.get("S3_BUCKET_NAME");
        if (cookieName == null || cookieName.trim().isEmpty()) {
            throw new IllegalStateException("S3 bucket name not found in environment variables");
        }
        return cookieName;
    }
    public static String getS3AccessKey() {
        String accessKey = dotenv.get("S3_ACCESS_KEY");
        if (accessKey == null || accessKey.trim().isEmpty()) {
            throw new IllegalStateException("S3 access key not found in environment variables");
        }
        return accessKey;
    }
    public static String getS3SecretKey() {
        String secretKey = dotenv.get("S3_SECRET_KEY");
        if (secretKey == null || secretKey.trim().isEmpty()) {
            throw new IllegalStateException("S3 secret key not found in environment variables");
        }
        return secretKey;
    }
    public static String getS3BucketRegion() {
        String region = dotenv.get("S3_BUCKET_REGION");
        if (region == null || region.trim().isEmpty()) {
            throw new IllegalStateException("S3 bucket region not found in environment variables");
        }
        return region;
    }
    // Optional settings with defaults: the app starts even if they are missing from .env
    public static double getMockPayoutFailureRate() {
        String value = dotenv.get("MOCK_PAYOUT_FAILURE_RATE", "0.05");
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0.05;
        }
    }
    public static int getWorkerBatchSize() {
        String value = dotenv.get("WORKER_BATCH_SIZE", "20");
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 20;
        }
    }
    public static long getWorkerIntervalSeconds() {
        String value = dotenv.get("WORKER_INTERVAL_SECONDS", "5");
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return 5;
        }
    }
}
