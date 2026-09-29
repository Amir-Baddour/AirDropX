package org.example.Api.Common;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.example.Core.Airdrop.Exception.AirdropNotFoundException;
import org.example.Core.Airdrop.Exception.AirdropValidationException;
import org.example.Core.Airdrop.Exception.InvalidAirdropStateException;
import org.example.Core.Claim.Exception.ClaimConflictException;
import org.example.Core.Claim.Exception.ClaimNotFoundException;
import org.example.Core.Claim.Exception.ClaimRejectedException;
import org.example.Core.Claim.Exception.TooManyClaimsException;
import org.example.Core.Company.Exception.CompanyNotFoundException;
import org.example.Core.Company.Provider.CompanyProvider;
import org.example.Middleware.AuthorizationMiddleware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.HaltException;
import spark.Request;
import spark.Response;

import java.util.List;
import java.util.UUID;

import static org.example.Api.Common.ApiJson.error;

/** Shared request handling for the claim APIs: auth, company lookup and error-to-HTTP mapping. */
public final class CompanyScope {
    private static final Logger logger = LoggerFactory.getLogger(CompanyScope.class.getName());
    private static final CompanyProvider companyProvider = new CompanyProvider();

    private CompanyScope() {
    }

    @FunctionalInterface
    public interface CompanyHandler {
        JsonObject handle(Request req, Response res, String userId, String companyId) throws Exception;
    }

    @FunctionalInterface
    public interface PublicHandler {
        JsonObject handle(Request req, Response res) throws Exception;
    }

    /** For company endpoints: requires a valid JWT and a company membership. */
    public static JsonObject company(Request req, Response res, CompanyHandler handler) {
        return run(req, res, () -> {
            String userId = AuthorizationMiddleware.authenticateAndGetCallerId(req, res);
            String companyId = companyProvider.getCompanyOfUser(userId).id();
            return handler.handle(req, res, userId, companyId);
        });
    }

    /** For public endpoints: no login. */
    public static JsonObject open(Request req, Response res, PublicHandler handler) {
        return run(req, res, () -> handler.handle(req, res));
    }

    private interface Body {
        JsonObject get() throws Exception;
    }

    private static JsonObject run(Request req, Response res, Body body) {
        res.type("application/json");
        try {
            return body.get();
        } catch (HaltException e) {
            throw e;
        } catch (CompanyNotFoundException e) {
            return error(res, 403, e.getMessage());
        } catch (AirdropNotFoundException | ClaimNotFoundException e) {
            return error(res, 404, e.getMessage());
        } catch (InvalidAirdropStateException | ClaimConflictException e) {
            return error(res, 409, e.getMessage());
        } catch (TooManyClaimsException e) {
            return error(res, 429, e.getMessage());
        } catch (AirdropValidationException e) {
            return withErrors(error(res, 422, e.getMessage()), e.getErrors());
        } catch (ClaimRejectedException e) {
            return withErrors(error(res, 422, e.getMessage()), e.getErrors());
        } catch (IllegalArgumentException e) {
            return error(res, 400, e.getMessage());
        } catch (Exception e) {
            logger.error("API error on {} {}: {}", req.requestMethod(), req.pathInfo(), e.getMessage(), e);
            return error(res, 500, "Internal server error");
        }
    }

    private static JsonObject withErrors(JsonObject response, List<String> errors) {
        JsonArray array = new JsonArray();
        errors.forEach(array::add);
        response.add("errors", array);
        return response;
    }

    /** Reads a UUID path parameter, or fails with 400. */
    public static String uuidParam(Request req, String name, String label) {
        String value = req.params(name);
        try {
            return UUID.fromString(value).toString();
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Invalid " + label + " id");
        }
    }
}
