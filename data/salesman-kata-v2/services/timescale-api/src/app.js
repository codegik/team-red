const express = require("express");
const aggregateRoutes = require("./routes/aggregateRoutes");
const healthRoutes = require("./routes/healthRoutes");
const writeRoutes = require("./routes/writeRoutes");

function createApp() {
  const app = express();

  app.use(express.json({ limit: "1mb" }));
  app.use(healthRoutes);
  app.use("/api", writeRoutes);
  app.use("/api", aggregateRoutes);

  app.use((error, _req, res, _next) => {
    const statusCode = error.statusCode || 500;
    console.error(error);
    res.status(statusCode).json({
      error: error.message || "Internal server error",
    });
  });

  return app;
}

module.exports = {
  createApp,
};
