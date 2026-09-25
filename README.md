# ecom-app

A Java/Spring Boot + React e-commerce app: browse products without logging in, add them to a cart, then register/log in to check out.

## Project layout

```
backend/          Spring Boot 4 REST API (Java 17, Maven, H2 in-memory DB, JWT auth)
frontend/         React 19 SPA (Vite, react-router-dom, Bootstrap 5)
payment-service/  Spring Boot 4 - a local stand-in for a third-party gateway like Stripe
e2e-tests/        Selenium UI tests driving the running frontend + backend
perf-tests/       Gatling performance / concurrency tests against the backend
karate-tests/     Karate API tests for the order-placement flow against the running backend
pacts/            Generated Pact contract file shared between backend and payment-service (not committed, like target/)
```

## Prerequisites

- Java 17+ (JDK) — no local Maven install needed, each Java module has its own wrapper (`mvnw` / `mvnw.cmd`)
- Node.js 18+ and npm, for the frontend
- Chrome, if you want to run the Selenium tests in `e2e-tests/`

## Starting the app

You need three servers running, in three separate terminals — checkout charges through `payment-service` for every order, so it's not optional even for local dev.

**Backend** (http://localhost:8080):
```bash
cd backend
./mvnw spring-boot:run
```

**payment-service** (http://localhost:8081):
```bash
cd payment-service
./mvnw spring-boot:run
```

**Frontend** (http://localhost:5173):
```bash
cd frontend
npm install   # first time only
npm run dev
```

On Windows PowerShell, use `.\mvnw.cmd spring-boot:run` for the Java services.

Open [http://localhost:5173](http://localhost:5173) in a browser — that's the app. On first backend startup it seeds:
- 11 sample products
- One demo user: `demo@example.com` / `password123`

The H2 database is in-memory, so all data resets every time the backend restarts.

To stop any server, press `Ctrl+C` in its terminal (or, if it's running in the background and you don't have that terminal, find and kill whatever process is listening on port 8080, 8081, or 5173).

If you only want to browse products and use the cart (no checkout), you can skip `payment-service` — it's only needed once you actually place an order.

## Trying it out in the browser

1. Browse products at `/` — no login needed.
2. Click "Add to cart" on a few products.
3. Go to Cart, adjust quantities, then click "Proceed to Checkout".
4. Since you're not logged in, you'll land on Login with a `redirect` back to checkout. Click "Register" to create an account (or log in with the seeded demo account above) — either way you'll be sent back to Checkout automatically.
5. Click "Place Order" — you'll land on My Orders showing the order you just placed.

## API endpoints

| Method | Path                | Auth required? | Description                          |
|--------|----------------------|-----------------|---------------------------------------|
| GET    | `/api/health`        | No              | Liveness check                        |
| GET    | `/api/products`      | No              | List all products                     |
| GET    | `/api/products/{id}` | No              | Get one product                       |
| POST   | `/api/auth/register` | No              | Create an account                     |
| POST   | `/api/auth/login`    | No              | Log in, returns a JWT                 |
| POST   | `/api/orders`        | Yes (Bearer JWT)| Place an order from a list of items, charged through payment-service |
| GET    | `/api/orders/my`     | Yes (Bearer JWT)| List the logged-in user's past orders |

## Manual testing (API only)

The UI walkthrough above is the easiest way to try the app end-to-end. If you want to exercise the REST API directly — for debugging, or to check error responses — you can use a browser (for `GET` requests), curl, or a tool like [Postman](https://www.postman.com/downloads/). Examples below use curl.

### 1. Browse products (no login)

```bash
curl http://localhost:8080/api/products
```

Or just open [http://localhost:8080/api/products](http://localhost:8080/api/products) in a browser.

### 2. Register a new user

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Your Name\",\"email\":\"you@example.com\",\"password\":\"password123\"}"
```

Or skip this and use the seeded demo account (`demo@example.com` / `password123`).

### 3. Log in to get a JWT

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"demo@example.com\",\"password\":\"password123\"}"
```

The response looks like:

```json
{"token":"eyJhbGciOi...","id":1,"name":"Demo User","email":"demo@example.com"}
```

Copy the `token` value for the next steps.

### 4. Place an order

Replace `PASTE_TOKEN_HERE` with the token from step 3. `productId` values 1–11 exist from the seed data.

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer PASTE_TOKEN_HERE" \
  -d "{\"items\":[{\"productId\":1,\"quantity\":2}]}"
```

This charges Stripe's real "always succeeds" test card (`4242424242424242`), used automatically since `cardNumber` was omitted. To see a declined payment instead, pass Stripe's real generic-decline test card explicitly:

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer PASTE_TOKEN_HERE" \
  -d "{\"items\":[{\"productId\":1,\"quantity\":2}],\"cardNumber\":\"4000000000000002\"}"
```
→ `402 Payment Required`, and the stock is not decremented (the `@Transactional` rollback undoes it, same as an insufficient-stock failure).

### 5. View your order history

```bash
curl http://localhost:8080/api/orders/my \
  -H "Authorization: Bearer PASTE_TOKEN_HERE"
```

### 6. Expected failure cases

These are useful to check the error handling works, not just the happy path:

- Steps 4/5 with no `Authorization` header, or a garbage token → `401 Unauthorized`
- Step 4 with a very large `quantity` (e.g. `9999`) → `409 Conflict` ("Insufficient stock...")
- Step 4 with a `productId` that doesn't exist (e.g. `999`) → `404 Not Found`
- Step 4 with `cardNumber: "4000000000000002"` (Stripe's real decline test card) → `402 Payment Required`
- Step 4 with payment-service not running → `503 Service Unavailable` instead of a raw 500
- Step 2 registering `demo@example.com` again → `409 Conflict` ("An account with this email already exists")
- Step 2/3 with a malformed body (e.g. missing `password`, or an invalid email) → `400 Bad Request` with a `fieldErrors` map
- Step 3 with the wrong password → `401 Unauthorized` ("Invalid email or password")

### Inspecting the database directly

Open [http://localhost:8080/h2-console](http://localhost:8080/h2-console) while the app is running:
- JDBC URL: `jdbc:h2:mem:ecomdb`
- User: `sa`
- Password: *(leave blank)*

From there you can run SQL against the `USERS`, `PRODUCTS`, `ORDERS`, and `ORDER_ITEMS` tables.

## Pact contract testing

`backend` calls `payment-service` the way it would call real Stripe. That boundary is contract-tested with [Pact](https://docs.pact.io/): `backend`'s `PaymentClientPactTest` (the consumer) defines exactly what it expects from `POST /api/v1/charges` and writes that contract to `pacts/backend-payment-service.json`; `payment-service`'s `PaymentServiceProviderPactTest` (the provider) replays it against the real running controller. If payment-service's response shape ever drifts from what backend expects, the provider test fails immediately, with a diff of exactly what changed.

No Pact Broker here — this is a monorepo, not a multi-team deploy pipeline, so the contract file is just shared via a local directory instead (not committed, regenerated like `target/`). The **consumer test must run before the provider test** — see below.

```bash
cd backend && ./mvnw test -Dtest=PaymentClientPactTest   # generates pacts/backend-payment-service.json
cd payment-service && ./mvnw test                        # verifies against it (also runs payment-service's own unit/controller tests)
```

## Running the automated tests

**Backend** (unit + `@WebMvcTest` + a full `@SpringBootTest` integration test, including the Pact consumer test — no other servers need to be running first, `PaymentClient` is mocked where needed):
```bash
cd backend
./mvnw test
```

**payment-service** (unit + controller tests, plus the Pact provider verification — needs the pact file from the command above to exist first):
```bash
cd payment-service
./mvnw test
```

**Selenium UI tests** (the backend, frontend, **and payment-service** dev servers must already be running — see [e2e-tests/README.md](e2e-tests/README.md) for details):
```bash
cd e2e-tests
./mvnw test
```

**Performance / concurrency tests** (only the backend needs to be running for the two throughput simulations; the concurrency race simulation also needs payment-service — see [perf-tests/README.md](perf-tests/README.md) for details, including a real race condition this caught and how it was fixed):
```bash
cd perf-tests
./mvnw gatling:test -Dgatling.simulationClass=com.ecom.app.perf.ConcurrentOrderStockRaceSimulation
```

**Karate API tests** (the backend and payment-service dev servers must already be running — see [karate-tests/README.md](karate-tests/README.md) for details, including happy-path and 401/404/409/402 coverage of the order-placement API):
```bash
cd karate-tests
./mvnw test
```
