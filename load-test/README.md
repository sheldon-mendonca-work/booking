# Booking API k6 Load Test

This suite creates its own temporary users and event, then load-tests:

```text
POST /events/{eventId}/bookings
```

It does not claim a breaking point by itself. Determine the breaking point from measured latency, throughput, rejection count, and unexpected failure rate.

## Prerequisites

- The Spring Boot app is running.
- PostgreSQL and Redis are running.
- `k6` is installed.

No pre-created users, event, or JWTs are required.

## 🚀 Performance

The booking API was stress-tested using **k6** with increasing concurrent Virtual Users (VUs).

| Concurrent VUs | Throughput |  p95 Latency | Unexpected Failures |
| -------------: | ---------: | -----------: | ------------------: |
|              5 | ~503 req/s |  **12.5 ms** |                  0% |
|             10 |  ~86 req/s | **134.5 ms** |                  0% |
|             25 |  ~71 req/s |   **279 ms** |                  0% |
|             50 |  ~62 req/s |   **507 ms** |                  0% |
|            100 |  ~57 req/s |   **1.18 s** |                  0% |

### Baseline finding

At **100 concurrent VUs**, p95 latency crossed the configured **1 second** threshold.

Importantly, the system remained functionally correct throughout the test:

* **0% unexpected failures**
* No ticket overselling
* Successful bookings never exceeded event capacity
* Concurrent inventory updates remained consistent

The primary bottleneck is contention on the event's `availableTickets` row, which currently uses **PostgreSQL pessimistic row-level locking**.

## Run

From the project root:

```bash
BASE_URL=http://localhost:8080 \
k6 run --vus 50 --duration 30s load-test/booking-load-test.js
```

From this directory:

```bash
BASE_URL=http://localhost:8080 \
k6 run --vus 50 --duration 30s booking-load-test.js
```

## What Setup Does

At startup, the script:

1. Generates unique emails for this run.
2. Registers one `ADMIN` user.
3. Uses the returned admin `jwt` to create a fresh event.
4. Registers `TEST_USERS` `USER` customers.
5. Uses the returned user JWTs for concurrent booking requests.
6. Prints cleanup information at the end.

## Configuration

- `BASE_URL`: app base URL. Defaults to `http://localhost:8080`.
- `TEST_USERS`: number of temporary customer users to create. Defaults to `20`.
- `EVENT_CAPACITY`: capacity for the generated event. Defaults to `10000`.
- `QUANTITY`: booking quantity per request. Defaults to `1`.
- `TEST_PASSWORD`: password used for temporary users. Defaults to `LoadTest123!`.
- `SLEEP_SECONDS`: optional delay between each VU iteration. Defaults to `0`.
- `MAX_HTTP_FAILURE_RATE`: k6 threshold expression. Defaults to `rate<0.05`.
- `MAX_REQUEST_DURATION`: k6 threshold expression. Defaults to `p(95)<1000`.

Example:

```bash
BASE_URL=http://localhost:8080 \
TEST_USERS=50 \
EVENT_CAPACITY=20000 \
QUANTITY=1 \
MAX_HTTP_FAILURE_RATE='rate<0.10' \
MAX_REQUEST_DURATION='p(95)<2000' \
k6 run --vus 100 --duration 30s load-test/booking-load-test.js
```

## Progressive Test Sequence

Use progressively higher load:

```text
10 VUs -> 25 -> 50 -> 100 -> 200 -> 500
```

Example:

```bash
BASE_URL=http://localhost:8080 k6 run --vus 10 --duration 30s load-test/booking-load-test.js
BASE_URL=http://localhost:8080 k6 run --vus 25 --duration 30s load-test/booking-load-test.js
BASE_URL=http://localhost:8080 k6 run --vus 50 --duration 30s load-test/booking-load-test.js
```

For each run, record:

- VUs and duration
- `http_req_duration`, especially p95 and p99
- `http_req_failed`
- `booking_success_count`
- `booking_rejection_count`
- `booking_unexpected_failure_rate`
- requests per second
- app logs or database errors

The baseline breaking point is the first level where latency, error rate, or server stability becomes unacceptable for the assignment's chosen threshold.

## Manual Cleanup

The script does not automatically delete users or events. At the end it prints:

- event ID
- admin email
- admin JWT
- test user emails
- cleanup steps

Use the printed event ID and admin JWT to delete the event through the API:

```bash
curl -X DELETE http://localhost:8080/events/<printed_event_id> \
  -H "Authorization: Bearer <printed_admin_jwt>"
```

For local assignment testing, you can also clean up directly in the database:

```sql
DELETE FROM bookings
WHERE event_id = <printed_event_id>;

DELETE FROM events
WHERE id = <printed_event_id>;

DELETE FROM users
WHERE email = '<printed_admin_email>'
   OR email IN ('<printed_user_email_1>', '<printed_user_email_2>');
```

If needed, clear related local notification state:

```text
Redis queues:
- booking-confirmation-queue
- event-update-queue

Database table:
- outbox_events
```

## Correctness Verification

After every run, verify there was no overselling:

```sql
SELECT id, capacity, available_tickets
FROM events
WHERE id = <printed_event_id>;
```

```sql
SELECT COALESCE(SUM(quantity), 0) AS successful_booking_quantity
FROM bookings
WHERE event_id = <printed_event_id>
  AND status = 'SUCCESSFUL';
```

The invariants must hold:

```text
successful booking quantity <= event capacity
availableTickets >= 0
successful booking quantity + availableTickets = event capacity
```

## Notes

- Expected ticket exhaustion responses are counted as `booking_rejection_count`, not as unexpected failures.
- Setup requests are tagged separately from booking requests in k6.
- Do not run long or high-VU tests on shared machines without permission.
