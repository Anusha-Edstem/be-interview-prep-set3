"use strict";

const fs = require("fs");
const path = require("path");

function readPayload() {
  let raw = "";
  try {
    raw = fs.readFileSync(0, "utf8");
  } catch {
    return {};
  }
  try {
    return JSON.parse(raw || "{}");
  } catch {
    return {};
  }
}

function projectRoot(payload) {
  return process.env.CLAUDE_PROJECT_DIR || payload.cwd || process.cwd();
}

function block(message) {
  process.stderr.write(message.endsWith("\n") ? message : `${message}\n`);
  process.exit(2);
}

function allow() {
  process.exit(0);
}

function surfaceToModel(note, hookEventName) {
  const text = (note || "").trim();
  if (text) {
    process.stdout.write(
      JSON.stringify({
        hookSpecificOutput: {
          hookEventName: hookEventName || "PostToolUse",
          additionalContext: text,
        },
      }),
    );
  }
  process.exit(0);
}

function readFileOr(filePath, fallback) {
  try {
    return fs.readFileSync(filePath, "utf8");
  } catch {
    return fallback;
  }
}

function applyOneEdit(text, edit) {
  const from = edit.old_string;
  if (typeof from !== "string" || from === "") return text;
  const to = typeof edit.new_string === "string" ? edit.new_string : "";
  return edit.replace_all ? text.split(from).join(to) : text.replace(from, to);
}

function extractFileEdit(payload) {
  const input = payload.tool_input || {};
  const filePath = input.file_path || input.notebook_path || "";
  if (!filePath) return null;
  return { filePath, input, toolName: payload.tool_name || "" };
}

function textAfterEdit(current, input) {
  if (typeof input.content === "string") return input.content;
  if (typeof input.new_source === "string") return `${current}\n${input.new_source}`;
  if (Array.isArray(input.edits)) return input.edits.reduce(applyOneEdit, current);
  if (typeof input.old_string === "string") return applyOneEdit(current, input);
  return current;
}

function registerValues(root, key) {
  const text = readFileOr(path.join(root, "CLAUDE.md"), "");
  const pattern = new RegExp(`^\\s*-\\s+\\*\\*${key}\\*\\*:\\s*(.+)$`, "mi");
  const found = text.match(pattern);
  if (!found) return [];
  return found[1]
    .split(",")
    .map((part) => part.replace(/[`*]/g, "").trim())
    .filter((part) => part && part.toUpperCase() !== "NULL");
}

function insideProject(root, filePath) {
  const relative = path.relative(root, path.resolve(root, filePath));
  return relative !== "" && !relative.startsWith("..") && !path.isAbsolute(relative);
}

function runLint(root, command, args) {
  const { spawnSync } = require("child_process");
  const result = spawnSync(command, args, {
    cwd: root,
    encoding: "utf8",
    shell: process.platform === "win32",
    timeout: 60000,
  });
  if (result.error || result.status === 0) return "";
  return `${result.stdout || ""}${result.stderr || ""}`.trim();
}

module.exports = {
  readPayload,
  projectRoot,
  block,
  allow,
  surfaceToModel,
  readFileOr,
  extractFileEdit,
  textAfterEdit,
  registerValues,
  insideProject,
  runLint,
};
