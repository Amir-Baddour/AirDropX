package org.example.Api.Common;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import spark.Request;
import spark.Response;

/**
 * Shared helpers for the {success, message, data} response shape used by all APIs.
 */
public final class ApiJson {
    private ApiJson() {
    }

    public static JsonObject ok(String message, JsonElement data) {
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("message", message);
        response.add("data", data);
        return response;
    }

    public static JsonObject error(Response res, int status, String message) {
        res.status(status);
        JsonObject response = new JsonObject();
        response.addProperty("success", false);
        response.addProperty("message", message);
        response.add("data", null);
        return response;
    }

    /** Parses the request body as a JSON object, or throws IllegalArgumentException (mapped to 400). */
    public static JsonObject bodyObject(Request req) {
        try {
            JsonElement element = JsonParser.parseString(req.body() == null ? "" : req.body());
            if (element == null || !element.isJsonObject()) {
                throw new IllegalArgumentException("Request body must be a JSON object");
            }
            return element.getAsJsonObject();
        } catch (RuntimeException e) {
            if (e instanceof IllegalArgumentException) {
                throw e;
            }
            throw new IllegalArgumentException("Request body is not valid JSON");
        }
    }

    public static String optString(JsonObject body, String key) {
        if (!body.has(key) || body.get(key).isJsonNull()) {
            return null;
        }
        if (!body.get(key).isJsonPrimitive()) {
            throw new IllegalArgumentException("Field '" + key + "' must be a text or number value");
        }
        return body.get(key).getAsString();
    }

    public static int intParam(Request req, String name, int defaultValue) {
        String value = req.queryParams(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Query parameter '" + name + "' must be a number");
        }
    }
}
