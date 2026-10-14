# Comment conventions

Every rule below is a gate. One violation fails the change — when writing it, when reviewing it, and
when auditing what already shipped.

- No comments. A source file contains code; any comment in it is an instant fail, whatever it says
  and however true it is.
- Code that needs narration is poor implementation. The fix is always the code — a better name, a
  smaller unit, an explicit type — never a sentence added beside it.
- The ban is categorical, not a list of bad kinds. Narration, summaries, region headings,
  documentation blocks, rationales, caveats, citations and bylines all fail alike. A Python docstring
  and a bare string statement are comments; Javadoc is a comment.
- No comment narrates history: why the code is shaped this way, what it was before, what defect or
  request changed it, or which item it came from. That record belongs to version control.
- No commented-out code. Deleted code lives in version history, which is the only place that records
  when and why it left.
- No deferred-work marker of any kind. Unfinished work is a tracked item with an owner, never a
  string in a source file where nothing will ever read it.
- No note asking a human to keep two things aligned. That alignment is the code's job, and the note
  is the defect rather than the mitigation.
- Comment syntax is permitted only where a machine reads it and no alternative exists, in its minimum
  form with no prose attached — `# noqa: E501`, `# type: ignore[arg-type]`, `// NOSONAR`. That is a
  directive to a tool, not a comment.
- API documentation lives in the OpenAPI metadata the framework already reads — a route `summary`, a
  field `description`, an `@Operation` annotation — never in a comment beside the handler.
- Knowledge that is not the code — a cause, a constraint, a prior behaviour, a removal condition —
  goes in the change description, the tracked item, or a test name. Never a comment.

This governs source files. Configuration that only a human will ever read — a CI workflow, a
dependency manifest, this repository's markdown — is not a source file and is not bound by it. Nor
are vendored build wrappers copied in unchanged from their upstream (`mvnw`, `mvnw.cmd`, `gradlew`,
`gradlew.bat`, `.mvn/`), whose license headers stay as shipped.

## Project-specific

None.
