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

### What we found running it (and the fix)

With the default seed data (stock 15) and 30 concurrent requests, this originally failed:

```
[perf] stockBefore=15 successfulOrders=30 expectedStockAfter=-15 actualStockAfter=11
RACE CONDITION DETECTED: expected stock -15 after 30 successful orders (started at 15),
but actual stock is 11. A concurrent update was lost.
```

All 30 requests got `201 Created` (none were correctly rejected with `409 Insufficient stock`), and the stock only dropped by 4 instead of being fully depleted and then correctly refusing the rest. `OrderService.placeOrder` had no protection against concurrent orders for the same product — it both oversold (accepted more orders than stock allowed) and lost stock updates (the final count didn't even reflect how many orders were actually accepted).

**Fixed** with pessimistic locking: `ProductRepository.findByIdForUpdate` uses `@Lock(LockModeType.PESSIMISTIC_WRITE)`, so the DB takes a row lock on the product for the duration of the enclosing `@Transactional` method, serializing concurrent orders for the same product instead of racing on a read-then-write. `OrderService.placeOrder` also now sorts a multi-item order's products by id before locking them, so two orders sharing products can never deadlock waiting on each other's locks in opposite order.

Rerunning the same 30-vs-15 scenario after the fix:

```
[perf] stockBefore=15 successfulOrders=15 expectedStockAfter=0 actualStockAfter=0
[perf] stock bookkeeping is consistent - no lost update detected
```

Exactly 15 of the 30 requests succeeded (matching available stock), the other 15 correctly got `409`, and the bookkeeping invariant holds. The other standard approaches we didn't use here, for reference:
- Optimistic locking: add `@Version` to `Product` and retry on `OptimisticLockException` — avoids holding a DB lock, but needs retry logic and can thrash under heavy contention.
- An atomic SQL update: `UPDATE products SET stock = stock - :qty WHERE id = :id AND stock >= :qty`, checking the affected-row count — no read-then-write at all, but reshapes the service method more than a one-line repository change.

### Configuration

Override via system properties:

```bash
./mvnw gatling:test -DbaseUrl=http://localhost:8080 -DproductId=3 -DconcurrentOrders=30
```

Note: successful runs permanently reduce that product's stock (H2 only resets on backend restart). If you want a clean 15-in-stock run again, restart the backend first.
