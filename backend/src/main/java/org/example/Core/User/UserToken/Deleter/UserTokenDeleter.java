package org.example.Core.User.UserToken.Deleter;
import org.example.Core.Authentication.Exception.AuthenticationCustomException;
import org.example.Infra.Persistence.UserToken.UserTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.SQLException;
public class UserTokenDeleter {
    private static final Logger logger = LoggerFactory.getLogger(UserTokenDeleter.class.getName());
    private final UserTokenRepository userTokenRepository;
    public UserTokenDeleter() {
        this.userTokenRepository = new UserTokenRepository();
    }
    public void deleteUserTokens(String userId) throws Exception {
        try {
            validateUserId(userId);
            userTokenRepository.deleteTokensByUserId(userId);
            logger.info("Successfully deleted tokens for user: {}", userId);
        } catch (SQLException e) {
            logger.error("Failed to delete user tokens: {}", e.getMessage());
            throw new AuthenticationCustomException("Failed to delete user tokens", e);
        }
    }
    private void validateUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
    }
}