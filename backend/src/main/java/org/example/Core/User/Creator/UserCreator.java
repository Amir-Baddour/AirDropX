package org.example.Core.User.Creator;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.example.Config.Config;
import org.example.Core.Authentication.Exception.AuthenticationCustomException;
import org.example.Core.Authentication.Model.Token;
import org.example.Core.User.Model.GoogleAuthResponse;
import org.example.Core.User.Model.User;
import org.example.Core.User.Service.GoogleAuthService;
import org.example.Core.User.UserToken.Model.UserToken;
import org.example.Core.User.UserToken.UserTokenCreator;
import org.example.Infra.Persistence.User.UserRepository;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Date;
public class UserCreator {
    private static final Logger logger = LoggerFactory.getLogger(UserCreator.class.getName());
    private final GoogleAuthService googleAuthService;
    private final UserRepository userRepository;
    private final UserTokenCreator userTokenCreator;
    public UserCreator() {
        this.googleAuthService = new GoogleAuthService();
        this.userRepository = new UserRepository();
        this.userTokenCreator = new UserTokenCreator();
    }
    public User processUserAuthentication(String provider, String accessToken, String host) throws Exception {
        validateAuthenticationInput(provider, accessToken);
        if (provider.equalsIgnoreCase("GOOGLE")) {
            return processGoogleAuthentication(accessToken, host);
        }
        throw new IllegalArgumentException("Unsupported provider: " + provider);
    }
    private User processGoogleAuthentication(String accessToken, String host) throws Exception {
        try {
            GoogleAuthResponse googleResponse = googleAuthService.fetchGoogleUserInfo(accessToken, host);
            JSONObject userInfo = googleResponse.userInfo();
            validateGoogleUserInfo(userInfo);
            String email = userInfo.getString("email");
            String providerId = userInfo.getString("sub");
            String fullName = userInfo.getString("name");
            String pfp = userInfo.optString("picture", null);
            String address = userInfo.optString("address", null);
            User existingUser = userRepository.findUserByProvider("GOOGLE", fullName);
            User user;
            if (existingUser != null) {
                user = existingUser;
                if (hasChanges(user, fullName, pfp, address)) {
                    userRepository.updateUser(user.id(), fullName, pfp, address);
                }
            } else {
                user = userRepository.createUser(
                        email,
                        fullName,
                        "GOOGLE",
                        providerId,
                        pfp,
                        address
                );

            }
            logger.info("User is created, google response is >> {}", googleResponse);
            UserToken userToken = userTokenCreator.createOrUpdateToken(
                    user,
                    googleResponse.refreshToken(),
                    googleResponse.refreshTokenExpiresIn()
            );
            String jwtToken = generateJwtToken(user);
            Token resultToken = new Token(jwtToken, userToken.expiresAt().getTime());
            return new User(
                    user.id(),
                    user.username(),
                    user.provider(),
                    user.providerId(),
                    user.pfp(),
                    user.address(),
                    user.role(),
                    resultToken
            );
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error processing Google authentication: {}", e.getMessage());
            throw new AuthenticationCustomException("Failed to process Google authentication", e);
        }
    }
    private String generateJwtToken(User user) {
        logger.info("Generating JWT token for user: {}", user.id());
        long now = System.currentTimeMillis();
        long expiryTime = now + (24 * 60 * 60 * 1000);
        return Jwts.builder()
                .setSubject(user.id())
                .claim("username", user.username())
                .claim("provider", user.provider())
                .claim("role", user.role() != null ? user.role().name() : null)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(expiryTime))
                .signWith(SignatureAlgorithm.HS256, Config.getJwtSecret())
                .compact();
    }
    private boolean hasChanges(User user, String newName, String newPfp, String newAddress) {
        return !user.username().equals(newName) ||
                (newPfp != null && !newPfp.equals(user.pfp())) ||
                (newAddress != null && !newAddress.equals(user.address()));
    }
    private void validateAuthenticationInput(String provider, String accessToken) {
        if (provider == null || provider.trim().isEmpty()) {
            throw new IllegalArgumentException("Provider cannot be null or empty");
        }
        if (accessToken == null || accessToken.trim().isEmpty()) {
            throw new IllegalArgumentException("Access token cannot be null or empty");
        }
    }
    private void validateGoogleUserInfo(JSONObject userInfo) {
        if (!userInfo.has("email")) {
            throw new IllegalArgumentException("Email is required in Google user info");
        }
        if (!userInfo.has("sub")) {
            throw new IllegalArgumentException("Subject ID is required in Google user info");
        }
    }
}