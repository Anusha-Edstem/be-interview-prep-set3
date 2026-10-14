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

| Method | Path | Auth | Request | Response | Test |
|---|---|---|---|---|---|
| POST | `/api/v1/expenses` | none | `{"amount":12.50,"category":"FOOD","date":"2026-10-01","note":"Lunch"}` — `amount` > 0 with at most 2 decimals, `category` one of `FOOD`/`TRAVEL`/`BILLS`/`OTHER`, `date` required, `note` optional (max 500) | `201` expense + `Location`; `400` problem detail with `errors[]` | EXP-01, EXP-02 |
| GET | `/api/v1/expenses` | none | query `from`, `to` (ISO dates, inclusive, each optional), `category`, `page`, `size` (capped at 100) | `200` `PageResponse` of expenses, newest date first; `400` when `from` is after `to` or a parameter is malformed | EXP-03, EXP-08, EXP-09 |
| GET | `/api/v1/expenses/summary` | none | query `month` (`yyyy-MM`, required) | `200 {"month":"2026-10","totals":{"FOOD":0.30,"TRAVEL":0.00,"BILLS":0.00,"OTHER":0.00},"overall":0.30}` — every category present, scale 2, first and last day of the month included; `400` missing or malformed month | EXP-10, EXP-11 |
| GET | `/api/v1/expenses/{id}` | none | — | `200` expense; `404` unknown id; `400` malformed id | EXP-04, EXP-05 |
| PUT | `/api/v1/expenses/{id}` | none | same body as POST | `200` revised expense; `400` invalid body; `404` unknown id | EXP-06, EXP-05 |
| DELETE | `/api/v1/expenses/{id}` | none | — | `204`; `404` unknown id | EXP-07, EXP-05 |

## Q3 — File Upload Service

| Method | Path | Auth | Request | Response | Test |
|---|---|---|---|---|---|
| POST | `/api/v1/files` | none | `multipart/form-data`, part `file`: JPEG, PNG or PDF, at most 5 MB | `201`, `Location: /api/v1/files/{id}`, `{id, originalName, contentType, size, uploadedAt}`; type detected from magic bytes, never from the client | FILE-01 |
| POST | `/api/v1/files` | none | content that is not JPEG/PNG/PDF (e.g. `virus.exe` renamed `photo.png`), or an extension that contradicts the content | `415` `unsupported-file-type` | FILE-02 |
| POST | `/api/v1/files` | none | file over 5 MB | `413` `file-too-large`; body over the 6 MB container limit: `413` `payload-too-large` | FILE-03 |
| POST | `/api/v1/files` | none | empty file / missing `file` part / not multipart | `400` `empty-file` / `400` `bad-request` / `415` `unsupported-media-type` | FILE-04 |
| POST | `/api/v1/files` | none | original name with path tricks (`../../evil.png`) | stored under a server-generated name inside the storage location; `originalName` reduced to `evil.png` | FILE-05 |
| GET | `/api/v1/files?page=&size=` | none | `page` (default 0), `size` (default 20, capped at 100) | `200` `PageResponse` of metadata, newest first | FILE-06 |
| GET | `/api/v1/files/{id}` | none | `id` UUID | `200` metadata; `404` `not-found`; `400` malformed id | FILE-07 |
| GET | `/api/v1/files/{id}/content` | none | `id` UUID | `200` bytes, `Content-Type` = detected type, `Content-Disposition: attachment` with the original name; `404` `not-found` | FILE-08 |
| DELETE | `/api/v1/files/{id}` | none | `id` UUID | `204`, record and stored file removed; `404` `not-found` | FILE-09 |
| — | upload → list → download → delete | none | — | end-to-end round trip on disk and database | FILE-10 |

## Q4 — API Rate Limiting

_Pending._

## Q5 — Appointment Booking

| Method | Path | Auth | Request | Response | Test |
|---|---|---|---|---|---|
| GET | `/api/v1/doctors` | none | `?page=&size=` (size capped at 100) | `200` page of `{id, name, specialty}` sorted by name | BOOK-01 |
| GET | `/api/v1/doctors/{doctorId}/slots` | none | `?date=YYYY-MM-DD` | `200 [{slotId, doctorId, startTime, endTime}]` — the day's free 30-minute slots (an expired hold counts as free), sorted by start; the day's slots are created on first read | BOOK-02 |
| GET | `/api/v1/doctors/{doctorId}/slots` | none | missing or malformed `date`, malformed `doctorId` | `400` problem detail | BOOK-03 |
| GET | `/api/v1/doctors/{doctorId}/slots` | none | unknown `doctorId` | `404` problem detail | BOOK-04 |
| POST | `/api/v1/slots/{slotId}/hold` | none (patient id in body) | `{"patientId": "<1-64 chars>"}` | `200 {slotId, doctorId, startTime, endTime, status: "HELD", patientId, holdExpiresAt}` — held for 5 minutes | BOOK-05 |
| POST | `/api/v1/slots/{slotId}/hold`, `/confirm`, `/cancel` | none | blank or missing `patientId`, missing body, malformed `slotId` | `400` problem detail (`validation-failed` with field errors, or `bad-request`) | BOOK-06 |
| POST | `/api/v1/slots/{slotId}/hold`, `/confirm`, `/cancel` | none | unknown `slotId` | `404` problem detail | BOOK-07 |
| POST | `/api/v1/slots/{slotId}/hold` | none | slot actively held or booked | `409` problem detail, type `slot-unavailable` | BOOK-08 |
| POST | `/api/v1/slots/{slotId}/hold` then `/confirm` | none | 20 patients holding and confirming the same slot at the same moment | exactly one booking; nineteen `409 slot-unavailable` on hold | BOOK-09 |
| POST | `/api/v1/slots/{slotId}/confirm` | none (patient id in body) | `{"patientId"}` of the patient holding the slot, before the hold expires | `200 {…, status: "BOOKED", holdExpiresAt: null}`; the booking notification is sent after commit | BOOK-10 |
| POST | `/api/v1/slots/{slotId}/confirm` | none | hold expired, held by another patient, or slot not held | `409` problem detail, type `hold-not-active` | BOOK-11 |
| — | scheduled, every `booking.expiry-sweep-interval` | — | an unconfirmed hold older than 5 minutes | the slot is listed and holdable again at once (lazy expiry); the sweep resets it to `AVAILABLE` in the table; a late confirm is `409 hold-not-active` | BOOK-12 |
| POST | `/api/v1/slots/{slotId}/cancel` | none (patient id in body) | `{"patientId"}` of the patient who booked the slot | `200 {…, status: "AVAILABLE", patientId: null}` — the slot is bookable again | BOOK-13 |
| POST | `/api/v1/slots/{slotId}/cancel` | none | slot not booked, or booked by another patient | `409` problem detail, type `booking-not-cancellable` | BOOK-14 |
| — | event after `/confirm` commits | — | `BookingConfirmedEvent` | notification logged once on the `booking-notify-` pool after commit, without delaying the response; none when the confirm is rejected or rolled back | BOOK-15 |
