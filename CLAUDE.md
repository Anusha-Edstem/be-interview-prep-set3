# CLAUDE.md

Thin router for this repository. It stays small and loads every session; everything else loads on
demand. Do not pre-read the convention files "just in case".

## Guardrails (hooks)

`.claude/hooks/` block secrets on write, block a comment added to a source file, block unsafe shell
commands, block commits to protected branches, and lint what was just written. They fire
automatically. If one blocks you, fix the cause; do not work around it.

## Where the rules live

| Topic | Source |
|---|---|
| Stack standards, what review flags as blocking | the `<stack>-conventions` skill named below |
| Comments in source files | `.claude/conventions/comment-conventions.md` |
| Git, commit and branch hygiene | `.claude/conventions/git-hygiene.md` |
| Issues, PR titles, merge rules | `.claude/conventions/github-pr.md` |
| Review method and the review body shape | `.claude/conventions/review-conventions.md`, `review-template.md` |
| Reviewer agents | `.claude/agents/` |
| The endpoints this service exposes | `docs/api.md` |

Each rule lives in one place; everything else points there. Do not repeat a rule here.

## Project-specific

This is the **only** register of this repo's values. Every convention file and skill that needs one
reads it here; none of them restates it, so a value changes in exactly one place. A value that
appears twice is a defect.

- **Repo**: `Anusha-Edstem/be-interview-prep-set3` — an interview-prep Spring Boot service exposing five
  feature APIs (library, expenses, file upload, rate-limited quotes, clinic booking) to HTTP clients
- **Stack**: Spring Boot 3.5.16 on Java 17, Spring Web MVC, Spring Data JPA, Bean Validation, Actuator,
  Flyway
- **Package Manager**: maven
- **Default Branch**: `main`
- **Protected Branches**: `main`, `master`
- **Branch Name**: `feature/q<N>-<slug>`
- **Ticket Prefix**: NULL
- **PR Title**: `Q<N> | <question title>` (10–100 chars)
- **PR Title Regex**: `^Q[1-5] \| .+$`
- **PR Template**: `.github/PULL_REQUEST_TEMPLATE.md`
- **Gate Commands**:
  - `./mvnw -B -q spotless:check`
  - `./mvnw -B verify`
- **Run Command**: `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`
- **Database**: PostgreSQL at runtime via `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`; H2 in PostgreSQL mode
  for tests and the `dev` profile; Flyway migrations under `src/main/resources/db/migration`
- **Version Bump**: NULL
- **Merge Method**: merge commit, so each PR's atomic commits stay visible on `main`; delete the branch
  after merge
- **Conventions Skills**: `springboot-conventions`
- **Reviewer Agents**: `senior-springboot-engineer`
- **Siblings**: NULL
