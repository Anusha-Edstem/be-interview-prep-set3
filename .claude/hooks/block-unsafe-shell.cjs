"use strict";

const { readPayload, block, allow } = require("./_util.cjs");

const SHELL_TOOLS = new Set(["Bash", "PowerShell"]);
const SHELLS = /^(?:sh|bash|zsh|dash|ksh|pwsh|powershell|iex|invoke-expression|python3?|node)$/i;
const DANGEROUS_TARGET = /^(?:\/|\/\*|~|~\/|\*|\.|\.\/|\$HOME|\$\{HOME\}|\$HOME\/|C:\\?|[A-Za-z]:\\\*?)$/;

function segments(command) {
  return command
    .split(/&&|\|\||;|\n/)
    .map((part) => part.trim())
    .filter(Boolean);
}

function tokenise(segment) {
  const tokens = segment.match(/"[^"]*"|'[^']*'|\S+/g) || [];
  return tokens.map((token) => token.replace(/^["']|["']$/g, ""));
}

function stripWrappers(tokens) {
  let index = 0;
  while (index < tokens.length && (/^[A-Za-z_][A-Za-z0-9_]*=/.test(tokens[index]) || /^(?:sudo|env|nohup|command|time)$/.test(tokens[index]))) {
    index += 1;
  }
  return tokens.slice(index);
}

function bundled(token, letter) {
  return /^-[A-Za-z]+$/.test(token) && token.includes(letter);
}

function gitCall(tokens) {
  if (!/(?:^|\/)git(?:\.exe)?$/.test(tokens[0] || "")) return null;
  let index = 1;
  const options = [];
  while (index < tokens.length && tokens[index].startsWith("-")) {
    options.push(tokens[index]);
    if (/^-[Cc]$/.test(tokens[index])) {
      options.push(tokens[index + 1] || "");
      index += 1;
    }
    index += 1;
  }
  return { options, subcommand: tokens[index] || "", rest: tokens.slice(index + 1) };
}

const RULES = [
  [
    "a force push",
    (git) =>
      git.subcommand === "push" &&
      git.rest.some((token) => token === "--force" || bundled(token, "f") || (/^\+./.test(token) && !token.startsWith("+refs/tags"))),
    "Use --force-with-lease, and never against a protected branch.",
  ],
  [
    "skipping git hooks",
    (git) =>
      (git.subcommand === "commit" && git.rest.some((token) => token === "--no-verify" || bundled(token, "n"))) ||
      (git.subcommand === "push" && git.rest.includes("--no-verify")),
    "Hooks exist to catch a bad change early. Fix the cause.",
  ],
  [
    "a history rewrite",
    (git) => git.subcommand === "filter-branch" || git.subcommand === "filter-repo" || (git.subcommand === "reflog" && git.rest[0] === "expire"),
    "Prefer a new commit.",
  ],
  [
    "disabling TLS verification",
    (git) => git.options.some((option) => /^http\.sslverify=(?:false|0|no|off)$/i.test(option)),
    "",
  ],
];

function recursiveDelete(tokens) {
  const name = (tokens[0] || "").toLowerCase();
  if (name === "rm") {
    const flags = tokens.slice(1).filter((token) => token.startsWith("-"));
    const recursive = flags.some((flag) => flag === "--recursive" || /^-[A-Za-z]*[rR]/.test(flag));
    const force = flags.some((flag) => flag === "--force" || /^-[A-Za-z]*f/.test(flag));
    return recursive && force && tokens.slice(1).some((token) => !token.startsWith("-") && DANGEROUS_TARGET.test(token));
  }
  if (name === "remove-item" || name === "rmdir" || name === "rd") {
    const recursive = tokens.some((token) => /^-recurse$/i.test(token) || /^\/s$/i.test(token));
    return recursive && tokens.slice(1).some((token) => !/^[-/]/.test(token) && DANGEROUS_TARGET.test(token));
  }
  return false;
}

function pipesNetworkToShell(command) {
  return command.split(/&&|\|\||;|\n/).some((pipeline) => {
    const stages = pipeline.split("|").map((stage) => stripWrappers(tokenise(stage.trim())));
    const fetchAt = stages.findIndex((stage) => /^(?:curl|wget|iwr|irm|invoke-webrequest|invoke-restmethod)$/i.test(stage[0] || ""));
    return fetchAt !== -1 && stages.slice(fetchAt + 1).some((stage) => SHELLS.test(stage[0] || ""));
  });
}

function insecureFetch(tokens) {
  return /^curl$/i.test(tokens[0] || "") && tokens.some((token) => token === "--insecure" || bundled(token, "k"));
}

function worldWritable(tokens) {
  if (!/^chmod$/.test(tokens[0] || "")) return false;
  const mode = tokens.slice(1).find((token) => !token.startsWith("-")) || "";
  if (/^[0-7]{3,4}$/.test(mode)) return (Number(mode[mode.length - 1]) & 2) !== 0;
  return mode.split(",").some((part) => /^[ugo]*a[ugo]*[+=][rwxXst]*w|^[ug]*o[ug]*[+=][rwxXst]*w|^[+=][rwxXst]*w/.test(part));
}

function main() {
  const payload = readPayload();
  if (!SHELL_TOOLS.has(payload.tool_name || "")) allow();
  const command = String((payload.tool_input || {}).command || "");
  if (!command.trim()) allow();

  const shown = command.slice(0, 160);

  if (pipesNetworkToShell(command)) {
    block(`Refused: this pipes a download straight into an interpreter.\n  ${shown}\n\nDownload to a file, read it, then run it.`);
  }

  for (const segment of segments(command)) {
    for (const stage of segment.split("|")) {
      const tokens = stripWrappers(tokenise(stage.trim()));

      if (recursiveDelete(tokens)) {
        block(`Refused: a recursive delete aimed at a root, home or wildcard target.\n  ${shown}\n\nName the exact project subdirectory.`);
      }
      if (insecureFetch(tokens)) {
        block(`Refused: disabling TLS verification.\n  ${shown}`);
      }
      if (worldWritable(tokens)) {
        block(`Refused: a world-writable chmod.\n  ${shown}\n\nGrant the narrowest permission that works.`);
      }

      const git = gitCall(tokens);
      if (!git) continue;
      for (const [label, test, hint] of RULES) {
        if (test(git)) block(`Refused: ${label}.\n  ${shown}${hint ? `\n\n${hint}` : ""}`);
      }
    }
  }

  allow();
}

main();
