package com.ecom.app.perf;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

/**
 * Sustained-load throughput test for GET /api/products - the app's highest-traffic endpoint
 * (public, hit by every guest browsing without logging in). No correctness assertions beyond
 * "requests succeed and come back reasonably fast"; the interesting output is the throughput
 * and latency percentiles in the HTML report.
 *
 * Run with: ./mvnw gatling:test -Dgatling.simulationClass=com.ecom.app.perf.ProductsReadThroughputSimulation
 * Override with -DbaseUrl=... -DrequestsPerSecond=... -DdurationSeconds=...
 */
public class ProductsReadThroughputSimulation extends Simulation {

    private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");
    private static final int REQUESTS_PER_SECOND = Integer.parseInt(System.getProperty("requestsPerSecond", "30"));
    private static final int DURATION_SECONDS = Integer.parseInt(System.getProperty("durationSeconds", "20"));

    private final HttpProtocolBuilder httpProtocol = http.baseUrl(BASE_URL);

    private final ScenarioBuilder browseProducts = scenario("Browse products")
            .exec(
                    http("GET /api/products")
                            .get("/api/products")
                            .check(status().is(200))
            );

    {
        setUp(
                browseProducts.injectOpen(
                        constantUsersPerSec(REQUESTS_PER_SECOND).during(Duration.ofSeconds(DURATION_SECONDS))
                )
        )
                .protocols(httpProtocol)
                .assertions(
                        global().failedRequests().count().is(0L),
                        global().responseTime().percentile(95).lt(1000)
                );
    }
}
