package com.ecom.app.perf;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

/**
 * Fires many concurrent "place order" requests at the same low-stock product to check
 * whether the backend's read-then-write stock decrement in OrderService loses updates
 * under concurrency (classic lost-update race: two requests both read stock=1, both see
 * "enough stock", both decrement and save, and one decrement is silently overwritten).
 *
 * Correctness, not just speed, is the point: after the burst finishes, the number of
 * requests that got HTTP 201 is compared against how much the product's stock actually
 * dropped by. If they don't match (or stock goes negative), that's the bug, surfaced as
 * a build failure rather than just a slow report.
 *
 * Run with: ./mvnw gatling:test
 * Override target/product/load with -DbaseUrl=... -DproductId=... -DconcurrentOrders=...
 */
public class ConcurrentOrderStockRaceSimulation extends Simulation {

    private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");
    // Seeded "27-inch Monitor", stock 15 on a fresh DB - low enough that a modest burst exceeds it.
    private static final long PRODUCT_ID = Long.parseLong(System.getProperty("productId", "3"));
    private static final int CONCURRENT_ORDERS = Integer.parseInt(System.getProperty("concurrentOrders", "30"));

    private static final HttpClient client = HttpClient.newHttpClient();
    private static final AtomicInteger successCount = new AtomicInteger();
    private static volatile String token;
    private static volatile int stockBefore;

    @Override
    public void before() {
        token = login();
        stockBefore = fetchStock();
        System.out.printf(
                "[perf] product %d stock before burst: %d (firing %d concurrent order requests for qty=1 each)%n",
                PRODUCT_ID, stockBefore, CONCURRENT_ORDERS);
    }

    @Override
    public void after() {
        int stockAfter = fetchStock();
        int successes = successCount.get();
        int expectedStockAfter = stockBefore - successes;

        System.out.printf(
                "[perf] stockBefore=%d successfulOrders=%d expectedStockAfter=%d actualStockAfter=%d%n",
                stockBefore, successes, expectedStockAfter, stockAfter);

        if (stockAfter < 0) {
            throw new IllegalStateException(
                    "RACE CONDITION DETECTED: stock went negative (" + stockAfter + ") - oversold under concurrency");
        }
        if (stockAfter != expectedStockAfter) {
            throw new IllegalStateException(String.format(
                    "RACE CONDITION DETECTED: expected stock %d after %d successful orders (started at %d), "
                            + "but actual stock is %d. A concurrent update was lost.",
                    expectedStockAfter, successes, stockBefore, stockAfter));
        }

        System.out.println("[perf] stock bookkeeping is consistent - no lost update detected");
    }

    private static String login() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/auth/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            "{\"email\":\"demo@example.com\",\"password\":\"password123\"}"))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Login failed with status " + response.statusCode() + ": " + response.body());
            }
            Matcher matcher = Pattern.compile("\"token\":\"([^\"]+)\"").matcher(response.body());
            if (!matcher.find()) {
                throw new IllegalStateException("No token in login response: " + response.body());
            }
            return matcher.group(1);
        } catch (java.io.IOException | InterruptedException e) {
            throw new RuntimeException("Could not log in as demo user - is the backend running at " + BASE_URL + "?", e);
        }
    }

    private static int fetchStock() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/products/" + PRODUCT_ID))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Could not fetch product " + PRODUCT_ID + ": HTTP " + response.statusCode());
            }
            Matcher matcher = Pattern.compile("\"stock\":(\\d+)").matcher(response.body());
            if (!matcher.find()) {
                throw new IllegalStateException("No stock field in response: " + response.body());
            }
            return Integer.parseInt(matcher.group(1));
        } catch (java.io.IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private final HttpProtocolBuilder httpProtocol = http.baseUrl(BASE_URL);

    private final ScenarioBuilder placeOneOrder = scenario("Place one order for a contended product")
            .exec(session -> session.set("authHeader", "Bearer " + token))
            .exec(
                    http("Place order (race)")
                            .post("/api/orders")
                            .header("Authorization", "#{authHeader}")
                            .body(StringBody(session ->
                                    "{\"items\":[{\"productId\":" + PRODUCT_ID + ",\"quantity\":1}]}"))
                            .asJson()
                            .check(status().saveAs("httpStatus"))
            )
            .exec(session -> {
                if (session.getInt("httpStatus") == 201) {
                    successCount.incrementAndGet();
                }
                return session;
            });

    {
        setUp(
                placeOneOrder.injectOpen(atOnceUsers(CONCURRENT_ORDERS))
        ).protocols(httpProtocol);
    }
}
