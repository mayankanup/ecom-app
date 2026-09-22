package com.ecom.app.perf;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

/**
 * Sustained-load throughput test for POST /api/auth/login. BCryptPasswordEncoder is
 * deliberately CPU-expensive (that's the point of bcrypt), so this endpoint is expected to
 * have much higher per-request latency and a much lower sustainable throughput than a plain
 * read like GET /api/products - this test exists to put a number on that gap and catch a
 * regression (e.g. someone cranking up the bcrypt strength) rather than to prove a bug.
 *
 * Run with: ./mvnw gatling:test -Dgatling.simulationClass=com.ecom.app.perf.LoginThroughputSimulation
 * Override with -DbaseUrl=... -DrequestsPerSecond=... -DdurationSeconds=...
 */
public class LoginThroughputSimulation extends Simulation {

    private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");
    // Deliberately lower than the products-read test by default: bcrypt is slow by design.
    private static final int REQUESTS_PER_SECOND = Integer.parseInt(System.getProperty("requestsPerSecond", "10"));
    private static final int DURATION_SECONDS = Integer.parseInt(System.getProperty("durationSeconds", "15"));

    private final HttpProtocolBuilder httpProtocol = http.baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json");

    private final ScenarioBuilder login = scenario("Login")
            .exec(
                    http("POST /api/auth/login")
                            .post("/api/auth/login")
                            .body(StringBody("{\"email\":\"demo@example.com\",\"password\":\"password123\"}"))
                            .check(status().is(200))
            );

    {
        setUp(
                login.injectOpen(
                        constantUsersPerSec(REQUESTS_PER_SECOND).during(Duration.ofSeconds(DURATION_SECONDS))
                )
        )
                .protocols(httpProtocol)
                .assertions(
                        global().failedRequests().count().is(0L),
                        global().responseTime().percentile(95).lt(3000)
                );
    }
}
