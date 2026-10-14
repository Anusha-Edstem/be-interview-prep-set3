"use strict";

const { spawnSync } = require("child_process");
const { comments } = require("./no-comments.cjs");

function git(args) {
  const result = spawnSync("git", args, { encoding: "utf8", maxBuffer: 64 * 1024 * 1024 });
  if (result.status !== 0) return null;
  return result.stdout;
}

function tally(entries) {
  const counts = new Map();
  for (const entry of entries) {
    const key = entry.body.trim();
    counts.set(key, (counts.get(key) || 0) + 1);
  }
  return counts;
}

function added(base, file) {
  const after = comments(file, git(["show", `HEAD:${file}`]) || "");
  if (after === null) return [];
  const before = tally(comments(file, git(["show", `${base}:${file}`]) || "") || []);
  return after.filter((entry) => {
    const key = entry.body.trim();
    const remaining = before.get(key) || 0;
    if (remaining === 0) return true;
    before.set(key, remaining - 1);
    return false;
  });
}

function main() {
  const base = process.argv[2];
  if (!base) {
    process.stderr.write("usage: node .claude/hooks/check-added-comments.cjs <base-ref>\n");
    process.exit(2);
  }
  const mergeBase = (git(["merge-base", base, "HEAD"]) || "").trim();
  if (!mergeBase) {
    process.stderr.write(`Cannot find a merge base between ${base} and HEAD.\n`);
    process.exit(2);
  }

  const files = (git(["diff", "--name-only", "--diff-filter=AMR", `${mergeBase}...HEAD`]) || "").split("\n").filter(Boolean);
  const failures = files.flatMap((file) => added(mergeBase, file).map((entry) => `${file}:${entry.line}: ${entry.body.trim().slice(0, 80)}`));

  if (failures.length === 0) {
    process.stdout.write(`No comments added across ${files.length} changed file(s).\n`);
    return;
  }
  process.stderr.write(`Comments added to source files:\n${failures.join("\n")}\n\nSee .claude/conventions/comment-conventions.md.\n`);
  process.exit(1);
}

main();
