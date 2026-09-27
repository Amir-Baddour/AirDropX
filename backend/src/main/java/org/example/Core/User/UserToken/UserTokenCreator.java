package org.example.Core.User.UserToken;
import org.example.Core.Authentication.Exception.AuthenticationCustomException;
import org.example.Core.User.Model.User;
import org.example.Core.User.UserToken.Model.UserToken;
import org.example.Infra.Persistence.UserToken.UserTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
public class UserTokenCreator {
    private static final Logger logger = LoggerFactory.getLogger(UserTokenCreator.class.getName());
    private final UserTokenRepository tokenRepository;
    public UserTokenCreator() {
        this.tokenRepository = new UserTokenRepository();
    }
    public UserToken createOrUpdateToken(User user, String refreshToken, long expiresAt) throws Exception {
        try {
            validateTokenInput(user, refreshToken);
            Timestamp expiresAtTime = Timestamp.from(Instant.now().plus(expiresAt, ChronoUnit.HOURS));
            Optional<UserToken> existingToken = tokenRepository.findTokenByUserId(user.id());
            if (existingToken.isPresent()) {
                UserToken token = existingToken.get();
                if (shouldUpdateToken(token, refreshToken)) {
                    tokenRepository.updateToken(token.id(), refreshToken, expiresAtTime);
                    return tokenRepository.findTokenByUserId(user.id())
                            .orElseThrow(() -> new AuthenticationCustomException("Token not found after update"));
                }
                return token;
            } else {
                return tokenRepository.createToken(user.id(), refreshToken, expiresAtTime);
            }
        } catch (Exception e) {
            logger.error("Error processing token creation: {}", e.getMessage());
            throw new AuthenticationCustomException("Failed to create or update token", e);
        }
    }
    private boolean shouldUpdateToken(UserToken token, String newRefreshToken) {
        if (!token.refreshToken().equals(newRefreshToken)) {
            return true;
        }
        Instant sixHoursFromNow = Instant.now().plus(6, ChronoUnit.HOURS);
        return token.expiresAt().toInstant().isBefore(sixHoursFromNow);
    }
    private void validateTokenInput(User user, String refreshToken) {
        validateUser(user);
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new IllegalArgumentException("Refresh token cannot be null or empty");
        }
    }
    private void validateUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (user.id() == null || user.id().trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
    }
}