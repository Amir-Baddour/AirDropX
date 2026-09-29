package org.example.Middleware;

import com.google.gson.JsonObject;
import spark.Request;
import spark.Response;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static spark.Spark.halt;

/**
 * In-memory sliding-window rate limiter, keyed by client IP and a bucket name.
 * Good for a single API instance; with several instances this would move to Redis or Postgres.
 */
public class RateLimiter {
    private final int maxRequests;
    private final long windowMillis;
    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    public RateLimiter(int maxRequests, long windowMillis) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowMillis;
    }

    /** Returns true if the request is allowed and records it. */
    public boolean tryAcquire(String key, long now) {
        Deque<Long> window = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (window) {
            while (!window.isEmpty() && window.peekFirst() <= now - windowMillis) {
                window.pollFirst();
            }
            if (window.size() >= maxRequests) {
                return false;
            }
            window.addLast(now);
            return true;
        }
    }

    /** Halts with 429 and a JSON body when the caller is over the limit. */
    public void enforce(Request req, Response res, String bucket) {
        long now = System.currentTimeMillis();
        if (hits.size() > 50_000) {
            cleanup(now);
        }
        if (!tryAcquire(bucket + "|" + clientIp(req), now)) {
            res.type("application/json");
            res.header("Retry-After", String.valueOf(Math.max(1, windowMillis / 1000)));
            JsonObject body = new JsonObject();
            body.addProperty("success", false);
            body.addProperty("message", "Too many requests, please slow down");
            halt(429, body.toString());
        }
    }

    /** Drops keys whose window is empty, so memory does not grow forever. */
    void cleanup(long now) {
        Iterator<Map.Entry<String, Deque<Long>>> it = hits.entrySet().iterator();
        while (it.hasNext()) {
            Deque<Long> window = it.next().getValue();
            synchronized (window) {
                if (window.isEmpty() || window.peekLast() <= now - windowMillis) {
                    it.remove();
                }
            }
        }
    }

    int trackedKeys() {
        return hits.size();
    }

    /** Caddy sets X-Forwarded-For to the real client IP; fall back to the socket address. */
    public static String clientIp(Request req) {
        String forwarded = req.headers("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return req.ip();
    }
}
