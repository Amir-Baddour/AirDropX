package org.example.Api.User;

import com.google.gson.JsonObject;
import org.example.Core.User.Exception.EmailAlreadyRegisteredException;
import org.example.Core.User.Exception.InvalidCredentialsException;
import org.example.Core.User.Model.UserProfile;
import org.example.Core.User.Service.AccountService;
import org.example.Middleware.AuthorizationMiddleware;
import org.example.Middleware.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.HaltException;

import static org.example.Api.Common.ApiJson.bodyObject;
import static org.example.Api.Common.ApiJson.error;
import static org.example.Api.Common.ApiJson.ok;
import static org.example.Api.Common.ApiJson.optString;
import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.put;

/** Email/password registration and login, and profile management (works for Google accounts too). */
public class AccountApi {
    private static final Logger logger = LoggerFactory.getLogger(AccountApi.class.getName());
    private final AccountService accounts = new AccountService();
    // Per client IP, and per email address so one account cannot be brute-forced from many IPs.
    private final RateLimiter registerLimiter = new RateLimiter(5, 60_000);
    private final RateLimiter loginLimiter = new RateLimiter(10, 60_000);
    private final RateLimiter loginPerEmailLimiter = new RateLimiter(10, 15 * 60_000);
    private final RateLimiter passwordLimiter = new RateLimiter(5, 15 * 60_000);

    public void initializeRoutes() {
        post("/auth/register", (req, res) -> {
            res.type("application/json");
            try {
                registerLimiter.enforce(req, res, "register");
                JsonObject body = bodyObject(req);
                accounts.register(optString(body, "email"), optString(body, "password"),
                        optString(body, "firstName"), optString(body, "lastName"));
                res.status(201);
                return ok("Account created. Please sign in.", new JsonObject());
            } catch (HaltException e) {
                throw e;
            } catch (IllegalArgumentException e) {
                return error(res, 400, e.getMessage());
            } catch (EmailAlreadyRegisteredException e) {
                return error(res, 409, e.getMessage());
            } catch (Exception e) {
                logger.error("Error registering account: {}", e.getMessage(), e);
                return error(res, 500, "Internal server error");
            }
        });

        post("/auth/login", (req, res) -> {
            res.type("application/json");
            try {
                loginLimiter.enforce(req, res, "login");
                JsonObject body = bodyObject(req);
                String email = optString(body, "email");
                String password = optString(body, "password");
                if (email == null || email.isBlank() || password == null || password.isEmpty()) {
                    return error(res, 400, "Email and password are required");
                }
                if (!loginPerEmailLimiter.tryAcquire(email.trim().toLowerCase(), System.currentTimeMillis())) {
                    return error(res, 429, "Too many sign-in attempts for this account. Try again in a few minutes.");
                }
                AccountService.LoginResult result = accounts.login(email, password);
                return ok("Authentication successful", loginJson(result));
            } catch (HaltException e) {
                throw e;
            } catch (IllegalArgumentException e) {
                return error(res, 400, e.getMessage());
            } catch (InvalidCredentialsException e) {
                return error(res, 401, e.getMessage());
            } catch (Exception e) {
                logger.error("Error during email login: {}", e.getMessage(), e);
                return error(res, 500, "Internal server error");
            }
        });

        get("/user/profile", (req, res) -> {
            res.type("application/json");
            try {
                String userId = AuthorizationMiddleware.authenticateAndGetCallerId(req, res);
                return ok("Profile retrieved", profileJson(accounts.getProfile(userId)));
            } catch (HaltException e) {
                throw e;
            } catch (IllegalArgumentException e) {
                return error(res, 404, e.getMessage());
            } catch (Exception e) {
                logger.error("Error fetching profile: {}", e.getMessage(), e);
                return error(res, 500, "Internal server error");
            }
        });

        put("/user/profile", (req, res) -> {
            res.type("application/json");
            try {
                String userId = AuthorizationMiddleware.authenticateAndGetCallerId(req, res);
                JsonObject body = bodyObject(req);
                UserProfile updated = accounts.updateProfile(userId, optString(body, "firstName"),
                        optString(body, "lastName"), optString(body, "phone"), optString(body, "address"));
                return ok("Profile updated", profileJson(updated));
            } catch (HaltException e) {
                throw e;
            } catch (IllegalArgumentException e) {
                return error(res, 400, e.getMessage());
            } catch (Exception e) {
                logger.error("Error updating profile: {}", e.getMessage(), e);
                return error(res, 500, "Internal server error");
            }
        });

        put("/user/password", (req, res) -> {
            res.type("application/json");
            try {
                String userId = AuthorizationMiddleware.authenticateAndGetCallerId(req, res);
                passwordLimiter.enforce(req, res, "password");
                JsonObject body = bodyObject(req);
                accounts.changePassword(userId, optString(body, "currentPassword"), optString(body, "newPassword"));
                return ok("Password changed", new JsonObject());
            } catch (HaltException e) {
                throw e;
            } catch (IllegalArgumentException e) {
                return error(res, 400, e.getMessage());
            } catch (InvalidCredentialsException e) {
                return error(res, 403, e.getMessage()); // 403, not 401: the session itself is still valid
            } catch (Exception e) {
                logger.error("Error changing password: {}", e.getMessage(), e);
                return error(res, 500, "Internal server error");
            }
        });
    }

    static JsonObject profileJson(UserProfile p) {
        JsonObject json = new JsonObject();
        json.addProperty("id", p.id());
        json.addProperty("username", p.username());
        json.addProperty("provider", p.provider());
        json.addProperty("email", p.email());
        json.addProperty("first_name", p.firstName());
        json.addProperty("last_name", p.lastName());
        json.addProperty("phone", p.phone());
        json.addProperty("address", p.address());
        json.addProperty("pfp", p.pfp());
        json.addProperty("role", p.role());
        json.addProperty("has_password", p.hasPassword());
        json.addProperty("display_name", p.displayName());
        json.addProperty("created_at", p.createdAt());
        return json;
    }

    /** Same shape as the Google login response, so the frontend stores both the same way. */
    static JsonObject loginJson(AccountService.LoginResult result) {
        UserProfile p = result.profile();
        JsonObject json = new JsonObject();
        json.addProperty("id", p.id());
        json.addProperty("username", p.username());
        json.addProperty("provider", p.provider());
        json.addProperty("pfp", p.pfp());
        json.addProperty("display_name", p.displayName());
        JsonObject role = new JsonObject();
        role.addProperty("name", p.role());
        json.add("role", role);
        JsonObject token = new JsonObject();
        token.addProperty("access_token", result.accessToken());
        token.addProperty("expires_at", result.expiresAtMillis());
        json.add("token", token);
        return json;
    }
}
