package org.example.Core.User.Provider;
import org.example.Core.User.Model.User;
import org.example.Infra.Persistence.User.UserRepository;
import org.example.Core.Authentication.Exception.AuthenticationCustomException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.SQLException;
public class UserProvider {
    private static final Logger logger = LoggerFactory.getLogger(UserProvider.class.getName());
    private final UserRepository userRepository;
    public UserProvider() {
        this.userRepository = new UserRepository();
    }
    public User getUser(String userId) throws Exception {
        try {
            if (userId == null || userId.trim().isEmpty()) {
                throw new IllegalArgumentException("User ID cannot be null or empty");
            }
            User user = userRepository.findUserById(userId);
            if (user == null) {
                logger.warn("User not found with ID: {}", userId);
                throw new AuthenticationCustomException("User not found");
            }
            logger.info("Successfully retrieved user with ID: {}", userId);
            return user;
        } catch (SQLException e) {
            logger.error("Database error while fetching user: {}", e.getMessage());
            throw new AuthenticationCustomException("Failed to fetch user", e);
        }
    }
}