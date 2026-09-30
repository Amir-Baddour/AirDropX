package org.example.Api.Admin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.example.Core.Admin.Model.AdminAction;
import org.example.Core.Admin.Model.CompanySummary;
import org.example.Core.Admin.Model.PlatformEvent;
import org.example.Core.Admin.Model.PlatformStats;
import org.example.Core.Admin.Provider.AdminProvider;
import org.example.Core.Admin.Updater.AdminUpdater;
import org.example.Core.Company.Exception.CompanyNotFoundException;
import org.example.Core.Company.Exception.InvalidCompanyStateException;
import org.example.Middleware.AuthorizationMiddleware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.HaltException;
import spark.Request;
import spark.Response;

import java.util.Map;

import static org.example.Api.Common.ApiJson.bodyObject;
import static org.example.Api.Common.ApiJson.error;
import static org.example.Api.Common.ApiJson.intParam;
import static org.example.Api.Common.ApiJson.ok;
import static org.example.Api.Common.ApiJson.optString;
import static org.example.Api.Common.CompanyScope.uuidParam;
import static spark.Spark.get;
import static spark.Spark.post;

/**
 * Platform administration. Every route requires the SUPERADMIN role (the existing
 * AuthorizationMiddleware.requireSuperAdmin check), and every change is audited.
 */
public class AdminApi {
    private static final Logger logger = LoggerFactory.getLogger(AdminApi.class.getName());
    private final AdminProvider adminProvider;
    private final AdminUpdater adminUpdater;

    public AdminApi() {
        this.adminProvider = new AdminProvider();
        this.adminUpdater = new AdminUpdater();
    }

    @FunctionalInterface
    private interface AdminHandler {
        JsonObject handle(Request req, Response res, String adminId) throws Exception;
    }

    public void initializeRoutes() {
        // Lets the frontend know whether to show the admin area (403 otherwise).
        get("/admin/access", (req, res) -> admin(req, res, (rq, rs, adminId) -> {
            JsonObject data = new JsonObject();
            data.addProperty("admin", true);
            return ok("Admin access granted", data);
        }));

        get("/admin/stats", (req, res) -> admin(req, res, (rq, rs, adminId) ->
                ok("Platform stats", toJson(adminProvider.getStats()))));

        get("/admin/companies", (req, res) -> admin(req, res, (rq, rs, adminId) -> {
            JsonArray items = new JsonArray();
            for (CompanySummary c : adminProvider.listCompanies(rq.queryParams("q"), rq.queryParams("status"),
                    intParam(rq, "limit", 50), intParam(rq, "offset", 0))) {
                items.add(toJson(c));
            }
            return ok("Companies retrieved", items);
        }));

        post("/admin/companies/:id/suspend", (req, res) -> admin(req, res, (rq, rs, adminId) -> {
            adminUpdater.suspendCompany(adminId, uuidParam(rq, ":id", "company"), optString(bodyObject(rq), "reason"));
            return ok("Company suspended", null);
        }));

        post("/admin/companies/:id/restore", (req, res) -> admin(req, res, (rq, rs, adminId) -> {
            String note = rq.body() == null || rq.body().isBlank() ? null : optString(bodyObject(rq), "note");
            adminUpdater.restoreCompany(adminId, uuidParam(rq, ":id", "company"), note);
            return ok("Company restored", null);
        }));

        get("/admin/audit", (req, res) -> admin(req, res, (rq, rs, adminId) -> {
            JsonArray items = new JsonArray();
            for (AdminAction a : adminProvider.listAudit(intParam(rq, "limit", 50))) {
                JsonObject json = new JsonObject();
                json.addProperty("id", a.id());
                json.addProperty("admin_id", a.adminId());
                json.addProperty("admin_username", a.adminUsername());
                json.addProperty("action", a.action());
                json.addProperty("target_type", a.targetType());
                json.addProperty("target_id", a.targetId());
                json.addProperty("detail", a.detail());
                json.addProperty("created_at", a.createdAt());
                items.add(json);
            }
            return ok("Audit log retrieved", items);
        }));

        get("/admin/activity", (req, res) -> admin(req, res, (rq, rs, adminId) -> {
            JsonArray items = new JsonArray();
            for (PlatformEvent e : adminProvider.recentActivity(intParam(rq, "limit", 30))) {
                JsonObject json = new JsonObject();
                json.addProperty("id", e.id());
                json.addProperty("company_name", e.companyName());
                json.addProperty("airdrop_id", e.airdropId());
                json.addProperty("airdrop_name", e.airdropName());
                json.addProperty("type", e.type());
                json.addProperty("message", e.message());
                json.addProperty("created_at", e.createdAt());
                items.add(json);
            }
            return ok("Activity retrieved", items);
        }));
    }

    private JsonObject admin(Request req, Response res, AdminHandler handler) {
        res.type("application/json");
        try {
            AuthorizationMiddleware.requireSuperAdmin(req, res);
            String adminId = req.attribute("callerId");
            return handler.handle(req, res, adminId);
        } catch (HaltException e) {
            throw e;
        } catch (CompanyNotFoundException e) {
            return error(res, 404, e.getMessage());
        } catch (InvalidCompanyStateException e) {
            return error(res, 409, e.getMessage());
        } catch (IllegalArgumentException e) {
            return error(res, 400, e.getMessage());
        } catch (Exception e) {
            logger.error("Admin API error on {} {}: {}", req.requestMethod(), req.pathInfo(), e.getMessage(), e);
            return error(res, 500, "Internal server error");
        }
    }

    static JsonObject toJson(PlatformStats s) {
        JsonObject json = new JsonObject();
        json.addProperty("users", s.users());
        json.add("companies", counts(s.companies()));
        json.add("airdrops", counts(s.airdrops()));
        json.add("claims", counts(s.claims()));
        json.add("recipients", counts(s.recipients()));
        json.addProperty("claims_last_24h", s.claimsLast24h());
        return json;
    }

    static JsonObject toJson(CompanySummary c) {
        JsonObject json = new JsonObject();
        json.addProperty("id", c.id());
        json.addProperty("name", c.name());
        json.addProperty("status", c.status());
        json.addProperty("owner_username", c.ownerUsername());
        json.addProperty("members", c.members());
        json.addProperty("airdrops", c.airdrops());
        json.addProperty("claims", c.claims());
        json.addProperty("suspended_reason", c.suspendedReason());
        json.addProperty("suspended_at", c.suspendedAt());
        json.addProperty("created_at", c.createdAt());
        return json;
    }

    private static JsonObject counts(Map<String, Long> map) {
        JsonObject json = new JsonObject();
        map.forEach(json::addProperty);
        return json;
    }
}
