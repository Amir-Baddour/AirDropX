package org.example.Api.Public;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.example.Api.Claim.ClaimApi;
import org.example.Core.Airdrop.Model.Airdrop;
import org.example.Core.Claim.Creator.ClaimCreator;
import org.example.Core.Claim.Model.ClaimSubmission;
import org.example.Core.Claim.Model.PublicClaimStatus;
import org.example.Core.Claim.Provider.ClaimProvider;
import org.example.Core.Task.Model.AirdropTask;
import org.example.Core.Task.Provider.TaskProvider;
import org.example.Middleware.RateLimiter;

import static org.example.Api.Common.ApiJson.bodyObject;
import static org.example.Api.Common.ApiJson.ok;
import static org.example.Api.Common.ApiJson.optString;
import static org.example.Api.Common.CompanyScope.open;
import static org.example.Api.Common.CompanyScope.uuidParam;
import static spark.Spark.before;
import static spark.Spark.get;
import static spark.Spark.post;

/** Public claim endpoints (no login). Rate limited per client IP. */
public class PublicClaimApi {
    private final ClaimCreator claimCreator;
    private final ClaimProvider claimProvider;
    private final TaskProvider taskProvider;
    private final RateLimiter readLimiter = new RateLimiter(60, 60_000);
    private final RateLimiter submitLimiter = new RateLimiter(5, 60_000);

    public PublicClaimApi() {
        this.claimCreator = new ClaimCreator();
        this.claimProvider = new ClaimProvider();
        this.taskProvider = new TaskProvider();
    }

    public void initializeRoutes() {
        before("/public/*", (req, res) -> {
            if ("POST".equalsIgnoreCase(req.requestMethod())) {
                submitLimiter.enforce(req, res, "submit");
            } else if (!"OPTIONS".equalsIgnoreCase(req.requestMethod())) {
                readLimiter.enforce(req, res, "read");
            }
        });

        get("/public/airdrops/:id", (req, res) -> open(req, res, (rq, rs) -> {
            String id = uuidParam(rq, ":id", "airdrop");
            Airdrop airdrop = claimProvider.getOpenAirdrop(id);
            JsonObject data = new JsonObject();
            data.addProperty("id", airdrop.id());
            data.addProperty("name", airdrop.name());
            data.addProperty("description", airdrop.description());
            data.addProperty("token_symbol", airdrop.tokenSymbol());
            data.addProperty("claim_amount", airdrop.claimAmount() == null ? null : airdrop.claimAmount().toPlainString());
            data.addProperty("max_claims", airdrop.maxClaims());
            JsonArray tasks = new JsonArray();
            for (AirdropTask task : claimProvider.getPublicTasks(id)) {
                JsonObject t = ClaimApi.toJson(task, false);
                t.add("config", taskProvider.publicConfig(task));
                tasks.add(t);
            }
            data.add("tasks", tasks);
            return ok("Airdrop retrieved", data);
        }));

        post("/public/airdrops/:id/claims", (req, res) -> open(req, res, (rq, rs) -> {
            JsonObject body = bodyObject(rq);
            JsonObject answers = body.has("answers") && body.get("answers").isJsonObject() ? body.getAsJsonObject("answers") : new JsonObject();
            ClaimSubmission submission = claimCreator.submit(uuidParam(rq, ":id", "airdrop"),
                    optString(body, "address"), answers, RateLimiter.clientIp(rq));
            JsonObject data = new JsonObject();
            data.addProperty("claim_id", submission.claim().id());
            data.addProperty("status", submission.claim().status().name());
            data.addProperty("claim_token", submission.claimToken());
            data.addProperty("status_path", "/public/claims/" + submission.claimToken());
            rs.status(201);
            return ok("Claim submitted. Save your claim token to check its status later.", data);
        }));

        get("/public/claims/:token", (req, res) -> open(req, res, (rq, rs) -> {
            PublicClaimStatus s = claimProvider.getByToken(rq.params(":token"));
            JsonObject data = new JsonObject();
            data.addProperty("airdrop_name", s.airdropName());
            data.addProperty("token_symbol", s.tokenSymbol());
            data.addProperty("address", s.address());
            data.addProperty("status", s.status().name());
            data.addProperty("reject_reason", s.rejectReason());
            data.addProperty("payout_status", s.payoutStatus());
            data.addProperty("tx_ref", s.txRef());
            return ok("Claim status", data);
        }));
    }
}
