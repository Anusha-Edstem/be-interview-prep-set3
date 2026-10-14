# Review body template

Every review uses this exact shape. It is written for a reader who does not code: one verdict word,
one count, one table. Nothing else goes in the body — no summary paragraph, no praise, no
non-blocking notes, no suggestions, no explanation of method, no attribution.

Write the body to a file and post it with `--body-file`.

---

```markdown
# Verdict: ✅ PASS

## Review count: 1
```

or

```markdown
# Verdict: ❌ FAIL

## Review count: 2

## Findings

| No. | File | Issue | Fix |
|---|---|---|---|
| 1 | `app/api/orders.py:31` | Any signed-in user can read another user's order by changing the id in the path. | Filter the query by the caller's user id and return 404 otherwise. |
| 2 | `tests/test_orders.py:18` | The test never asserts the response body, so it passes when the endpoint returns the wrong order. | Assert the returned id and total equal the created order's. |
```

or, when the branch conflicts with its base:

```markdown
# Verdict: ❌ FAIL

## Review count: 3

## Findings

| No. | File | Issue | Fix |
|---|---|---|---|
| 1 | `whole branch` | This branch conflicts with `main`, so nothing here can be merged or reviewed yet. | Merge `main` into the branch, resolve the conflicts and push; the review then runs again. |
```

---

## Rules for each part

- **Verdict**: `❌ FAIL` when at least one finding remains on the PR's current HEAD, otherwise
  `✅ PASS`. Nothing else appears on this line.
- **Review count**: how many times this PR has been reviewed, including this one. Count every earlier
  review or comment whose body starts with `# Verdict:` and add one. It never resets while the PR
  stays open.
- **Findings**: present only on `FAIL`. Omit the heading and the table entirely on `PASS`.
  - **No.**: 1, 2, 3 … in the order a developer should fix them, most serious first.
  - **File**: repo-root-relative path with the line or line range, in backticks.
  - **Issue**: what is wrong, in plain English, one line, at most 25 words. No jargon a newcomer
    could not follow. Name the consequence ("any user can read another user's order"), not the rule.
  - **Fix**: the corrected code in backticks if it fits on one line, otherwise what to do, at most 25
    words.
- **The conflict body**: the whole review when the PR is `CONFLICTING`. It carries exactly one
  finding, the conflict itself; the **File** cell is `whole branch`. No code finding is added to it,
  because no code was reviewed.
- Every finding is blocking. A point that would not stop the merge is not a finding and is not
  written anywhere in the body.

## Project-specific

None.
