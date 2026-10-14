# GitHub and PR conventions

Shared reference, read on demand. Not auto-loaded. The values it needs are in `CLAUDE.md` under
`## Project-specific`.

## Issues are the plan of record

- Every PR implements an issue and says **`Closes #N`** in its body. Work without an issue gets one
  first.
- Issues are plain-English user stories with acceptance criteria. They name outcomes, not files or
  line numbers.
- Labels are the repo's own; read them with `gh label list` rather than assuming a vocabulary.

## One closing keyword per issue

`Closes #46, #48` closes only #46. Write `Closes #46, closes #48`, then verify with
`gh pr view <N> --json closingIssuesReferences`.

## The PR

- The base branch is the **Default Branch**.
- The title follows **PR Title**. When **PR Title Regex** is set, CI rejects a title that does not
  match it, so check before creating.
- Read the file **PR Template** names and fill every section it defines. It is the single source of
  truth for the body.
- Tick a checklist item only when it is genuinely true.
- If **Version Bump** is set, the PR carries that bump exactly once, however many pushes it takes.
- No pasted diffs in the body, and no tool attribution anywhere in it.

## Review and merge

- One review is posted per round, in the shape of `review-template.md`. `PASS` approves, `FAIL`
  requests changes; on the author's own PR both post as a comment, because GitHub refuses a
  self-approval.
- Merge only when CI is green and the latest review is `PASS`. The method is in **Merge Method**.

## Creating the PR

```bash
git push -u origin HEAD
gh pr create --base <Default Branch> --title "<per PR Title>" --body-file pr-body.md
```

Delete `pr-body.md` afterwards.

## Project-specific

Every value this file names — **Default Branch**, **PR Title**, **PR Title Regex**, **PR Template**,
**Version Bump**, **Merge Method** — is registered once in `CLAUDE.md` under `## Project-specific`.
Read it there. Nothing is restated here, so nothing here can fall out of step with it.
