---
name: springboot-conventions
description: The engineering standard for this repository's Spring Boot code. Load on demand when writing or reviewing anything under src/ or pom.xml.
---

# Spring Boot conventions

- **Scope**: `src/`, `pom.xml`

Apply it to the lines a change **adds or modifies**; a pre-existing violation in untouched code is a
backlog item, not a blocker. Surrounding code is never precedent for a new violation.

**Architecture**

- Package by feature: one package per module, layered `controller -> service -> repository ->
  entity`, with `dto` and `mapper`. A layer calls only the one below it.
- A controller maps HTTP to a service call and back. It holds no business rule and no repository.
- Entities never cross the controller boundary. Requests and responses are Java `record` DTOs.
- Dependencies are injected through the constructor into `final` fields. No field `@Autowired`.
- Configuration is bound with `@ConfigurationProperties` records. `@Value` is not scattered across
  beans, and nothing reads `System.getenv` directly.

**Data**

- The schema moves only through migrations. `ddl-auto` stays `validate`. A migration is never edited
  once merged; a correction is a new one. A migration stays compatible with the code still running
  against it.
- Primary keys are `UUID`, generated server side.
- Money is never `float` or `double`.
- Transactions are declared on the service; a read-only method is `@Transactional(readOnly = true)`.
- A query binds its parameters; it is never assembled by string concatenation. A relation read in a
  loop is fetched with `@EntityGraph` or `JOIN FETCH`, not one query per row.
- Enums persist as `EnumType.STRING`. Timestamps are `Instant`, set by `@CreationTimestamp` /
  `@UpdateTimestamp` or the database.
- A list endpoint takes `Pageable`, caps the page size, and returns `PageResponse<T>`.

**Errors and security**

- A missing entity raises a domain exception through `orElseThrow`; never `Optional.get()`.
- One `@RestControllerAdvice` maps exceptions to `ProblemDetail` (RFC 9457) with a stable `type`. A
  validation failure is 400 with the field errors; not found is 404; anything unexpected is 500 with
  no internal detail.
- No empty catch, no catch-and-log-and-continue, no bare `RuntimeException` thrown.
- Every request body and path variable is validated: `@Valid` on the body, constraints on each field.
- The identity behind an authorisation decision comes from the security context, never from the
  request body or path.
- Secrets come from `${ENV}` placeholders. Logging is SLF4J with `{}` parameters; no token,
  password or personal data is logged at any level.

**Testing**

- Every code path a change adds or modifies has a test that asserts behaviour.
- Controllers: `@WebMvcTest` with `MockMvc`, asserting status, body and the validation and not-found
  paths. Services: Mockito with `@ExtendWith(MockitoExtension.class)`. Repositories: `@DataJpaTest`
  against H2 with Flyway applied.
- Assertions use AssertJ. Exceptions are asserted with `assertThatThrownBy`.
- A test arranges, acts and asserts, separated by blank lines and named locals — never by
  `// given` markers, which are comments.
- Each test's `@DisplayName` starts with the `docs/api.md` ID it covers.
- No `Thread.sleep`; async work is awaited with Awaitility.

**Blocking in review**

- **Security** — an endpoint missing the authorisation it needs; identity taken from the request; a
  controller input without validation; a hardcoded secret; a token, password or personal data in a
  log; an error body leaking a stack trace, SQL or internal identifiers.
- **Data** — a schema change outside a migration, an edited merged migration, a migration that breaks
  running code; money in `float`/`double`; a concatenated query; an unbounded list endpoint.
- **Layering** — an entity in a request or response, business logic in a controller, a controller
  calling a repository, field injection.
- **Errors** — a swallowed exception, `Optional.get()`, an error not mapped to `ProblemDetail`.
- **False confidence** — a changed path with no test, a test with no assertion or one that cannot
  fail, `@Disabled` without a tracked reason, `Thread.sleep`.
- **Comments** — any comment or Javadoc added to a changed source file.

**Not flagged**

- Lombok-generated code, formatting Spotless owns, OpenAPI annotation verbosity, test fixture
  verbosity.
