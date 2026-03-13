const express = require("express");
const { ping } = require("../repositories/timescaleRepository");

const router = express.Router();

router.get("/health", async (_req, res, next) => {
  try {
    await ping();
    res.json({
      status: "UP",
      service: "timescale-api",
      timestamp: new Date().toISOString(),
    });
  } catch (error) {
    next(error);
  }
});

module.exports = router;
