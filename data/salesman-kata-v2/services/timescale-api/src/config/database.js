const { Pool } = require("pg");
const { env } = require("./env");

const pool = new Pool({
  host: env("TIMESCALEDB_HOST", env("DB_HOST", "timescaledb")),
  port: Number.parseInt(env("TIMESCALEDB_PORT", env("DB_PORT", "5432")), 10),
  user: env("TIMESCALEDB_USER", env("DB_USER", "sales")),
  password: env("TIMESCALEDB_PASSWORD", env("DB_PASSWORD", "sales123")),
  database: env("TIMESCALEDB_DATABASE", env("DB_NAME", "salesdb")),
  max: Number.parseInt(env("DB_POOL_MAX", "10"), 10),
});

module.exports = {
  pool,
};
