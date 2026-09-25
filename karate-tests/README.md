# karate-tests

[Karate](https://github.com/karatelabs/karate) API tests for the order-placement flow (`POST /api/orders`, `GET /api/orders/my`), driving the real running backend over HTTP. This is a separate Maven module — it is **not** part of the `backend` build.

Karate was picked for this specific flow because it's the one API in this repo with real complexity worth showing off: it needs a login first to get a JWT (chaining a token from one call into the next is a classic Karate strength), it has business logic to assert on (computed total, stock decremented), and it has more distinct error paths than anything else here (401, 404, 409, 402 - that last one only exists since `payment-service` was wired in).

## Prerequisites

- The backend running
- **payment-service also running** — every order now charges through it, including the "declined card" scenario below

## Running

```bash
cd backend && ./mvnw spring-boot:run             # in one terminal
cd payment-service && ./mvnw spring-boot:run     # in another

cd karate-tests                                  # in another
./mvnw test
```

On Windows PowerShell, use `.\mvnw.cmd test`.

Karate writes an HTML report to `target/karate-reports/karate-summary.html` — open it in a browser for a readable view of each scenario's requests/responses.

## What's covered

`src/test/resources/karate/orders.feature` — a `Background` registers and logs in a fresh unique user for every scenario, then:

- **Happy path**: place an order with the default test card, assert the response shape (`status: PLACED`, correct `total`, correct line items), then confirm it shows up in `GET /api/orders/my`
- **401**: placing an order with no `Authorization` header
- **404**: ordering a product id that doesn't exist
- **409**: ordering more than the product's current stock (asks for `stock + 1`, so it's robust to whatever the current stock actually is rather than assuming specific seed data)
- **402**: paying with Stripe's real decline test card (`4000000000000002`), then confirming the product's stock is unchanged afterward (the rollback held)

## Configuration

Override the target backend with the `baseUrl` system property (read in `src/test/resources/karate-config.js`):

```bash
./mvnw test -DbaseUrl=http://localhost:8080
```

## A naming gotcha worth knowing

The JUnit runner class must be named to match Maven Surefire's default test-discovery patterns (`*Test.java`, `Test*.java`, etc.) — a `@Karate.Test`-annotated method in a class named e.g. `OrdersRunner.java` compiles fine but Surefire silently skips it (0 tests run, still reports `BUILD SUCCESS`). This module's runner is named `OrdersTest.java` for exactly that reason.
