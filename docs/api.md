# API catalogue

Every endpoint the service exposes, with the ID of the test that covers it. A test cites its ID at the
start of its `@DisplayName`. Errors are `application/problem+json` (RFC 9457).

## Platform

| Method | Path | Auth | Request | Response | Test |
|---|---|---|---|---|---|
| GET | `/actuator/health` | none | — | `200 {"status":"UP"}` | HEALTH-01 |
| any | unknown path | none | — | `404` problem detail | ERR-01 |

## Q1 — Library API

_Pending._

## Q2 — Expense Tracker

_Pending._

## Q3 — File Upload Service

_Pending._

## Q4 — API Rate Limiting

_Pending._

## Q5 — Appointment Booking

_Pending._
