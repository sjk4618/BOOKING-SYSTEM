# Reservation and Payment Platform Requirements

## 1. Overview

This project implements a first-come-first-served reservation system for a limited-quantity lodging product that opens at a fixed time, 00:00.

The system assumes a distributed environment with two or more application servers and must satisfy the following goals:

- Provide equal purchase opportunity to users
- Strictly preserve stock consistency
- Continue to behave safely during payment failure and system failure scenarios
- Document the reasoning behind architectural and technical choices

## 2. Technology Requirements

| Area | Requirement |
| --- | --- |
| Language | Java 8 or higher, or Kotlin |
| Framework | Spring Boot 2.7 or higher |
| RDB | MySQL or compatible database, such as MariaDB |
| Cache | Redis |
| Infra | Assume two or more application servers |
| Other | Additional choices are allowed when documented with rationale in `DECISIONS.md` |

## 3. Core Scenario

- Product type: special lodging product
- Opening time: 00:00
- Stock: 10 units
- Normal traffic: about 50 TPS
- Peak traffic: about 500 to 1000 TPS for 1 to 5 minutes after opening
- Scale-up or scale-out is assumed to be limited

## 4. Required APIs

### GET Checkout API

This API is used when a user enters the checkout page.

It should return:

- Product information
- Product name
- Price
- Check-in time
- Check-out time
- User's available point balance

This API should not mutate order, payment, or stock state.

### POST Booking API

This API receives checkout information, processes payment, and creates the final booking.

It should handle:

- Stock reservation
- Payment request
- Point usage
- Final booking creation
- Idempotency
- Compensation on failure

## 5. Functional Requirements

### Stock Consistency and Fairness

The system must prevent underselling and overselling during concentrated 00:00 traffic.

The design must consider how to provide users with equal opportunity in a distributed environment.

### High Availability

The system must include a strategy that prevents collapse during sudden TPS increases.

The approach and trade-offs must be documented in `DECISIONS.md`.

### Idempotency

Repeated payment requests sent within a very short interval from the checkout page must not create duplicate bookings, payments, or stock reservations.

### Payment Extensibility

Supported payment methods:

- Credit card
- Y Pay
- Y Point

Supported combined payments:

- Credit card + point
- Y Pay + point

Invalid combination:

- Credit card + Y Pay

The Booking API business logic should require minimal changes when a new payment method is added.

The extension structure and rationale must be documented in `DECISIONS.md`.

### Failure Handling

The system must define and implement strategies for:

- Redis failure and fallback behavior
- Payment failure such as limit exceeded
- Compensation when stock or point usage must be rolled back
- Recovery from inconsistent or stale pending state

The strategy and reasoning must be documented in `DECISIONS.md`.

## 6. External Payment Integration

Actual PG integration may be omitted.

However, the code should include interfaces or abstractions that allow the booking and payment flow to proceed structurally as if an external PG existed.

## 7. Out of Scope

User authentication, login, and security processing are outside the evaluation scope and may be omitted.

## 8. Required Deliverables

### Source Code

- The project must run without code changes.
- Any required infrastructure must be documented in `README.md`.

### README.md

The README should include:

- System architecture
- How to run the project
- API list
- Sequence diagram or flowchart
- ERD or DDL script focused on booking and payment domains

### DECISIONS.md

The decisions document should include:

- Major technical issues and chosen solutions
- Rationale behind library or infrastructure choices
- Failure handling strategy
- Trade-offs considered during design
- Cost and benefit of any additional infrastructure

Suggested format:

```text
## Issue 1. Title

### Context
Describe the problem.

### Options
Describe the considered options.

### Decision Rationale
Explain the trade-offs and why the final choice was made.
```

