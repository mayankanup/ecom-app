# e2e-tests

Selenium UI tests that drive the real frontend (and, through it, the real backend). This is a separate Maven project — it is **not** part of the `backend` build and is not run by `mvn test` there.

## Prerequisites

- A local Chrome/Chromium install (WebDriverManager downloads the matching `chromedriver` automatically)
- The backend, frontend, **and payment-service** dev servers all running — `CheckoutFlowTest` places a real order through a real checkout call, and since `OrderService` now charges through `PaymentClient` for every order (not just that test), the backend can't complete `POST /api/orders` at all without payment-service up.

## Running the tests

1. Start the backend:
   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```
2. Start payment-service, in a separate terminal:
   ```bash
   cd payment-service
   ./mvnw spring-boot:run
   ```
3. Start the frontend, in a separate terminal:
   ```bash
   cd frontend
   npm install   # first time only
   npm run dev
   ```
4. Run the Selenium tests, in a fourth terminal:
   ```bash
   cd e2e-tests
   ./mvnw test
   ```
   On Windows PowerShell, use `.\mvnw.cmd test` instead.

Tests run headless (`--headless=new`) by default, so no browser window pops up. To watch them run in a visible Chrome window instead:

```bash
mvn test -Dheadless=false
```

## Configuration

By default tests target `http://localhost:5173` (the Vite dev server). Override with the `APP_BASE_URL` system property or environment variable if the frontend is running somewhere else:

```bash
mvn test -DAPP_BASE_URL=http://localhost:4173
```

## What's covered

- `ProductBrowsingTest` — the home page lists seeded products and the product detail page is reachable, both without logging in
- `GuestCartTest` — adding a product to the cart as a guest updates the cart badge, and clicking "Proceed to Checkout" redirects to `/login` with a `redirect` param
- `AuthTest` — a wrong password shows an inline error; registering a brand-new account, logging out, and logging back in round-trips correctly
- `CheckoutFlowTest` — logging in as the seeded demo user (`demo@example.com` / `password123`), adding an item, completing checkout, and confirming the order shows up in order history

These tests create real data against whatever backend they're pointed at (H2 is in-memory and resets on backend restart, so nothing accumulates across a full restart — but running the suite twice against the same running backend will add more orders/users, which the assertions are written to tolerate).
