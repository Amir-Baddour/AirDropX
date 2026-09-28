package org.example.Api.Airdrop;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.example.Core.Airdrop.Creator.AirdropCreator;
import org.example.Core.Airdrop.Exception.AirdropNotFoundException;
import org.example.Core.Airdrop.Exception.AirdropValidationException;
import org.example.Core.Airdrop.Exception.InvalidAirdropStateException;
import org.example.Core.Airdrop.Model.Airdrop;
import org.example.Core.Airdrop.Model.AirdropEvent;
import org.example.Core.Airdrop.Model.AirdropStats;
import org.example.Core.Airdrop.Model.Recipient;
import org.example.Core.Airdrop.Model.RecipientInput;
import org.example.Core.Airdrop.Provider.AirdropProvider;
import org.example.Core.Airdrop.Updater.AirdropUpdater;
import org.example.Core.Airdrop.Validator.RecipientValidator;
import org.example.Core.Company.Exception.CompanyNotFoundException;
import org.example.Core.Company.Provider.CompanyProvider;
import org.example.Middleware.AuthorizationMiddleware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.HaltException;
import spark.Request;
import spark.Response;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.example.Api.Common.ApiJson.bodyObject;
import static org.example.Api.Common.ApiJson.error;
import static org.example.Api.Common.ApiJson.intParam;
import static org.example.Api.Common.ApiJson.ok;
import static org.example.Api.Common.ApiJson.optString;
import static spark.Spark.get;
import static spark.Spark.post;

public class AirdropApi {
    private static final Logger logger = LoggerFactory.getLogger(AirdropApi.class.getName());
    private final CompanyProvider companyProvider;
    private final AirdropCreator airdropCreator;
    private final AirdropProvider airdropProvider;
    private final AirdropUpdater airdropUpdater;

    public AirdropApi() {
        this.companyProvider = new CompanyProvider();
        this.airdropCreator = new AirdropCreator();
        this.airdropProvider = new AirdropProvider();
        this.airdropUpdater = new AirdropUpdater();
    }

    /** Handler that runs with the authenticated user and their company already resolved. */
    @FunctionalInterface
    private interface CompanyScopedHandler {
        JsonObject handle(Request req, Response res, String userId, String companyId) throws Exception;
    }

    public void initializeRoutes() {
        post("/airdrops", (req, res) -> handle(req, res, (rq, rs, userId, companyId) -> {
            JsonObject body = bodyObject(rq);
            Airdrop airdrop = airdropCreator.createAirdrop(companyId, userId,
                    optString(body, "name"), optString(body, "description"), optString(body, "token_symbol"));
            rs.status(201);
            return ok("Airdrop created", toJson(airdrop));
        }));

        get("/airdrops", (req, res) -> handle(req, res, (rq, rs, userId, companyId) -> {
            JsonArray items = new JsonArray();
            for (Airdrop airdrop : airdropProvider.listAirdrops(companyId, intParam(rq, "limit", 20), intParam(rq, "offset", 0))) {
                items.add(toJson(airdrop));
            }
            return ok("Airdrops retrieved", items);
        }));

        get("/airdrops/:id", (req, res) -> handle(req, res, (rq, rs, userId, companyId) -> {
            String id = airdropId(rq);
            JsonObject data = toJson(airdropProvider.getAirdrop(companyId, id));
            data.add("stats", toJson(airdropProvider.getStats(companyId, id)));
            return ok("Airdrop retrieved", data);
        }));

        post("/airdrops/:id/recipients", (req, res) -> handle(req, res, (rq, rs, userId, companyId) -> {
            List<RecipientInput> recipients = parseRecipients(bodyObject(rq));
            int added = airdropCreator.addRecipients(companyId, airdropId(rq), recipients);
            JsonObject data = new JsonObject();
            data.addProperty("received", recipients.size());
            data.addProperty("added", added);
            data.addProperty("skipped_existing", recipients.size() - added);
            return ok("Recipients added", data);
        }));

        get("/airdrops/:id/recipients", (req, res) -> handle(req, res, (rq, rs, userId, companyId) -> {
            JsonArray items = new JsonArray();
            for (Recipient r : airdropProvider.listRecipients(companyId, airdropId(rq), rq.queryParams("status"),
                    intParam(rq, "limit", 50), intParam(rq, "offset", 0))) {
                items.add(toJson(r));
            }
            return ok("Recipients retrieved", items);
        }));

        get("/airdrops/:id/events", (req, res) -> handle(req, res, (rq, rs, userId, companyId) -> {
            JsonArray items = new JsonArray();
            for (AirdropEvent e : airdropProvider.listEvents(companyId, airdropId(rq))) {
                JsonObject json = new JsonObject();
                json.addProperty("id", e.id());
                json.addProperty("type", e.type());
                json.addProperty("message", e.message());
                json.addProperty("created_at", e.createdAt());
                items.add(json);
            }
            return ok("Events retrieved", items);
        }));

        post("/airdrops/:id/validate", (req, res) -> handle(req, res, (rq, rs, userId, companyId) ->
                ok("Airdrop validated", toJson(airdropUpdater.validate(companyId, airdropId(rq))))));

        post("/airdrops/:id/reopen", (req, res) -> handle(req, res, (rq, rs, userId, companyId) ->
                ok("Airdrop moved back to draft", toJson(airdropUpdater.reopen(companyId, airdropId(rq))))));

        post("/airdrops/:id/launch", (req, res) -> handle(req, res, (rq, rs, userId, companyId) ->
                ok("Airdrop launched", toJson(airdropUpdater.launch(companyId, airdropId(rq))))));

        post("/airdrops/:id/cancel", (req, res) -> handle(req, res, (rq, rs, userId, companyId) ->
                ok("Airdrop cancelled", toJson(airdropUpdater.cancel(companyId, airdropId(rq))))));
    }

    /** Authenticates, resolves the caller's company, runs the handler and maps errors to HTTP codes. */
    private JsonObject handle(Request req, Response res, CompanyScopedHandler handler) {
        res.type("application/json");
        try {
            String userId = AuthorizationMiddleware.authenticateAndGetCallerId(req, res);
            String companyId = companyProvider.getCompanyOfUser(userId).id();
            return handler.handle(req, res, userId, companyId);
        } catch (HaltException e) {
            throw e;
        } catch (CompanyNotFoundException e) {
            return error(res, 403, e.getMessage());
        } catch (AirdropNotFoundException e) {
            return error(res, 404, e.getMessage());
        } catch (InvalidAirdropStateException e) {
            return error(res, 409, e.getMessage());
        } catch (AirdropValidationException e) {
            JsonObject response = error(res, 422, e.getMessage());
            JsonArray errors = new JsonArray();
            e.getErrors().forEach(errors::add);
            response.add("errors", errors);
            return response;
        } catch (IllegalArgumentException e) {
            return error(res, 400, e.getMessage());
        } catch (Exception e) {
            logger.error("Airdrop API error on {} {}: {}", req.requestMethod(), req.pathInfo(), e.getMessage(), e);
            return error(res, 500, "Internal server error");
        }
    }

    private static String airdropId(Request req) {
        String id = req.params(":id");
        try {
            return UUID.fromString(id).toString();
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Invalid airdrop id");
        }
    }

    /** Expects {"recipients": [{"address": "0x...", "amount": "10.5"}, ...]}. */
    static List<RecipientInput> parseRecipients(JsonObject body) {
        if (!body.has("recipients") || !body.get("recipients").isJsonArray()) {
            throw new IllegalArgumentException("Body must contain a 'recipients' array");
        }
        JsonArray array = body.getAsJsonArray("recipients");
        if (array.size() > RecipientValidator.MAX_RECIPIENTS_PER_REQUEST) {
            throw new IllegalArgumentException("Too many recipients in one request (max " + RecipientValidator.MAX_RECIPIENTS_PER_REQUEST + ")");
        }
        List<RecipientInput> result = new ArrayList<>(array.size());
        for (int i = 0; i < array.size(); i++) {
            JsonElement element = array.get(i);
            if (!element.isJsonObject()) {
                throw new IllegalArgumentException("Row " + (i + 1) + ": must be an object with address and amount");
            }
            JsonObject row = element.getAsJsonObject();
            String address = optString(row, "address");
            String amountText = optString(row, "amount");
            BigDecimal amount = null;
            if (amountText != null) {
                try {
                    amount = new BigDecimal(amountText.trim());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Row " + (i + 1) + ": amount '" + amountText + "' is not a number");
                }
            }
            result.add(new RecipientInput(address, amount));
        }
        return result;
    }

    static JsonObject toJson(Airdrop airdrop) {
        JsonObject json = new JsonObject();
        json.addProperty("id", airdrop.id());
        json.addProperty("company_id", airdrop.companyId());
        json.addProperty("name", airdrop.name());
        json.addProperty("description", airdrop.description());
        json.addProperty("token_symbol", airdrop.tokenSymbol());
        json.addProperty("status", airdrop.status().name());
        json.addProperty("created_by", airdrop.createdBy());
        json.addProperty("created_at", airdrop.createdAt());
        json.addProperty("updated_at", airdrop.updatedAt());
        json.addProperty("launched_at", airdrop.launchedAt());
        json.addProperty("completed_at", airdrop.completedAt());
        return json;
    }

    static JsonObject toJson(AirdropStats stats) {
        JsonObject json = new JsonObject();
        json.addProperty("total_recipients", stats.totalRecipients());
        json.addProperty("total_amount", stats.totalAmount().toPlainString());
        JsonObject byStatus = new JsonObject();
        for (Map.Entry<String, Long> entry : stats.byStatus().entrySet()) {
            byStatus.addProperty(entry.getKey(), entry.getValue());
        }
        json.add("by_status", byStatus);
        return json;
    }

    static JsonObject toJson(Recipient recipient) {
        JsonObject json = new JsonObject();
        json.addProperty("id", recipient.id());
        json.addProperty("address", recipient.address());
        json.addProperty("amount", recipient.amount().toPlainString());
        json.addProperty("status", recipient.status().name());
        json.addProperty("tx_ref", recipient.txRef());
        json.addProperty("error", recipient.error());
        json.addProperty("updated_at", recipient.updatedAt());
        return json;
    }
}
