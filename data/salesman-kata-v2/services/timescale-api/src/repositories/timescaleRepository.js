const { pool } = require("../config/database");
const {
  buildTimeFilter,
  normalizeTimestamp,
  requireField,
  toNullableJson,
} = require("../utils/requestParsers");

const insertSaleSql = `
  INSERT INTO sales (
    sale_id, source, product_code, product_name, category, brand,
    salesman_name, salesman_email, region, store_name, city, store_type,
    quantity, unit_price, total_amount, status, sale_timestamp, trace_id
  )
  VALUES (
    $1, $2, $3, $4, $5, $6, $7, $8, $9,
    $10, $11, $12, $13, $14, $15, $16, $17, $18
  )
  ON CONFLICT (sale_id, sale_timestamp) DO NOTHING
`;

async function ping() {
  await pool.query("SELECT 1");
}

async function insertSale(payload) {
  requireField(payload.sale_id, "sale_id");
  requireField(payload.source, "source");
  requireField(payload.total_amount, "total_amount");

  const saleTimestamp = normalizeTimestamp(payload.sale_timestamp, "sale_timestamp");
  const result = await pool.query(insertSaleSql, [
    payload.sale_id,
    payload.source,
    payload.product_code || null,
    payload.product_name || null,
    payload.category || null,
    payload.brand || null,
    payload.salesman_name || null,
    payload.salesman_email || null,
    payload.region || null,
    payload.store_name || null,
    payload.city || null,
    payload.store_type || null,
    payload.quantity ?? 0,
    payload.unit_price ?? 0,
    payload.total_amount,
    payload.status || null,
    saleTimestamp,
    payload.trace_id || null,
  ]);

  return {
    inserted: result.rowCount > 0,
    saleId: payload.sale_id,
    saleTimestamp,
  };
}

async function queryTopCities({ from, to, limit }) {
  const params = [];
  const filter = buildTimeFilter(from, to, params);

  if (filter) {
    params.push(limit);
    const result = await pool.query(
      `
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
      `,
      params,
    );

    return { mode: "range", rows: result.rows };
  }

  const result = await pool.query(
    `
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
    `,
    [limit],
  );

  return { mode: "latest", rows: result.rows };
}

async function queryTopSalesmen({ from, to, limit }) {
  const params = [];
  const filter = buildTimeFilter(from, to, params);

  if (filter) {
    params.push(limit);
    const result = await pool.query(
      `
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
      `,
      params,
    );

    return { mode: "range", rows: result.rows };
  }

  const result = await pool.query(
    `
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
    `,
    [limit],
  );

  return { mode: "latest", rows: result.rows };
}

async function closePool() {
  await pool.end();
}

module.exports = {
  closePool,
  insertSale,
  ping,
  queryTopCities,
  queryTopSalesmen,
};
