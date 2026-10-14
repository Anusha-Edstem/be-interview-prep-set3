"use strict";

const path = require("path");
const { readPayload, block, allow, extractFileEdit, textAfterEdit, readFileOr, projectRoot } = require("./_util.cjs");

const PATTERNS = [
  [/-----BEGIN (?:RSA |EC |DSA |OPENSSH |PGP )?PRIVATE KEY-----/, "a private key block"],
  [/\bAKIA[0-9A-Z]{16}\b/, "an AWS access key id"],
  [/\baws_secret_access_key\s*[=:]\s*["']?[A-Za-z0-9/+=]{40}/i, "an AWS secret access key"],
  [/\bgh[pousr]_[A-Za-z0-9]{36,}\b/, "a GitHub token"],
  [/\bsk-ant-[A-Za-z0-9_-]{20,}\b/, "an Anthropic API key"],
  [/\bxox[baprs]-[A-Za-z0-9-]{10,}\b/, "a Slack token"],
  [/\bAIza[0-9A-Za-z_-]{35}\b/, "a Google API key"],
  [/\bsk_(?:live|test)_[A-Za-z0-9]{20,}\b/, "a Stripe secret key"],
  [/\beyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\b/, "a JSON web token"],
  [/\b(?:api[_-]?key|secret|password|passwd|token|credential)\s*[=:]\s*["'][^"'\s]{12,}["']/i, "a hardcoded credential"],
];

const CONFIG_PATTERN = [
  /^\s*(?:-\s+)?[-\w.]*(?:key|secret|password|passwd|token|credential)[-\w.]*\s*[:=]\s*["']?\S{8,}\s*$/i,
  "a credential in a configuration value",
  /^\s*(?:-\s+)?(?:keys?|restore-keys|cache-key|key-prefix)\s*[:=]/i,
];

const PLACEHOLDER =
  /<[^>]+>|\$\{|%[A-Za-z_]+%|\b(?:your[_-]?|example|sample|placeholder|changeme|dummy|redacted|xxx+)\b|process\.env|os\.environ|System\.getenv/i;

const EXEMPT_FILE = /\.env\.(?:example|sample|template)$|(?:^|[\\/])(?:package-lock\.json|pnpm-lock\.yaml|yarn\.lock)$/i;

const CONFIG_FILE = /\.(?:env[^\\/]*|ya?ml|ini|toml|properties|cfg|conf)$/i;

function offendingLines(text, pattern, exempt) {
  return text
    .split(/\r?\n/)
    .map((line, index) => ({ line, number: index + 1 }))
    .filter((entry) => pattern.test(entry.line) && !PLACEHOLDER.test(entry.line))
    .filter((entry) => !exempt || !exempt.test(entry.line));
}

function main() {
  const payload = readPayload();
  const edit = extractFileEdit(payload);
  if (!edit) allow();

  const root = projectRoot(payload);
  const absolute = path.resolve(root, edit.filePath);
  if (EXEMPT_FILE.test(absolute)) allow();

  const proposed = textAfterEdit(readFileOr(absolute, ""), edit.input);
  const checks = CONFIG_FILE.test(absolute) ? PATTERNS.concat([CONFIG_PATTERN]) : PATTERNS;

  for (const [pattern, label, exempt] of checks) {
    const hits = offendingLines(proposed, pattern, exempt);
    if (hits.length === 0) continue;
    const where = hits.map((hit) => `  line ${hit.number}`).join("\n");
    block(
      `Refused: this write puts ${label} into ${path.relative(root, absolute) || edit.filePath}.\n${where}\n\n` +
        "Credentials never enter a tracked file. Read the value from the environment and add a " +
        "placeholder entry to .env.example instead.",
    );
  }

  allow();
}

main();
