"use strict";

const path = require("path");
const { readPayload, block, allow, extractFileEdit, textAfterEdit, readFileOr, projectRoot } = require("./_util.cjs");

const C_LIKE = /\.(?:ts|tsx|js|jsx|mjs|cjs|java|kt|kts|cs|go|swift|dart|scala|groovy)$/i;
const HASH_LIKE = /\.(?:py|pyi|rb|sh)$/i;

const DIRECTIVE =
  /^(?:\/\s*<reference\b|eslint-(?:disable|enable|env)\b|prettier-ignore\b|@ts-(?:ignore|expect-error|nocheck|check)\b|istanbul\s+ignore\b|[cv]8\s+ignore\b|noinspection\s|NOSONAR\b|noqa\b|type:\s*ignore\b|pragma:\s*no\s+cover\b|fmt:\s*(?:off|on|skip)\b|@formatter:(?:off|on)\b|spotless:(?:off|on)\b|(?:pylint|mypy|flake8|rubocop|ruff|pyright):)/i;

function isDirective(body) {
  return DIRECTIVE.test(body.trim());
}

const REGEX_BEFORE = /[(,=:[!&|?{};+\-*%~^>\n]/;

const REGEX_KEYWORDS = new Set([
  "return",
  "typeof",
  "instanceof",
  "in",
  "of",
  "case",
  "do",
  "else",
  "yield",
  "await",
  "new",
  "delete",
  "void",
  "throw",
]);

const REGEX_LITERALS = /\.(?:ts|tsx|js|jsx|mjs|cjs)$/i;

function scanCLike(text, filePath) {
  const found = [];
  const regexAware = REGEX_LITERALS.test(filePath);
  let index = 0;
  let line = 1;
  let previous = "\n";
  let previousWord = "";
  const length = text.length;

  while (index < length) {
    const char = text[index];
    const next = text[index + 1];

    if (/[A-Za-z_$]/.test(char)) {
      let end = index;
      while (end < length && /[A-Za-z0-9_$]/.test(text[end])) end += 1;
      previousWord = text.slice(index, end);
      previous = text[end - 1];
      index = end;
      continue;
    }

    if (char === "\n") {
      line += 1;
      index += 1;
      previous = "\n";
      previousWord = "";
      continue;
    }

    if (char === '"' && next === '"' && text[index + 2] === '"') {
      const end = text.indexOf('"""', index + 3);
      const stop = end === -1 ? length : end + 3;
      line += text.slice(index, stop).split("\n").length - 1;
      index = stop;
      previous = '"';
      previousWord = "";
      continue;
    }

    if (char === '"' || char === "'" || char === "`") {
      const quote = char;
      index += 1;
      while (index < length) {
        if (text[index] === "\\") {
          index += 2;
          continue;
        }
        if (text[index] === "\n") line += 1;
        if (text[index] === quote) {
          index += 1;
          break;
        }
        index += 1;
      }
      previous = quote;
      previousWord = "";
      continue;
    }

    if (char === "/" && next === "/") {
      const end = text.indexOf("\n", index);
      const stop = end === -1 ? length : end;
      found.push({ line, body: text.slice(index + 2, stop) });
      index = stop;
      continue;
    }

    if (char === "/" && next === "*") {
      const end = text.indexOf("*/", index + 2);
      const stop = end === -1 ? length : end + 2;
      const body = text.slice(index + 2, end === -1 ? length : end);
      found.push({ line, body });
      line += body.split("\n").length - 1;
      index = stop;
      continue;
    }

    if (regexAware && char === "/" && (REGEX_BEFORE.test(previous) || REGEX_KEYWORDS.has(previousWord))) {
      index += 1;
      let inClass = false;
      while (index < length) {
        const current = text[index];
        if (current === "\\") {
          index += 2;
          continue;
        }
        if (current === "\n") {
          index += 1;
          line += 1;
          break;
        }
        if (current === "[") inClass = true;
        else if (current === "]") inClass = false;
        else if (current === "/" && !inClass) {
          index += 1;
          break;
        }
        index += 1;
      }
      previous = "/";
      previousWord = "";
      continue;
    }

    if (!/\s/.test(char)) {
      previous = char;
      previousWord = "";
    }
    index += 1;
  }

  return found;
}

const STRING_PREFIX = /^(?:[rRbBuUfF]|[rR][bBfF]|[bBfF][rR])$/;
const QUOTES = /^[A-Za-z]*("""|'''|"|')|("""|'''|"|')$/g;

function stringEnd(text, from, quote) {
  const triple = text.startsWith(quote.repeat(3), from);
  const close = triple ? quote.repeat(3) : quote;
  let index = from + close.length;
  while (index < text.length) {
    if (text[index] === "\\") {
      index += 2;
      continue;
    }
    if (!triple && text[index] === "\n") return index;
    if (text.startsWith(close, index)) return index + close.length;
    index += 1;
  }
  return text.length;
}

function standsAlone(text, from) {
  let index = from;
  while (index < text.length && (text[index] === " " || text[index] === "\t")) index += 1;
  return index >= text.length || "\r\n#;".includes(text[index]);
}

function scanHashLike(text) {
  const found = [];
  const length = text.length;
  let index = 0;
  let line = 1;
  let depth = 0;
  let lineStart = true;
  let continued = false;

  if (text.startsWith("#!")) {
    const end = text.indexOf("\n");
    index = end === -1 ? length : end;
  }

  while (index < length) {
    const char = text[index];

    if (char === "\n") {
      line += 1;
      index += 1;
      if (depth === 0 && !continued) lineStart = true;
      continued = false;
      continue;
    }

    if (char === " " || char === "\t" || char === "\r") {
      index += 1;
      continue;
    }

    if (char === "\\" && (text[index + 1] === "\n" || text[index + 1] === "\r")) {
      continued = true;
      index += 1;
      continue;
    }

    if (char === "#") {
      const end = text.indexOf("\n", index);
      const stop = end === -1 ? length : end;
      found.push({ line, body: text.slice(index + 1, stop) });
      index = stop;
      continue;
    }

    let quoteAt = -1;
    if (char === '"' || char === "'") {
      quoteAt = index;
    } else if (/[A-Za-z_]/.test(char)) {
      let end = index;
      while (end < length && /[A-Za-z0-9_]/.test(text[end])) end += 1;
      if (STRING_PREFIX.test(text.slice(index, end)) && (text[end] === '"' || text[end] === "'")) {
        quoteAt = end;
      } else {
        index = end;
        lineStart = false;
        continue;
      }
    }

    if (quoteAt !== -1) {
      const statement = lineStart && depth === 0;
      const stop = stringEnd(text, quoteAt, text[quoteAt]);
      const literal = text.slice(index, stop);
      if (statement && standsAlone(text, stop)) found.push({ line, body: literal.replace(QUOTES, "") });
      line += literal.split("\n").length - 1;
      index = stop;
      lineStart = false;
      continue;
    }

    if (char === "(" || char === "[" || char === "{") depth += 1;
    else if ((char === ")" || char === "]" || char === "}") && depth > 0) depth -= 1;
    else if (char === ";" && depth === 0) {
      index += 1;
      lineStart = true;
      continue;
    }
    lineStart = false;
    index += 1;
  }

  return found;
}

function comments(filePath, text) {
  const scan = C_LIKE.test(filePath) ? scanCLike : HASH_LIKE.test(filePath) ? scanHashLike : null;
  if (!scan) return null;
  return scan(text, filePath).filter((entry) => !isDirective(entry.body));
}

function tally(entries) {
  const counts = new Map();
  for (const entry of entries) {
    const key = entry.body.trim();
    counts.set(key, (counts.get(key) || 0) + 1);
  }
  return counts;
}

function main() {
  const payload = readPayload();
  const edit = extractFileEdit(payload);
  if (!edit) allow();

  const root = projectRoot(payload);
  const absolute = path.resolve(root, edit.filePath);
  const current = readFileOr(absolute, "");
  const existing = comments(absolute, current);
  if (existing === null) allow();

  const proposed = comments(absolute, textAfterEdit(current, edit.input));
  const before = tally(existing);
  const added = proposed.filter((entry) => {
    const key = entry.body.trim();
    const remaining = before.get(key) || 0;
    if (remaining === 0) return true;
    before.set(key, remaining - 1);
    return false;
  });

  if (added.length === 0) allow();

  const listed = added
    .slice(0, 10)
    .map((entry) => `  line ${entry.line}: ${entry.body.trim().slice(0, 70)}`)
    .join("\n");

  block(
    `Refused: this write adds ${added.length} comment${added.length === 1 ? "" : "s"} to ` +
      `${path.relative(root, absolute) || edit.filePath}.\n${listed}\n\n` +
      "A source file contains code. Code that needs narration is poor implementation: the fix is a " +
      "better name, a smaller unit or an explicit type. A docstring and Javadoc are comments too. " +
      "Knowledge that is not the code belongs in the commit message, the issue or a test name. See " +
      ".claude/conventions/comment-conventions.md.",
  );
}

if (require.main === module) main();

module.exports = { comments };
