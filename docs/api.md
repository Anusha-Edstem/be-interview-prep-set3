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

| Method | Path | Auth | Request | Response | Test |
|---|---|---|---|---|---|
| GET | `/api/v1/doctors` | none | `?page=&size=` (size capped at 100) | `200` page of `{id, name, specialty}` sorted by name | BOOK-01 |
| GET | `/api/v1/doctors/{doctorId}/slots` | none | `?date=YYYY-MM-DD` | `200 [{slotId, doctorId, startTime, endTime}]` — the day's free 30-minute slots (an expired hold counts as free), sorted by start; the day's slots are created on first read | BOOK-02 |
| GET | `/api/v1/doctors/{doctorId}/slots` | none | missing or malformed `date`, malformed `doctorId` | `400` problem detail | BOOK-03 |
| GET | `/api/v1/doctors/{doctorId}/slots` | none | unknown `doctorId` | `404` problem detail | BOOK-04 |
| POST | `/api/v1/slots/{slotId}/hold` | none (patient id in body) | `{"patientId": "<1-64 chars>"}` | `200 {slotId, doctorId, startTime, endTime, status: "HELD", patientId, holdExpiresAt}` — held for 5 minutes | BOOK-05 |
| POST | `/api/v1/slots/{slotId}/hold` | none | blank or missing `patientId`, missing body, malformed `slotId` | `400` problem detail (`validation-failed` with field errors, or `bad-request`) | BOOK-06 |
| POST | `/api/v1/slots/{slotId}/hold` | none | unknown `slotId` | `404` problem detail | BOOK-07 |
| POST | `/api/v1/slots/{slotId}/hold` | none | slot actively held or booked | `409` problem detail, type `slot-unavailable` | BOOK-08 |
| POST | `/api/v1/slots/{slotId}/hold` | none | 20 patients at the same moment | exactly one `200`, nineteen `409 slot-unavailable` | BOOK-09 |
