# be-interview-prep (Set 2)

Five Spring Boot features, each delivered as its own branch, pull request and merge into `main`.

| # | Question | PR link |
|---|---|---|
| 1 | Library API | _pending_ |
| 2 | Expense Tracker | [#7](https://github.com/Anusha-Edstem/be-interview-prep-set3/pull/7) |
| 3 | File Upload Service | _pending_ |
| 4 | API Rate Limiting | _pending_ |
| 5 | Appointment Booking | _pending_ |

**Video:** _to be added after the last merge_

## Stack

Java 17, Spring Boot 3.5, Spring Web MVC, Spring Data JPA, Bean Validation, Flyway, Actuator.
PostgreSQL in production, H2 (PostgreSQL mode) for tests and the `dev` profile. Maven wrapper, JUnit 5,
AssertJ, Mockito, MockMvc.

## Run

Requires JDK 17 or newer. Nothing else: the `dev` profile uses an H2 file database under `./data`.

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
curl http://localhost:8080/actuator/health
```

On Windows use `mvnw.cmd` in place of `./mvnw`.

To run against PostgreSQL instead, start it with `docker compose up -d`, copy `.env.example` to `.env`,
export its variables and run `./mvnw spring-boot:run` without the profile.

## Test

```bash
./mvnw -B -q spotless:check
./mvnw -B verify
```

`spotless:apply` fixes formatting. Every endpoint and the test that covers it is listed in
[`docs/api.md`](docs/api.md).

## Errors

Every error is an RFC 9457 `application/problem+json` body with `type`, `title`, `status`, `detail` and
`instance`; validation failures add an `errors` array of `{field, message}`.
