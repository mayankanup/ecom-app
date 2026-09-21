# ecom-app

A Java/Spring Boot e-commerce backend with a REST API: browse products without logging in, then register/log in to place an order. (Frontend is not built yet — this covers the backend only.)

## Project layout

```
backend/   Spring Boot 4 REST API (Java 17, Maven, H2 in-memory DB, JWT auth)
```

## Prerequisites

- Java 17+ (JDK)
- No local Maven install needed — use the wrapper (`mvnw` / `mvnw.cmd`) committed in `backend/`

## Starting the server

```bash
cd backend
./mvnw spring-boot:run
```

On Windows PowerShell, use `.\mvnw.cmd spring-boot:run` instead.

The app starts on **http://localhost:8080**. On first startup it seeds:
- 8 sample products
- One demo user: `demo@example.com` / `password123`

The H2 database is in-memory, so all data resets every time you restart the app.

To stop the server, press `Ctrl+C` in the terminal running it (or, if it's running in the background and you don't have that terminal, find and kill whatever process is listening on port 8080).

## API endpoints

| Method | Path                | Auth required? | Description                          |
|--------|----------------------|-----------------|---------------------------------------|
| GET    | `/api/health`        | No              | Liveness check                        |
| GET    | `/api/products`      | No              | List all products                     |
| GET    | `/api/products/{id}` | No              | Get one product                       |
| POST   | `/api/auth/register` | No              | Create an account                     |
| POST   | `/api/auth/login`    | No              | Log in, returns a JWT                 |
| POST   | `/api/orders`        | Yes (Bearer JWT)| Place an order from a list of items   |
| GET    | `/api/orders/my`     | Yes (Bearer JWT)| List the logged-in user's past orders |

## Manual testing

You can test with a browser (for `GET` requests), curl, or a tool like [Postman](https://www.postman.com/downloads/). Examples below use curl.

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

Replace `PASTE_TOKEN_HERE` with the token from step 3. `productId` values 1–8 exist from the seed data.

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer PASTE_TOKEN_HERE" \
  -d "{\"items\":[{\"productId\":1,\"quantity\":2}]}"
```

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
- Step 2 registering `demo@example.com` again → `409 Conflict` ("An account with this email already exists")
- Step 2/3 with a malformed body (e.g. missing `password`, or an invalid email) → `400 Bad Request` with a `fieldErrors` map
- Step 3 with the wrong password → `401 Unauthorized` ("Invalid email or password")

### Inspecting the database directly

Open [http://localhost:8080/h2-console](http://localhost:8080/h2-console) while the app is running:
- JDBC URL: `jdbc:h2:mem:ecomdb`
- User: `sa`
- Password: *(leave blank)*

From there you can run SQL against the `USERS`, `PRODUCTS`, `ORDERS`, and `ORDER_ITEMS` tables.

## Running the automated tests

```bash
cd backend
./mvnw test
```

This runs the full suite: Mockito unit tests for the service layer, `@WebMvcTest` slice tests for the controllers (including 401/404/409 cases), and an end-to-end `@SpringBootTest` that registers, logs in, places an order, and checks order history against a real (in-memory) database.
