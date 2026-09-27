package org.example.Api.User;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.example.Config.Config;
import org.example.Core.Authentication.Exception.AuthenticationCustomException;
import org.example.Core.Authentication.Session.SessionProvider;
import org.example.Core.User.Model.User;
import org.example.Core.User.Creator.UserCreator;
import org.example.Core.User.Updater.UserUpdater;
import org.example.Core.User.UserToken.Deleter.UserTokenDeleter;
import org.example.Middleware.AuthorizationMiddleware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.HaltException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import static spark.Spark.*;
public class UserApi {
    private static final Logger logger = LoggerFactory.getLogger(UserApi.class.getName());
    private final UserCreator userCreator;
    private final UserUpdater userUpdater;
    private final UserTokenDeleter userTokenDeleter;
    private final SessionProvider sessionProvider;
    public UserApi() {
        this.userCreator = new UserCreator();
        this.userUpdater = new UserUpdater();
        this.userTokenDeleter = new UserTokenDeleter();
        this.sessionProvider = new SessionProvider();
    }
    public void initializeRoutes() {
        post("/auth/login/oauth", (req, res) -> {
            try {
                logger.info("Received authentication request");
                String host = req.headers("Origin");
                String referer = req.headers("Referer");
                logger.info("Request Origin: " + host);
                logger.info("Request Referer: " + referer);
                JsonObject requestJson = JsonParser.parseString(req.body()).getAsJsonObject();
                if (!requestJson.has("provider") || !requestJson.has("code")) {
                    res.status(400);
                    JsonObject response = new JsonObject();
                    response.addProperty("success", false);
                    response.addProperty("message", "Provider and access token are required");
                    response.add("data", null);
                    return response;
                }
                String provider = requestJson.get("provider").getAsString().toUpperCase();
                String code = requestJson.get("code").getAsString();
                User authenticatedUser = userCreator.processUserAuthentication(provider, URLDecoder.decode(code, StandardCharsets.UTF_8), host);
                return getJsonObject(authenticatedUser);
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid authentication request: {}", e.getMessage());
                res.status(400);
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", e.getMessage());
                response.add("data", null);
                return response;
            } catch (AuthenticationCustomException e) {
                logger.warn("Authentication failed: {}", e.getMessage());
                res.status(401);
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Authentication failed:" + e.getMessage());
                response.add("data", null);
                return response;
            } catch (Exception e) {
                logger.error("Error during authentication: {}", e.getMessage());
                res.status(500);
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Internal server error");
                response.add("data", null);
                return response;
            }
        });
        put("/admin/user-role/:userId", (req, res) -> {
            try {
                logger.info("Received user role update request");
                res.type("application/json");
                AuthorizationMiddleware.requireSuperAdmin(req, res);
                String userId = req.params(":userId");
                JsonObject requestJson = JsonParser.parseString(req.body()).getAsJsonObject();
                if (!requestJson.has("role")) {
                    res.status(400);
                    JsonObject response = new JsonObject();
                    response.addProperty("success", false);
                    response.addProperty("message", "Role ID is required");
                    response.add("data", null);
                    return response;
                }
                String role = requestJson.get("role").getAsString();
                userUpdater.updateUserRole(userId, role);
                JsonObject result = new JsonObject();
                result.addProperty("success", true);
                result.addProperty("message", "User role updated successfully");
                return result;
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid role update request: {}", e.getMessage());
                res.status(400);
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", e.getMessage());
                response.add("data", null);
                return response;
            } catch (AuthenticationCustomException e) {
                logger.warn("Role update failed: {}", e.getMessage());
                res.status(401);
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Role update failed");
                response.add("data", null);
                return response;
            } catch (Exception e) {
                logger.error("Error during role update: {}", e.getMessage());
                res.status(500);
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Internal server error");
                response.add("data", null);
                return response;
            }
        });
        delete("/auth/logout", (req, res) -> {
            try {
                String userId = AuthorizationMiddleware.authenticateAndGetCallerId(req, res);
                res.type("application/json");
                userTokenDeleter.deleteUserTokens(userId);
                String cookieName = Config.getCookieName();
                if (req.cookie(cookieName) != null) {
                    res.removeCookie(cookieName);
                }
                JsonObject response = new JsonObject();
                response.addProperty("message", "Logout successful");
                return response;
            } catch (HaltException e) {
                throw e;
            } catch (Exception e) {
                logger.error("Error during logout: {}", e.getMessage());
                res.status(500);
                JsonObject response = new JsonObject();
                response.addProperty("message", "Internal server error");
                return response;
            }
        });
        get("/auth/session", (req, res) -> {
            try {
                String token = req.headers("Authorization");
                if (token == null || !token.startsWith("Bearer ")) {
                    res.status(401);
                    JsonObject response = new JsonObject();
                    response.addProperty("message", "auth required");
                    response.addProperty("code", "401__AUTH__UNKNOWN_USER");
                    return response;
                }
                token = token.substring(7);
                Claims claims = Jwts.parser()
                        .setSigningKey(Config.getJwtSecret())
                        .parseClaimsJws(token)
                        .getBody();
                String userId = claims.getSubject();
                long expiresAt = claims.getExpiration().getTime() / 1000;
                SessionProvider.SessionResponse session = sessionProvider.getSession(userId, token, expiresAt);
                JsonObject userJson = new JsonObject();
                userJson.addProperty("id", session.getUser().id());
                userJson.addProperty("username", session.getUser().username());
                userJson.addProperty("provider", session.getUser().provider());
                userJson.addProperty("provider_id", session.getUser().providerId());
                userJson.addProperty("address", session.getUser().address());
                userJson.addProperty("role", session.getUser().role().name());
                userJson.addProperty("pfp", session.getUser().pfp());
                JsonObject authJson = new JsonObject();
                authJson.addProperty("access_token", session.getAuth().access_token());
                authJson.addProperty("expires_at", String.valueOf(session.getAuth().expires_at()));
                JsonObject response = new JsonObject();
                response.add("user", userJson);
                response.add("auth", authJson);
                res.type("application/json");
                return response;
            } catch (JwtException e) {
                res.status(401);
                JsonObject response = new JsonObject();
                response.addProperty("message", "auth required");
                response.addProperty("code", "401__AUTH__UNKNOWN_USER");
                return response;
            } catch (Exception e) {
                logger.error("Error retrieving session: {}", e.getMessage());
                res.status(500);
                JsonObject response = new JsonObject();
                response.addProperty("message", "Internal server error");
                return response;
            }
        });
    }
    private static JsonObject getJsonObject(User authenticatedUser) {
        JsonObject userJson = new JsonObject();
        userJson.addProperty("id", authenticatedUser.id());
        userJson.addProperty("username", authenticatedUser.username());
        userJson.addProperty("provider", authenticatedUser.provider());
        userJson.addProperty("provider_id", authenticatedUser.providerId());
        userJson.addProperty("pfp", authenticatedUser.pfp());
        userJson.addProperty("address", authenticatedUser.address());
        if (authenticatedUser.role() != null) {
            JsonObject roleJson = new JsonObject();
            roleJson.addProperty("id", authenticatedUser.role().id());
            roleJson.addProperty("name", authenticatedUser.role().name());
            userJson.add("role", roleJson);
        }
        if (authenticatedUser.token() != null) {
            JsonObject tokenJson = new JsonObject();
            tokenJson.addProperty("access_token", authenticatedUser.token().access_token());
            tokenJson.addProperty("expires_at", authenticatedUser.token().expires_at());
            userJson.add("token", tokenJson);
        }
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("message", "Authentication successful");
        response.add("data", userJson);
        return response;
    }
}