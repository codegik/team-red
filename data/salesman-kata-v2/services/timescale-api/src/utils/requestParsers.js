const { badRequest } = require("./httpErrors");

function parseLimit(value, fallback = 10, max = 100) {
  const parsed = Number.parseInt(value || String(fallback), 10);
  if (Number.isNaN(parsed) || parsed <= 0) {
    return fallback;
  }
  return Math.min(parsed, max);
}

function parseIsoDate(value, fieldName) {
  if (!value) {
    return null;
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    throw badRequest(`Invalid '${fieldName}' date. Use ISO-8601.`);
  }

  return date.toISOString();
}

function requireField(value, fieldName) {
  if (value === undefined || value === null || String(value).trim() === "") {
    throw badRequest(`Field '${fieldName}' is required.`);
  }
}

function normalizeTimestamp(value, fieldName) {
  requireField(value, fieldName);
  return parseIsoDate(value, fieldName);
}

function buildTimeFilter(from, to, params, columnName = "bucket") {
  const clauses = [];

  if (from) {
    params.push(from);
    clauses.push(`${columnName} >= $${params.length}`);
  }

  if (to) {
    params.push(to);
    clauses.push(`${columnName} <= $${params.length}`);
  }

  return clauses.length > 0 ? `WHERE ${clauses.join(" AND ")}` : "";
}

function toNullableJson(value) {
  if (value === undefined || value === null) {
    return null;
  }
  return JSON.stringify(value);
}

module.exports = {
  buildTimeFilter,
  normalizeTimestamp,
  parseIsoDate,
  parseLimit,
  requireField,
  toNullableJson,
};
