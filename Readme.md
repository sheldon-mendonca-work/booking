# Event Booking System

Production-oriented backend for an event booking platform supporting **Event Organizers** and **Customers**, with JWT authentication, role-based authorization, concurrent ticket booking, asynchronous notifications, transactional outbox processing, and real email delivery.

**Stack:** Java · Spring Boot · PostgreSQL · Redis · JWT · Resend · Docker · k6

For metrics checkout [here](https://github.com/sheldon-mendonca-work/booking/tree/master/load-test)

Emails can be sent as in the [image here](https://github.com/sheldon-mendonca-work/booking/blob/master/Email_sent.png) but not included in load test due to limits

---

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

### Optimization status

The baseline prioritizes **correctness and reliability** over maximum throughput.

Further optimization of inventory contention, database tuning, and worker throughput was **not implemented yet due to scope and time constraints**.

Potential next steps include atomic inventory updates / optimistic locking, connection-pool tuning, query optimization, and higher-concurrency background workers.

---

## Architecture

```text
                    REST API
                       |
              +--------+--------+
              |                 |
          Auth/JWT          Business APIs
                                |
                    +-----------+-----------+
                    |                       |
                PostgreSQL                Redis
                    |                       |
              Transactional            Job Queues
                 Outbox                    |
                                            |
                              +-------------+-------------+
                              |                           |
                       Booking Worker              Event Update Worker
                              |                           |
                              +-------------+-------------+
                                            |
                                          Resend
                                            |
                                      Customer Email
```

### Key design decision

Booking correctness is enforced at the database level:

```text
BEGIN TRANSACTION
    ↓
Lock event row
    ↓
Check availableTickets
    ↓
Decrease inventory
    ↓
Create booking
    ↓
Create outbox event
    ↓
COMMIT
```

This prevents two concurrent requests from consuming the same inventory.

---

# Core APIs

### Authentication

```http
POST /auth/register
POST /auth/login
```

JWT contains the authenticated user's ID and role.

### Events

```http
POST   /events
PUT    /events/{eventId}
DELETE /events/{eventId}
```

Event management is restricted to organizers, with ownership checks for updates/deletes.

### Bookings

```http
POST   /events/{eventId}/bookings
GET    /events/{eventId}/bookings
GET    /bookings
GET    /bookings/{id}
DELETE /bookings/{id}
```

Customers can only access and cancel their own bookings.

---

# Concurrency & Inventory

The main consistency requirement is preventing overselling.

For an event with 1 remaining ticket:

```text
Request A ──┐
            ├── PostgreSQL row lock ──> only one updates inventory
Request B ──┘
```

The booking transaction locks the event row before checking and decrementing `availableTickets`.

The load test validated this behavior under concurrent traffic.

**Tradeoff:** pessimistic locking provides strong correctness but introduces contention when many customers book the same event simultaneously. This is the primary performance limitation observed in testing.

---

# Asynchronous Processing

The assignment requires two background tasks.

## 1. Booking Confirmation

```text
Successful Booking
       ↓
Transactional Outbox
       ↓
Redis
       ↓
Booking Worker
       ↓
Resend
       ↓
Real Email
```

A real email was successfully delivered and manually verified.

High-volume k6 testing intentionally did **not** send thousands of real emails; email delivery was validated separately to avoid provider limits and unnecessary external traffic.

## 2. Event Update Notification

```text
Event Updated
      ↓
Transactional Outbox
      ↓
Redis
      ↓
Event Notification Worker
      ↓
Find customers who booked the event
      ↓
Send email notifications
```

Notification delivery is asynchronous so external email latency does not block the core API transaction.

---

# Why Transactional Outbox?

Publishing directly to Redis after a database update can create an inconsistent state:

```text
DB update succeeds
      ↓
Redis publish fails
      ↓
Business change exists
but notification is lost
```

Instead, the database transaction writes both:

```text
Business change
+
Outbox event
```

After commit, a publisher moves the outbox event to Redis.

This gives the system reliable handoff from the synchronous business transaction to asynchronous processing.

---

# Security

* JWT-based authentication
* Role-based authorization
* Organizer ownership validation
* Customer ownership validation
* Request validation using Jakarta Bean Validation
* Passwords stored as hashes
* Protected business endpoints

---

# Technology Choices

| Concern      | Choice               | Reason                                |
| ------------ | -------------------- | ------------------------------------- |
| API          | Spring Boot          | Mature Java backend ecosystem         |
| Database     | PostgreSQL           | ACID transactions + row-level locking |
| Queue        | Redis                | Lightweight asynchronous processing   |
| Reliability  | Transactional Outbox | Prevents lost background jobs         |
| Auth         | JWT                  | Stateless API authentication          |
| Email        | Resend               | Real transactional email delivery     |
| Load testing | k6                   | Concurrent API stress testing         |
| Deployment   | Docker               | Reproducible deployment               |

---

# Performance Tradeoffs

The implementation deliberately favors **correct ticket inventory over maximum raw throughput**.

The stress test demonstrates the tradeoff clearly:

```text
More concurrency
      ↓
More event-row contention
      ↓
Higher waiting time
      ↓
Higher p95 latency
```

At 100 VUs:

```text
~57 req/s
1.18 s p95
0% unexpected failures
```

### Not optimized yet due to scope/time constraints

The main areas identified for a subsequent iteration are:

* Replace/re-evaluate pessimistic locking with atomic inventory updates or optimistic concurrency
* Reduce database contention
* Tune PostgreSQL connection pooling
* Analyze query/index performance
* Increase background-worker parallelism
* Repeat the stress test after optimization and compare the delta

---

# Running Locally

### Start infrastructure

```bash
docker compose up -d
```

### Configure environment

```env
RESEND_API_KEY=<your-api-key>
EMAIL_FROM=<verified-sender>
```

### Start application

```bash
./mvnw spring-boot:run
```

### Run load test

```bash
BASE_URL=http://localhost:8080 \
TEST_USERS=100 \
EVENT_CAPACITY=10000 \
k6 run --vus 100 --duration 10s load-test/booking-load-test.js
```

---

# Assignment Coverage

| Requirement                     | Status                                          |
| ------------------------------- | ----------------------------------------------- |
| Organizer + Customer roles      | ✅                                               |
| Role-based API access           | ✅                                               |
| Event management                | ✅                                               |
| Ticket booking                  | ✅                                               |
| Concurrent booking protection   | ✅                                               |
| Background job processing       | ✅                                               |
| Real booking confirmation email | ✅ Verified                                      |
| Event update notifications      | ✅                                               |
| Transactional outbox            | ✅                                               |
| Stress testing                  | ✅                                               |
| Breaking point identified       | ✅                                               |
| Further optimization            | Not optimized yet due to scope/time constraints |

