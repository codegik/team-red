const express = require("express");
const { Pool } = require("pg");

const app = express();
const port = Number.parseInt(process.env.PORT || "8090", 10);

const pool = new Pool({
  host: process.env.DB_HOST || "timescaledb",
  port: Number.parseInt(process.env.DB_PORT || "5432", 10),
  user: process.env.DB_USER || "sales",
  password: process.env.DB_PASSWORD || "sales123",
  database: process.env.DB_NAME || "salesdb",
  max: Number.parseInt(process.env.DB_POOL_MAX || "10", 10),
});

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
    const error = new Error(`Invalid '${fieldName}' date. Use ISO-8601.`);
    error.statusCode = 400;
    throw error;
  }

  return date.toISOString();
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

async function queryTopCities({ from, to, limit }) {
  const params = [];
  const filter = buildTimeFilter(from, to, params);

  if (filter) {
    params.push(limit);
    const sql = `
      SELECT
        city,
        MAX(region) AS region,
        SUM(total_revenue) AS total_revenue,
        SUM(total_quantity) AS total_quantity,
        SUM(total_sales) AS total_sales,
        MIN(bucket) AS first_bucket,
        MAX(bucket) AS last_bucket
      FROM top_cities
      ${filter}
      GROUP BY city
      ORDER BY total_revenue DESC, city ASC
      LIMIT $${params.length}
    `;

    const result = await pool.query(sql, params);
    return {
      mode: "range",
      rows: result.rows,
    };
  }

  const sql = `
    WITH latest_bucket AS (
      SELECT MAX(bucket) AS bucket
      FROM top_cities
    )
    SELECT
      tc.bucket,
      tc.city,
      tc.region,
      tc.total_revenue,
      tc.total_quantity,
      tc.total_sales
    FROM top_cities tc
    JOIN latest_bucket lb ON tc.bucket = lb.bucket
    ORDER BY tc.total_revenue DESC, tc.city ASC
    LIMIT $1
  `;

  const result = await pool.query(sql, [limit]);
  return {
    mode: "latest",
    rows: result.rows,
  };
}

async function queryTopSalesmen({ from, to, limit }) {
  const params = [];
  const filter = buildTimeFilter(from, to, params);

  if (filter) {
    params.push(limit);
    const sql = `
      SELECT
        salesman_name,
        salesman_email,
        MAX(region) AS region,
        SUM(total_revenue) AS total_revenue,
        SUM(total_quantity) AS total_quantity,
        SUM(total_sales) AS total_sales,
        MIN(bucket) AS first_bucket,
        MAX(bucket) AS last_bucket
      FROM top_salesmen
      ${filter}
      GROUP BY salesman_name, salesman_email
      ORDER BY total_revenue DESC, salesman_name ASC
      LIMIT $${params.length}
    `;

    const result = await pool.query(sql, params);
    return {
      mode: "range",
      rows: result.rows,
    };
  }

  const sql = `
    WITH latest_bucket AS (
      SELECT MAX(bucket) AS bucket
      FROM top_salesmen
    )
    SELECT
      ts.bucket,
      ts.salesman_name,
      ts.salesman_email,
      ts.region,
      ts.total_revenue,
      ts.total_quantity,
      ts.total_sales
    FROM top_salesmen ts
    JOIN latest_bucket lb ON ts.bucket = lb.bucket
    ORDER BY ts.total_revenue DESC, ts.salesman_name ASC
    LIMIT $1
  `;

  const result = await pool.query(sql, [limit]);
  return {
    mode: "latest",
    rows: result.rows,
  };
}

app.get("/health", async (_req, res, next) => {
  try {
    await pool.query("SELECT 1");
    res.json({
      status: "UP",
      service: "query-api-node",
      timestamp: new Date().toISOString(),
    });
  } catch (error) {
    next(error);
  }
});

app.get("/api/aggregates/top-sales-per-city", async (req, res, next) => {
  try {
    const from = parseIsoDate(req.query.from, "from");
    const to = parseIsoDate(req.query.to, "to");
    const limit = parseLimit(req.query.limit, 10);
    const data = await queryTopCities({ from, to, limit });

    res.json({
      source: "top_cities",
      mode: data.mode,
      filters: { from, to, limit },
      count: data.rows.length,
      items: data.rows,
    });
  } catch (error) {
    next(error);
  }
});

app.get("/api/aggregates/top-salesman-country", async (req, res, next) => {
  try {
    const from = parseIsoDate(req.query.from, "from");
    const to = parseIsoDate(req.query.to, "to");
    const limit = parseLimit(req.query.limit, 10);
    const data = await queryTopSalesmen({ from, to, limit });

    res.json({
      source: "top_salesmen",
      mode: data.mode,
      filters: { from, to, limit },
      count: data.rows.length,
      items: data.rows,
    });
  } catch (error) {
    next(error);
  }
});

app.get("/api/aggregates/summary", async (req, res, next) => {
  try {
    const from = parseIsoDate(req.query.from, "from");
    const to = parseIsoDate(req.query.to, "to");
    const cityLimit = parseLimit(req.query.cityLimit, 5);
    const salesmanLimit = parseLimit(req.query.salesmanLimit, 5);

    const [cities, salesmen] = await Promise.all([
      queryTopCities({ from, to, limit: cityLimit }),
      queryTopSalesmen({ from, to, limit: salesmanLimit }),
    ]);

    res.json({
      filters: { from, to, cityLimit, salesmanLimit },
      topSalesPerCity: {
        source: "top_cities",
        mode: cities.mode,
        count: cities.rows.length,
        items: cities.rows,
      },
      topSalesmanCountry: {
        source: "top_salesmen",
        mode: salesmen.mode,
        count: salesmen.rows.length,
        items: salesmen.rows,
      },
    });
  } catch (error) {
    next(error);
  }
});

app.use((error, _req, res, _next) => {
  const statusCode = error.statusCode || 500;
  console.error(error);
  res.status(statusCode).json({
    error: error.message || "Internal server error",
  });
});

async function start() {
  await pool.query("SELECT 1");
  app.listen(port, () => {
    console.log(`Query API listening on port ${port}`);
  });
}

async function shutdown(signal) {
  console.log(`Received ${signal}, shutting down`);
  await pool.end();
  process.exit(0);
}

process.on("SIGINT", () => {
  shutdown("SIGINT").catch((error) => {
    console.error(error);
    process.exit(1);
  });
});

process.on("SIGTERM", () => {
  shutdown("SIGTERM").catch((error) => {
    console.error(error);
    process.exit(1);
  });
});

start().catch((error) => {
  console.error("Failed to start query API", error);
  process.exit(1);
});
