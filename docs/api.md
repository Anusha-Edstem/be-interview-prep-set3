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

| Method | Path | Auth | Request | Response | Test |
|---|---|---|---|---|---|
| POST | `/api/v1/files` | none | `multipart/form-data`, part `file`: JPEG, PNG or PDF, at most 5 MB | `201`, `Location: /api/v1/files/{id}`, `{id, originalName, contentType, size, uploadedAt}`; type detected from magic bytes, never from the client | FILE-01 |
| POST | `/api/v1/files` | none | content that is not JPEG/PNG/PDF (e.g. `virus.exe` renamed `photo.png`), or an extension that contradicts the content | `415` `unsupported-file-type` | FILE-02 |
| POST | `/api/v1/files` | none | file over 5 MB | `413` `file-too-large`; body over the multipart limit: `413` `payload-too-large` | FILE-03 |
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

_Pending._
