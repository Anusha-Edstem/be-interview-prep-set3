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

| Method | Path | Auth | Request | Response | Test |
|---|---|---|---|---|---|
| POST | `/api/v1/expenses` | none | `{"amount":12.50,"category":"FOOD","date":"2026-10-01","note":"Lunch"}` — `amount` > 0 with at most 2 decimals, `category` one of `FOOD`/`TRAVEL`/`BILLS`/`OTHER`, `date` required, `note` optional (max 500) | `201` expense + `Location`; `400` problem detail with `errors[]` | EXP-01, EXP-02 |
| GET | `/api/v1/expenses` | none | query `page`, `size` (capped at 100) | `200` `PageResponse` of expenses, newest date first | EXP-03 |
| GET | `/api/v1/expenses/{id}` | none | — | `200` expense; `404` unknown id; `400` malformed id | EXP-04, EXP-05 |
| PUT | `/api/v1/expenses/{id}` | none | same body as POST | `200` revised expense; `400` invalid body; `404` unknown id | EXP-06, EXP-05 |
| DELETE | `/api/v1/expenses/{id}` | none | — | `204`; `404` unknown id | EXP-07, EXP-05 |

## Q3 — File Upload Service

_Pending._

## Q4 — API Rate Limiting

_Pending._

## Q5 — Appointment Booking

_Pending._
