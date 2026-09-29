package org.example.Api.Claim;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.example.Core.Airdrop.Model.Airdrop;
import org.example.Core.Claim.Model.Claim;
import org.example.Core.Claim.Provider.ClaimProvider;
import org.example.Core.Claim.Updater.ClaimUpdater;
import org.example.Core.Task.Creator.TaskCreator;
import org.example.Core.Task.Model.AirdropTask;
import org.example.Core.Task.Model.TaskResult;
import org.example.Core.Task.Provider.TaskProvider;

import java.math.BigDecimal;
import java.util.Map;

import static org.example.Api.Common.ApiJson.bodyObject;
import static org.example.Api.Common.ApiJson.intParam;
import static org.example.Api.Common.ApiJson.ok;
import static org.example.Api.Common.ApiJson.optString;
import static org.example.Api.Common.CompanyScope.company;
import static org.example.Api.Common.CompanyScope.uuidParam;
import static spark.Spark.delete;
import static spark.Spark.get;
import static spark.Spark.post;

/** Company side of claims: tasks, opening/closing the claim page, and the review queue. */
public class ClaimApi {
    private final TaskCreator taskCreator;
    private final TaskProvider taskProvider;
    private final ClaimProvider claimProvider;
    private final ClaimUpdater claimUpdater;

    public ClaimApi() {
        this.taskCreator = new TaskCreator();
        this.taskProvider = new TaskProvider();
        this.claimProvider = new ClaimProvider();
        this.claimUpdater = new ClaimUpdater();
    }

    public void initializeRoutes() {
        post("/airdrops/:id/tasks", (req, res) -> company(req, res, (rq, rs, userId, companyId) -> {
            JsonObject body = bodyObject(rq);
            JsonObject config = body.has("config") && body.get("config").isJsonObject() ? body.getAsJsonObject("config") : null;
            AirdropTask task = taskCreator.createTask(companyId, airdropId(rq), optString(body, "type"),
                    optString(body, "title"), optString(body, "description"), config);
            rs.status(201);
            return ok("Task created", toJson(task, true));
        }));

        get("/airdrops/:id/tasks", (req, res) -> company(req, res, (rq, rs, userId, companyId) -> {
            JsonArray items = new JsonArray();
            for (AirdropTask task : taskProvider.listTasks(companyId, airdropId(rq))) {
                items.add(toJson(task, true));
            }
            return ok("Tasks retrieved", items);
        }));

        delete("/airdrops/:id/tasks/:taskId", (req, res) -> company(req, res, (rq, rs, userId, companyId) -> {
            taskCreator.deleteTask(companyId, airdropId(rq), uuidParam(rq, ":taskId", "task"));
            return ok("Task deleted", null);
        }));

        post("/airdrops/:id/claims/open", (req, res) -> company(req, res, (rq, rs, userId, companyId) -> {
            JsonObject body = bodyObject(rq);
            Airdrop airdrop = claimUpdater.openClaims(companyId, airdropId(rq),
                    decimal(optString(body, "claim_amount"), "claim_amount"), integer(optString(body, "max_claims"), "max_claims"));
            return ok("Claims opened", claimSettings(airdrop));
        }));

        post("/airdrops/:id/claims/close", (req, res) -> company(req, res, (rq, rs, userId, companyId) ->
                ok("Claims closed", claimSettings(claimUpdater.closeClaims(companyId, airdropId(rq))))));

        get("/airdrops/:id/claims", (req, res) -> company(req, res, (rq, rs, userId, companyId) -> {
            String id = airdropId(rq);
            JsonArray items = new JsonArray();
            for (Claim claim : claimProvider.listClaims(companyId, id, rq.queryParams("status"),
                    intParam(rq, "limit", 50), intParam(rq, "offset", 0))) {
                items.add(toJson(claim));
            }
            JsonObject counts = new JsonObject();
            for (Map.Entry<String, Long> e : claimProvider.countsByStatus(companyId, id).entrySet()) {
                counts.addProperty(e.getKey(), e.getValue());
            }
            JsonObject data = new JsonObject();
            data.add("counts", counts);
            data.add("items", items);
            return ok("Claims retrieved", data);
        }));

        post("/airdrops/:id/claims/:claimId/approve", (req, res) -> company(req, res, (rq, rs, userId, companyId) ->
                ok("Claim approved", toJson(claimUpdater.approve(companyId, userId, airdropId(rq), claimId(rq))))));

        post("/airdrops/:id/claims/:claimId/reject", (req, res) -> company(req, res, (rq, rs, userId, companyId) ->
                ok("Claim rejected", toJson(claimUpdater.reject(companyId, userId, airdropId(rq), claimId(rq),
                        optString(bodyObject(rq), "reason"))))));
    }

    private static String airdropId(spark.Request req) {
        return uuidParam(req, ":id", "airdrop");
    }

    private static String claimId(spark.Request req) {
        return uuidParam(req, ":claimId", "claim");
    }

    static BigDecimal decimal(String text, String field) {
        if (text == null) {
            return null;
        }
        try {
            return new BigDecimal(text.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(field + " must be a number");
        }
    }

    static Integer integer(String text, String field) {
        if (text == null) {
            return null;
        }
        try {
            return Integer.valueOf(text.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(field + " must be a whole number");
        }
    }

    static JsonObject claimSettings(Airdrop airdrop) {
        JsonObject json = new JsonObject();
        json.addProperty("airdrop_id", airdrop.id());
        json.addProperty("claims_open", airdrop.claimsOpen());
        json.addProperty("claim_amount", airdrop.claimAmount() == null ? null : airdrop.claimAmount().toPlainString());
        json.addProperty("max_claims", airdrop.maxClaims());
        json.addProperty("public_path", "/public/airdrops/" + airdrop.id());
        return json;
    }

    public static JsonObject toJson(AirdropTask task, boolean withConfig) {
        JsonObject json = new JsonObject();
        json.addProperty("id", task.id());
        json.addProperty("type", task.type().name());
        json.addProperty("title", task.title());
        json.addProperty("description", task.description());
        json.addProperty("position", task.position());
        json.addProperty("auto_verified", task.type().isAutoVerified());
        if (withConfig) {
            json.add("config", task.config());
        }
        return json;
    }

    static JsonObject toJson(Claim claim) {
        JsonObject json = new JsonObject();
        json.addProperty("id", claim.id());
        json.addProperty("address", claim.address());
        json.addProperty("status", claim.status().name());
        json.addProperty("reject_reason", claim.rejectReason());
        json.addProperty("created_at", claim.createdAt());
        json.addProperty("reviewed_at", claim.reviewedAt());
        JsonArray results = new JsonArray();
        for (TaskResult r : claim.results()) {
            JsonObject jr = new JsonObject();
            jr.addProperty("task_id", r.taskId());
            jr.addProperty("passed", r.passed());
            jr.addProperty("needs_review", r.needsReview());
            jr.addProperty("proof", r.proof());
            jr.addProperty("detail", r.detail());
            results.add(jr);
        }
        json.add("results", results);
        return json;
    }
}
