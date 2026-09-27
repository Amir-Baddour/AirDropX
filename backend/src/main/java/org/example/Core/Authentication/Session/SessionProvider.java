package org.example.Core.Authentication.Session;
import org.example.Core.Authentication.Exception.AuthenticationCustomException;
import org.example.Core.User.Model.User;
import org.example.Core.User.Provider.UserProvider;
import org.example.Core.Authentication.Model.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class SessionProvider {
    private static final Logger logger = LoggerFactory.getLogger(SessionProvider.class.getName());
    private final UserProvider userProvider;
    public class SessionResponse {
        private final User user;
        private final Token auth;
        public SessionResponse(User user, Token auth) {
            this.user = user;
            this.auth = auth;
        }
        public User getUser() {
            return user;
        }
        public Token getAuth() {
            return auth;
        }
    }
    public SessionProvider() {
        this.userProvider = new UserProvider();
    }
    public SessionResponse getSession(String userId, String accessToken, long expiresAt) throws Exception {
        try {
            validateInput(userId, accessToken);

            User user = userProvider.getUser(userId);
            Token token = new Token(accessToken, expiresAt);

            logger.info("Successfully retrieved session for user: {}", userId);
            return new SessionResponse(user, token);

        } catch (Exception e) {
            logger.error("Error retrieving session: {}", e.getMessage());
            throw new AuthenticationCustomException("Failed to retrieve session", e);
        }
    }
    private void validateInput(String userId, String accessToken) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        if (accessToken == null || accessToken.trim().isEmpty()) {
            throw new IllegalArgumentException("Access token cannot be null or empty");
        }
    }
}