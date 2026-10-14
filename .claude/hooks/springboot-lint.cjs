"use strict";

const path = require("path");
const fs = require("fs");
const {
  readPayload,
  allow,
  surfaceToModel,
  extractFileEdit,
  projectRoot,
  insideProject,
  runLint,
} = require("./_util.cjs");

const LINTED = /^src\/(?:main|test)\/java\/.+\.java$/;

const LINTERS = [
  {
    command: process.platform === "win32" ? "mvnw.cmd" : "./mvnw",
    args: (file) => ["-q", "spotless:check", "-DspotlessFiles=.*" + file.replace(/[.*+?^$(){}|[\]\\]/g, "\\$&")],
    requires: "mvnw",
  },
];

function main() {
  const payload = readPayload();
  const edit = extractFileEdit(payload);
  if (!edit) allow();

  const root = projectRoot(payload);
  if (!insideProject(root, edit.filePath)) allow();

  const relative = path.relative(root, path.resolve(root, edit.filePath)).replace(/\\/g, "/");
  if (!LINTED.test(relative)) allow();

  const reports = LINTERS.filter((linter) => !linter.requires || fs.existsSync(path.join(root, linter.requires)))
    .map((linter) => runLint(root, linter.command, linter.args(relative)))
    .filter(Boolean);
  if (reports.length === 0) allow();

  surfaceToModel(
    `Lint reported problems in ${relative}. Fix them now; CI runs the same check.\n\n${reports.join("\n\n").slice(0, 4000)}`,
  );
}

main();
