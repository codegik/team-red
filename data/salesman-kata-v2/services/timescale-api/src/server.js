const { createApp } = require("./app");
const { env } = require("./config/env");
const { closePool, ping } = require("./repositories/timescaleRepository");

const app = createApp();
const port = Number.parseInt(env("PORT", "8090"), 10);

async function start() {
  await ping();
  app.listen(port, () => {
    console.log(`Timescale API listening on port ${port}`);
  });
}

async function shutdown(signal) {
  console.log(`Received ${signal}, shutting down`);
  await closePool();
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
  console.error("Failed to start Timescale API", error);
  process.exit(1);
});
