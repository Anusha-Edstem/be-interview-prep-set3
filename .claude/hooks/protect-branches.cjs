"use strict";

const fs = require("fs");
const path = require("path");
const { spawnSync } = require("child_process");
const { readPayload, block, allow, projectRoot, registerValues } = require("./_util.cjs");

const WRITING = /^(?:commit|merge|revert|cherry-pick|am|rebase)$/;

function protectedBranches(root) {
  const registered = registerValues(root, "Protected Branches");
  return registered.length ? registered : ["main", "master"];
}

function segments(command) {
  return command
    .split(/&&|\|{1,2}|;|\n/)
    .map((part) => part.trim())
    .filter(Boolean);
}

function tokenise(segment) {
  const tokens = segment.match(/"[^"]*"|'[^']*'|\S+/g) || [];
  return tokens.map((token) => token.replace(/^["']|["']$/g, ""));
}

const TAKES_VALUE = new Set([
  "-C",
  "-c",
  "--work-tree",
  "--git-dir",
  "--namespace",
  "--config-env",
  "--exec-path",
  "--super-prefix",
]);

const LOCATES = new Set(["-C", "--work-tree"]);

const WRAPPERS = new Set(["sudo", "env", "nohup", "command", "time", "stdbuf", "doas"]);

function gitParts(tokens) {
  let index = 0;
  while (
    index < tokens.length &&
    (/^[A-Za-z_][A-Za-z0-9_]*=/.test(tokens[index]) || WRAPPERS.has(tokens[index]))
  ) {
    index += 1;
  }
  if (tokens[index] !== "git") return null;

  index += 1;
  let directory = null;
  while (index < tokens.length && tokens[index].startsWith("-")) {
    const token = tokens[index];
    const split = token.indexOf("=");
    const flag = split === -1 ? token : token.slice(0, split);

    if (LOCATES.has(flag)) {
      directory = split === -1 ? tokens[index + 1] || null : token.slice(split + 1);
    }

    index += split === -1 && TAKES_VALUE.has(flag) ? 2 : 1;
  }

  return { directory, subcommand: tokens[index] || "", rest: tokens.slice(index + 1) };
}

function nativePath(base, target) {
  if (process.platform !== "win32" || !target.startsWith("/")) return path.resolve(base, target);
  const converted = spawnSync("cygpath", ["-w", target], { encoding: "utf8", timeout: 5000 });
  if (converted.status === 0 && converted.stdout.trim()) return converted.stdout.trim();
  const drive = target.match(/^\/([A-Za-z])(?:\/(.*))?$/);
  if (drive) return `${drive[1].toUpperCase()}:\\${(drive[2] || "").replace(/\//g, "\\")}`;
  return path.resolve(base, target);
}

function gitOutput(cwd, args) {
  const result = spawnSync("git", args, { cwd, encoding: "utf8", timeout: 10000 });
  if (result.status !== 0) return null;
  const value = (result.stdout || "").trim();
  return value || null;
}

function currentBranch(cwd) {
  const named = gitOutput(cwd, ["rev-parse", "--abbrev-ref", "HEAD"]);
  if (named && named !== "HEAD") return named;
  return gitOutput(cwd, ["symbolic-ref", "--short", "HEAD"]);
}

function isDestructiveReset(parts) {
  return parts.subcommand === "reset" && parts.rest.some((token) => token === "--hard");
}

function switchedTo(parts, cwd) {
  if (parts.subcommand !== "switch" && parts.subcommand !== "checkout") return null;
  const rest = parts.rest;

  for (let i = 0; i < rest.length; i += 1) {
    if (rest[i] === "-c" || rest[i] === "-b" || rest[i] === "-C" || rest[i] === "-B") {
      return rest[i + 1] || null;
    }
  }

  if (rest.includes("--")) return null;
  if (rest.includes("-")) return gitOutput(cwd, ["rev-parse", "--abbrev-ref", "@{-1}"]);

  const positional = rest.filter((token) => !token.startsWith("-"));
  if (positional.length !== 1) return null;

  const target = positional[0];
  if (fs.existsSync(path.resolve(cwd, target))) return null;
  return target;
}

const SHELL_TOOLS = new Set(["Bash", "PowerShell"]);

function main() {
  const payload = readPayload();
  if (!SHELL_TOOLS.has(payload.tool_name || "")) allow();

  const command = (payload.tool_input || {}).command || "";
  if (!/\bgit\b/.test(command)) allow();

  const root = projectRoot(payload);
  const guarded = protectedBranches(root);
  let workingDirectory = root;
  let branch = null;

  for (const segment of segments(command)) {
    const tokens = tokenise(segment);

    if (tokens[0] === "cd" || tokens[0] === "pushd") {
      workingDirectory = tokens[1] ? nativePath(workingDirectory, tokens[1]) : root;
      branch = null;
      continue;
    }

    const parts = gitParts(tokens);
    if (!parts) continue;

    if (parts.subcommand === "switch" || parts.subcommand === "checkout") {
      const moved = switchedTo(parts, workingDirectory);
      if (moved) branch = moved;
      continue;
    }

    if (!WRITING.test(parts.subcommand) && !isDestructiveReset(parts)) continue;

    const directory = parts.directory
      ? nativePath(workingDirectory, parts.directory)
      : workingDirectory;
    const target = branch || currentBranch(directory);
    if (!target) {
      if (!fs.existsSync(directory)) continue;
      block(
        `Refused: could not tell which branch \`git ${parts.subcommand}\` would write to in ${directory}.

` +
          "Run it from the repository root on a non-protected branch, without a path the hook cannot resolve.",
      );
    }

    if (guarded.includes(target)) {
      block(
        `Refused: \`git ${parts.subcommand}\` would write to \`${target}\`, which is a protected branch.\n\n` +
          "Protected branches stay releasable at all times. Create a branch named per **Branch Name** " +
          "in CLAUDE.md, move the work onto it, and open a pull request.\n\n" +
          "  git switch -c <branch>",
      );
    }
  }

  allow();
}

main();
