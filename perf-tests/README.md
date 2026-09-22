# perf-tests

Gatling performance / concurrency tests against the running backend. This is a separate Maven module — it is **not** part of the `backend` build.

## Prerequisites

- The backend running (does **not** need the frontend)

## Running

```bash
cd backend && ./mvnw spring-boot:run     # in one terminal
cd perf-tests && ./mvnw gatling:test     # in another
```

On Windows PowerShell, use `.\mvnw.cmd` for either.

Gatling writes an HTML report under `target/gatling/<simulation-name>-<timestamp>/index.html` — open it for latency/throughput charts. But for this module, the more important signal is whether the run **passes or fails**, printed to the console and reflected in the Maven exit code.

## ConcurrentOrderStockRaceSimulation

Fires many concurrent `POST /api/orders` requests (default: 30) at the same low-stock seeded product (default: product id 3, "27-inch Monitor", stock 15 on a fresh DB), each ordering quantity 1. This isn't primarily a throughput/latency test — it's a **correctness-under-concurrency** test: `OrderService.placeOrder` reads a product's stock, checks it, decrements it in memory, and saves — a classic read-then-write with no locking. Under concurrent load, that's a textbook lost-update race.

After the burst, the simulation compares `stockBefore - <count of HTTP 201 responses>` against the product's actual stock afterward. If they don't match, or stock goes negative, it throws and **fails the Maven build** with a message explaining the mismatch — you don't need to read the HTML report to know something's wrong.

### What we found running it

With the default seed data (stock 15) and 30 concurrent requests:

```
[perf] stockBefore=15 successfulOrders=30 expectedStockAfter=-15 actualStockAfter=11
RACE CONDITION DETECTED: expected stock -15 after 30 successful orders (started at 15),
but actual stock is 11. A concurrent update was lost.
```

All 30 requests got `201 Created` (none were correctly rejected with `409 Insufficient stock`), and the stock only dropped by 4 instead of being fully depleted and then correctly refusing the rest. This confirms `OrderService.placeOrder` has no protection against concurrent orders for the same product — it can both oversell (accept more orders than stock allows) and lose stock updates (the final count doesn't even reflect how many orders were actually accepted).

**This has not been fixed yet** — this module's job was to prove the bug exists, not fix it. The standard fixes are:
- Pessimistic locking: `SELECT ... FOR UPDATE` on the product row (e.g. `@Lock(LockModeType.PESSIMISTIC_WRITE)` on a repository method), so concurrent transactions serialize on that row.
- Optimistic locking: add `@Version` to `Product` and retry on `OptimisticLockException`.
- An atomic SQL update: `UPDATE products SET stock = stock - :qty WHERE id = :id AND stock >= :qty`, checking the affected-row count to know whether it succeeded — no read-then-write at the application layer at all.

### Configuration

Override via system properties:

```bash
./mvnw gatling:test -DbaseUrl=http://localhost:8080 -DproductId=3 -DconcurrentOrders=30
```

Note: successful runs permanently reduce that product's stock (H2 only resets on backend restart). If you want a clean 15-in-stock run again, restart the backend first.
