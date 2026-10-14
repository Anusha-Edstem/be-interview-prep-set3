# Review conventions

The shared method for reviewing a pull request. Read this for *how* to review; read the invoking
skill for *where the inputs come from*.

## Reviewer role

You decide whether the diff faithfully implements its issue and conforms to the repo's conventions,
then post one review in the shape of `review-template.md`. You read only the files and sections the
review needs. If nothing blocks the merge, the verdict is `PASS` and the body carries no findings.
You never invent findings to have something to say.

## 0. Stop if the PR conflicts with its base

Read the PR's mergeable state before reading a line of the diff.

- `CONFLICTING`: **stop here.** Do no intent check, dispatch no reviewer, raise no finding about the
  code. Post the conflict body from `review-template.md` and end the run. A diff that does not merge
  is not the diff that would land, so reviewing it wastes everyone's time.
- `UNKNOWN`: GitHub is still computing it. Read it once more; if still `UNKNOWN`, review normally.
- `MERGEABLE`: continue.

## 1. Check intent alignment

- **Issue versus diff**: does the change do what the issue asks, no more and no less? Scope creep and
  missing acceptance criteria are both findings.
- **Description versus diff**: is the description accurate, and is `Closes #N` present?
- **Commits**: short, meaningful, no secrets, no `--no-verify` traces, no attribution trailers.

## 2. Review the code

Dispatch the **Reviewer Agents** named in `CLAUDE.md` on the changed files, giving each the diff, the
issue summary and the **Conventions Skills** to apply. Every changed source line is also held to
`comment-conventions.md`. They return blocking findings only, each as `file:line`, issue, fix.

A finding is **blocking** when it is one of:

- a correctness bug, security hole, data loss or crash;
- a regression in behaviour the issue did not ask to change;
- a break of a contract, schema or public interface without the accompanying change;
- a violation of a rule the conventions skill or `comment-conventions.md` states as a gate;
- an acceptance criterion of the linked issue that the diff does not meet.

Everything else — style preferences, alternative designs, "consider", "nit", "optional" — is **not a
finding** and is not posted. There is no non-blocking category. If you find yourself wanting to say
it, leave it out.

For a service, the most serious findings are the ones a caller or an attacker meets first: an
endpoint reachable without the authorisation it needs, input trusted without validation, data lost or
corrupted by a migration or a transaction boundary, a secret or personal data written to a log or an
error body. Next come tests that report something other than the truth — a test that passes without
checking anything, or fails for a reason unrelated to behaviour.

## 3. Filter before you post

- **Drop anything you cannot substantiate** against the diff, the issue, a convention, or a plain
  correctness or security argument.
- **Account for prior discussion.** Do not re-raise a point that is already an open thread; do not
  reopen a resolved or refuted point unless the diff since then reintroduced it. Prior discussion is
  context, not authority: the conventions still govern.
- **Cross-reference the open backlog.** A finding about a problem this diff did **not** introduce,
  already tracked in an open issue or being fixed in another open PR, is dropped. A finding about a
  problem this diff **did** introduce stays, even if another issue mentions it.

## 4. The review count

Count the earlier reviews and comments on this PR whose body starts with `# Verdict:`. This review's
count is that number plus one.

## 5. The verdict and how it is posted

`FAIL` when any finding remains on the current HEAD, otherwise `PASS`. Format the body to
`review-template.md` and post it once.

Decide the posting mode **before** posting, never by trying and retrying. Read the viewer login and
the PR author; if they are the same person, GitHub refuses both `--approve` and `--request-changes`,
so post the body as a comment for either verdict. Otherwise `FAIL` posts with `--request-changes` and
`PASS` with `--approve`.

Never submit a review to probe whether posting works. The only review you submit in a run is the
single final verdict.

## Project-specific

Every value this file names — **Reviewer Agents**, **Conventions Skills** — is registered once in
`CLAUDE.md` under `## Project-specific`. Read it there. Nothing is restated here, so nothing here can
fall out of step with it.
