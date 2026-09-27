package org.example.Middleware;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import org.example.Config.Config;
import org.example.Core.Authentication.Exception.AuthenticationCustomException;
import org.example.Core.Role.Model.Role;
import org.example.Core.User.Model.User;
import org.example.Core.User.Provider.UserProvider;
import org.example.Infra.Persistence.Role.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.sql.SQLException;
import java.util.Optional;

import static spark.Spark.halt;

public class AuthorizationMiddleware {
    private static final Logger logger = LoggerFactory.getLogger(AuthorizationMiddleware.class.getName());
    private static final String cookieName = Config.getCookieName();
    private static final UserProvider userProvider = new UserProvider();
    private static final RoleRepository roleRepository = new RoleRepository();

    public static void requireSuperAdmin(Request req, Response res) {
        logger.info("Starting superadmin authorization check for path: {}", req.pathInfo());
        if (req.raw().getMethod().equals("OPTIONS")) {
            logger.info("OPTIONS request - skipping authorization");
            return;
        }

        logger.info("Authenticating request and getting caller ID");
        String callerId = authenticateAndGetCallerId(req, res);
        logger.info("Authentication successful for user ID: {}", callerId);

        try {
            logger.info("Checking if user {} is a superadmin", callerId);
            if (!isUserSuperAdmin(callerId)) {
                logger.warn("User {} attempted to access superadmin-only endpoint: {}", callerId, req.pathInfo());
                halt(403, "UNAUTHORIZED_ACCESS");
            }

            logger.info("User {} confirmed as superadmin", callerId);
            // Store the verified caller ID for use in the route handler
            req.attribute("callerId", callerId);

        } catch (Exception e) {
            logger.error("Error checking superadmin status for user {} {}", callerId, e.getMessage());
            logger.error("Stack trace:", e);
            halt(403, "UNAUTHORIZED_ACCESS");
        }
    }

    public static void requireAdmin(Request req, Response res) {
        logger.info("Starting admin authorization check for path: {}", req.pathInfo());
        if (req.raw().getMethod().equals("OPTIONS")) {
            logger.info("OPTIONS request - skipping authorization");
            return;
        }

        logger.info("Authenticating request and getting caller ID");
        String callerId = authenticateAndGetCallerId(req, res);
        logger.info("Authentication successful for user ID: {}", callerId);

        try {
            logger.info("Retrieving user {} details", callerId);
            User user = userProvider.getUser(callerId);

            if (user == null) {
                logger.warn("User {} not found", callerId);
                halt(403, "UNAUTHORIZED_ACCESS");
            }

            if (user.role() == null) {
                logger.warn("User {} has no role assigned", callerId);
                halt(403, "UNAUTHORIZED_ACCESS");
            }

            String roleId = user.role().id();
            logger.info("User {} has role ID: {}", callerId, roleId);

            Optional<Role> role = roleRepository.findRoleById(roleId);
            logger.info("Retrieved role: {}", (role.isPresent() ? role.get().name() : "NONE"));

            if (role.isEmpty() || !role.get().name().equals("ADMIN")) {
                logger.warn("User {} with role {} attempted to access admin-only endpoint: {}",
                        callerId, (role.map(Role::name).orElse("NONE")), req.pathInfo());
                halt(403, "UNAUTHORIZED_ACCESS");
            }

            logger.info("User {} confirmed as admin", callerId);
            req.attribute("callerId", callerId);

        } catch (Exception e) {
            logger.error("Error checking admin status for user {} {}", callerId, e.getMessage());
            logger.error("Stack trace:", e);
            halt(403, "UNAUTHORIZED_ACCESS");
        }
    }

    public static String authenticateAndGetCallerId(Request req, Response res) {
        logger.info("Starting authentication process");
        String token = req.headers("Authorization");
        logger.info("Authorization header present: {}", (token != null));

        if (token != null && token.startsWith("Bearer ")) {
            logger.info("Bearer token found, extracting token");
            token = token.substring(7);
        }

        if (token == null || token.isEmpty()) {
            logger.warn("No valid authorization token provided in request to {}", req.pathInfo());
            halt(403, "AUTH_REQUIRED");
        }

        try {
            logger.info("Parsing JWT token");
            Claims claims = Jwts.parser()
                    .setSigningKey(Config.getJwtSecret())
                    .parseClaimsJws(token)
                    .getBody();

            String callerId = claims.getSubject();
            User userFromDb = userProvider.getUser(callerId);

            logger.info("Token parsed successfully, subject: {}", callerId);

            if (userFromDb == null || userFromDb.id() == null || !userFromDb.id().equals(callerId)) {
                logger.warn("Token has no subject (user ID)");
                res.removeCookie(cookieName);
                halt(403, "INVALID_TOKEN");
            }

            req.attribute("callerId", callerId);

            return callerId;
        } catch (ExpiredJwtException e) {
            logger.warn("Token expired for request to {}: {}", req.pathInfo(), e.getMessage());
            res.removeCookie(cookieName);
            halt(403, "EXPIRED_TOKEN");
        } catch (Exception e) {
            logger.error("Token validation error for request to {}: {}", req.pathInfo(), e.getMessage());
            logger.error("Token validation stack trace:", e);
            res.removeCookie(cookieName);
            halt(403, "INVALID_TOKEN");
        }

        return null; // Will never reach here due to halt() calls
    }

    public static String getCallerIdUsername(String callerId) {
        try {
            User userFromDb = userProvider.getUser(callerId);

            logger.info("Token parsed successfully, subject: {}", callerId);

            if (userFromDb == null || userFromDb.id() == null || !userFromDb.id().equals(callerId)) {
                logger.warn("Token has no subject (user ID)");
                halt(403, "INVALID_TOKEN");
            }

            return userFromDb.username();
        } catch (ExpiredJwtException e) {
            logger.warn("Token expired for request to: {}", e.getMessage());
            halt(403, "EXPIRED_TOKEN");
        } catch (Exception e) {
            logger.error("Token validation error for request to: {}", e.getMessage());
            logger.error("Token validation stack trace:", e);
            halt(403, "INVALID_TOKEN");
        }

        return null;
    }

    private static String getSuperAdminId() throws SQLException {
        logger.info("Looking up SUPERADMIN role ID");
        Optional<Role> role = roleRepository.findRoleByName("SUPERADMIN");
        if (role.isEmpty()) {
            logger.error("SUPERADMIN role not found in database");
            throw new IllegalStateException("SUPERADMIN role not found in database");
        }
        logger.info("Found SUPERADMIN role with ID: {}", role.get().id());
        return role.get().id();
    }

    public static boolean isSuperAdmin(String roleId) {
        try {
            logger.info("Checking if role ID {} is SUPERADMIN", roleId);
            String superAdminId = getSuperAdminId();
            boolean result = superAdminId.equals(roleId);
            logger.info("Role {} is {} SUPERADMIN", roleId, (result ? "" : "not "));
            return result;
        } catch (NumberFormatException e) {
            logger.warn("Invalid role ID format: {}", roleId);
            return false;
        } catch (SQLException e) {
            logger.error("Database error checking SUPERADMIN role: {}", e.getMessage());
            logger.error("Stack trace:", e);
            throw new RuntimeException(e);
        }
    }

    private static String getAdminId() throws SQLException {
        logger.info("Looking up ADMIN role ID");
        Optional<Role> role = roleRepository.findRoleByName("ADMIN");
        if (role.isEmpty()) {
            logger.error("ADMIN role not found in database");
            throw new IllegalStateException("ADMIN role not found in database");
        }
        logger.info("Found ADMIN role with ID: {}", role.get().id());
        return role.get().id();
    }

    public static boolean isAdmin(String roleId) {
        try {
            logger.info("Checking if role ID {} is ADMIN", roleId);

            String adminId = getAdminId();
            boolean result = adminId.equals(roleId);
            logger.info("Role {} is {} ADMIN", roleId, (result ? "" : "not "));
            return result;
        } catch (NumberFormatException e) {
            logger.warn("Invalid role ID format: {}", roleId);
            return false;
        } catch (SQLException e) {
            logger.error("Database error checking ADMIN role: {}", e.getMessage());
            logger.error("Stack trace:", e);
            throw new RuntimeException(e);
        }
    }

    public static boolean isUserSuperAdmin(String userId) throws Exception {
        logger.info("Checking if user {} is a SUPERADMIN", userId);
        try {
            logger.info("Retrieving user details for {}", userId);
            User user = userProvider.getUser(userId);

            if (user == null) {
                logger.warn("User {} not found", userId);
                return false;
            }

            logger.info("User retrieved: {}", user.username());

            if (user.role() == null) {
                logger.warn("User {} ({}) has no role assigned", userId, user.username());
                return false;
            }

            logger.info("User {} has role: {}  (ID: {})", userId, user.role().name(), user.role().id());
            boolean result = isSuperAdmin(user.role().id());
            logger.info("User {} ({}) is {} a SUPERADMIN", userId, user.username(), (result ? "" : "not "));
            return result;

        } catch (AuthenticationCustomException e) {
            logger.warn("Failed to check superadmin status for user {}: {}", userId, e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("Error checking superadmin status for user {}: {}", userId, e.getMessage());
            logger.error("Stack trace:", e);
            throw new AuthenticationCustomException("Failed to check superadmin status", e);
        }
    }

    public static boolean isUserAdmin(String userId) throws Exception {
        logger.info("Checking if user {} is an ADMIN", userId);
        try {
            logger.info("Retrieving user details for {}", userId);
            User user = userProvider.getUser(userId);

            if (user == null) {
                logger.warn("User {}", userId + " not found");
                return false;
            }

            logger.info("User retrieved: {}", user.username());

            if (user.role() == null) {
                logger.warn("User {} ({}) has no role assigned", userId, user.username());
                return false;
            }

            logger.info("User {} has role: {} (ID: {})", userId, user.role().name(), user.role().id());

            boolean result = isAdmin(user.role().id());
            logger.info("User {}  ({}) is {} an ADMIN", userId, user.username(), result ? "" : "not ");
            return result;

        } catch (AuthenticationCustomException e) {
            logger.warn("Failed to check admin status for user {}: {}", userId, e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("Error checking admin status for user {}: {}", userId, e.getMessage());
            logger.error("Stack trace:", e);
            throw new AuthenticationCustomException("Failed to check admin status", e);
        }
    }
}