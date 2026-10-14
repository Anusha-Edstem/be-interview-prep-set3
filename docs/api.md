# API catalogue

Every endpoint the service exposes, with the ID of the test that covers it. A test cites its ID at the
start of its `@DisplayName`. Errors are `application/problem+json` (RFC 9457).

## Platform

| Method | Path | Auth | Request | Response | Test |
|---|---|---|---|---|---|
| GET | `/actuator/health` | none | — | `200 {"status":"UP"}` | HEALTH-01 |
| any | unknown path | none | — | `404` problem detail | ERR-01 |

## Q1 — Library API

| Method | Path | Auth | Request | Response | Test |
|---|---|---|---|---|---|
| POST | `/api/v1/books` | none | `{"title","author","isbn","publishedYear"}` | `201` book + `Location`; `400` `validation-failed` with `errors[]`; `409` `duplicate-isbn` | LIB-01, LIB-02, LIB-03 |
| GET | `/api/v1/books?title=&author=&page=&size=` | none | — | `200` `PageResponse` of books, case-insensitive contains on title and author, sorted by title, size capped at 100; `400` search term over 255 chars | LIB-04 |
| GET | `/api/v1/books/{id}` | none | — | `200` book; `400` malformed id; `404` `not-found` | LIB-05, LIB-06 |
| PUT | `/api/v1/books/{id}` | none | `{"title","author","isbn","publishedYear"}` | `200` book; `400` `validation-failed`; `404` `not-found`; `409` `duplicate-isbn` | LIB-07, LIB-08 |
| DELETE | `/api/v1/books/{id}` | none | — | `204`; `404` `not-found`; `409` `book-borrowed` | LIB-09, LIB-10 |
| POST | `/api/v1/books/{id}/borrow` | none | `{"memberId"}` | `201` loan; `400` `validation-failed`; `404` `not-found`; `409` `book-unavailable` | LIB-11, LIB-12, LIB-13 |
| POST | `/api/v1/books/{id}/return` | none | — | `200` loan with `returnedAt`; `404` `not-found`; `409` `book-not-borrowed` | LIB-14, LIB-15 |

## Q2 — Expense Tracker

_Pending._

## Q3 — File Upload Service

_Pending._

## Q4 — API Rate Limiting

_Pending._

## Q5 — Appointment Booking

_Pending._
