# ecom-app

A Java/Spring Boot + React e-commerce app: browse products without logging in, add them to a cart, then register/log in to check out.

## Project layout

```
backend/    Spring Boot 4 REST API (Java 17, Maven, H2 in-memory DB, JWT auth)
frontend/   React 19 SPA (Vite, react-router-dom, Bootstrap 5)
e2e-tests/  Selenium UI tests driving the running frontend + backend
```

## Prerequisites

- Java 17+ (JDK) — no local Maven install needed, each Java module has its own wrapper (`mvnw` / `mvnw.cmd`)
- Node.js 18+ and npm, for the frontend
- Chrome, if you want to run the Selenium tests in `e2e-tests/`

## Starting the app

You need both servers running, in two separate terminals.

**Backend** (http://localhost:8080):
```bash
cd backend
./mvnw spring-boot:run
```

**Frontend** (http://localhost:5173):
```bash
cd frontend
npm install   # first time only
npm run dev
```

On Windows PowerShell, use `.\mvnw.cmd spring-boot:run` for the backend.

Open [http://localhost:5173](http://localhost:5173) in a browser — that's the app. On first backend startup it seeds:
- 8 sample products
- One demo user: `demo@example.com` / `password123`

The H2 database is in-memory, so all data resets every time the backend restarts.

To stop either server, press `Ctrl+C` in its terminal (or, if it's running in the background and you don't have that terminal, find and kill whatever process is listening on port 8080 or 5173).

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
| POST   | `/api/orders`        | Yes (Bearer JWT)| Place an order from a list of items   |
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

**Backend** (unit + `@WebMvcTest` + a full `@SpringBootTest` integration test — no servers need to be running first):
```bash
cd backend
./mvnw test
```

**Selenium UI tests** (both the backend and frontend dev servers must already be running — see [e2e-tests/README.md](e2e-tests/README.md) for details):
```bash
cd e2e-tests
./mvnw test
```
