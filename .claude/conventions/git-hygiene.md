# Git hygiene

Shared reference, read on demand. Not auto-loaded, but it governs every change from the first commit
on a branch, not just at PR time. The branch names it needs are in `CLAUDE.md` under
`## Project-specific`.

## Branches

- The **Default Branch** and every branch in **Protected Branches** stay releasable at all times.
  Never commit directly to any of them; a hook blocks it.
- One branch per issue, cut from an up-to-date **Default Branch**, named per **Branch Name**. Keep
  branches short-lived.

```bash
git switch <Default Branch> && git pull --ff-only
git switch -c <branch per Branch Name>
```

## Commits

- Short, meaningful, imperative subject lines. No trailing period, no emoji, no body unless the
  change genuinely needs one.
- Atomic commits. Each commit is one coherent step that builds and passes on its own. A reviewer
  should be able to read the diff commit by commit, and a revert of any single commit should make
  sense.
- Stage selectively so unrelated edits do not ride along: name the paths the commit covers
  (`git add <path>…`), never a blanket `git add -A`. Do not reach for `git add -p` here — it needs a
  terminal to answer its prompts and it never offers an untracked file, so on a new scaffold it
  stages nothing and the commit that follows fails.
- Squash only true fixup noise before opening the PR; keep meaningful steps as separate commits.
- **No attribution of any kind.** No `Co-Authored-By`, no generated-by line, no session link, no tool
  name, in a commit message, a PR body, a changelog entry or a source file. `.claude/settings.json`
  empties `attribution.commit` and `attribution.pr` and sets `attribution.sessionUrl` to `false`;
  all three are required, and keep them that way.
- Never `--no-verify` and never skip a hook. Never commit secrets, credentials or `.env` values; use
  `.env.example` with placeholders.
- Do not rewrite shared history. Prefer a new commit over amending a pushed branch. If a pushed
  branch truly must be updated, use `--force-with-lease`, never `--force`, and never on a protected
  branch.

## Keeping a branch in sync

When your branch drifts behind its base, **merge the base into your branch; do not rebase**. Rebasing
a pushed branch rewrites history and forces everyone else to recover from it; a merge preserves
history and is safe to repeat.

```bash
git switch <Default Branch> && git pull --ff-only
git switch <your branch>
git merge <Default Branch>
```

## Before you push

- `git status` clean; no stray debug files, large binaries or generated artefacts.
- Every command under **Gate Commands** passes.
- Your branch is up to date with its base.

```bash
git status
git push -u origin HEAD
```

## Project-specific

Every value this file names — **Default Branch**, **Protected Branches**, **Branch Name**,
**Gate Commands** — is registered once in `CLAUDE.md` under `## Project-specific`. Read it there.
Nothing is restated here, so nothing here can fall out of step with it.
