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

| Method | Path | Auth | Request | Response | Test |
|---|---|---|---|---|---|
| GET | `/api/v1/quotes/random` | `X-API-Key` header | — | `200 {"text","author"}` with `X-RateLimit-Limit` / `X-RateLimit-Remaining` | RL-01 |
| GET | `/api/v1/quotes/random` | `X-API-Key` header | 11th request in the window | `429` problem `rate-limit-exceeded` with `Retry-After` header and `retryAfterSeconds` | RL-02 |
| GET | `/api/v1/quotes/random` | `X-API-Key` header | another key after one key is limited | `200`, limits counted per key | RL-03 |
| GET | `/api/v1/quotes/random` | none | missing or blank `X-API-Key` | `401` problem `missing-api-key` | RL-04 |
| GET | `/api/v1/quotes/random` | `X-API-Key` header | many simultaneous requests from one key | exactly `max-requests` admitted | RL-05 |
| GET | `/api/v1/quotes/random` | `X-API-Key` header | overridden `rate-limit.*` config | limit and window follow config; invalid config fails startup | RL-06 |
| — | — | — | — | idle keys swept every `window` | RL-07 |

Config: `rate-limit.max-requests` (default 10), `rate-limit.window` (default `1m`) and `rate-limit.paths` (default `/api/v1/quotes/**`), overridable by `RATE_LIMIT_MAX_REQUESTS` / `RATE_LIMIT_WINDOW`.

## Q5 — Appointment Booking

_Pending._
