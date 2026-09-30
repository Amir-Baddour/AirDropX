package org.example.Api.Company;

import com.google.gson.JsonObject;
import org.example.Core.Company.Creator.CompanyCreator;
import org.example.Core.Company.Exception.CompanyAlreadyExistsException;
import org.example.Core.Company.Exception.CompanyNotFoundException;
import org.example.Core.Company.Exception.CompanySuspendedException;
import org.example.Core.Company.Model.Company;
import org.example.Core.Company.Provider.CompanyProvider;
import org.example.Middleware.AuthorizationMiddleware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.HaltException;

import static org.example.Api.Common.ApiJson.bodyObject;
import static org.example.Api.Common.ApiJson.error;
import static org.example.Api.Common.ApiJson.ok;
import static org.example.Api.Common.ApiJson.optString;
import static spark.Spark.get;
import static spark.Spark.post;

public class CompanyApi {
    private static final Logger logger = LoggerFactory.getLogger(CompanyApi.class.getName());
    private final CompanyCreator companyCreator;
    private final CompanyProvider companyProvider;

    public CompanyApi() {
        this.companyCreator = new CompanyCreator();
        this.companyProvider = new CompanyProvider();
    }

    public void initializeRoutes() {
        // Create a company; the caller becomes its owner.
        post("/companies", (req, res) -> {
            res.type("application/json");
            try {
                String userId = AuthorizationMiddleware.authenticateAndGetCallerId(req, res);
                JsonObject body = bodyObject(req);
                Company company = companyCreator.createCompany(userId, optString(body, "name"));
                res.status(201);
                return ok("Company created", toJson(company));
            } catch (HaltException e) {
                throw e;
            } catch (IllegalArgumentException e) {
                return error(res, 400, e.getMessage());
            } catch (CompanyAlreadyExistsException e) {
                return error(res, 409, e.getMessage());
            } catch (Exception e) {
                logger.error("Error creating company: {}", e.getMessage(), e);
                return error(res, 500, "Internal server error");
            }
        });

        // The caller's company.
        get("/companies/me", (req, res) -> {
            res.type("application/json");
            try {
                String userId = AuthorizationMiddleware.authenticateAndGetCallerId(req, res);
                return ok("Company retrieved", toJson(companyProvider.getCompanyOfUser(userId)));
            } catch (HaltException e) {
                throw e;
            } catch (CompanySuspendedException e) {
                JsonObject response = error(res, 403, e.getMessage());
                response.addProperty("code", "COMPANY_SUSPENDED");
                response.addProperty("reason", e.getReason());
                return response;
            } catch (CompanyNotFoundException e) {
                return error(res, 404, e.getMessage());
            } catch (Exception e) {
                logger.error("Error fetching company: {}", e.getMessage(), e);
                return error(res, 500, "Internal server error");
            }
        });
    }

    static JsonObject toJson(Company company) {
        JsonObject json = new JsonObject();
        json.addProperty("id", company.id());
        json.addProperty("name", company.name());
        json.addProperty("owner_id", company.ownerId());
        json.addProperty("status", company.status());
        json.addProperty("created_at", company.createdAt());
        return json;
    }
}
