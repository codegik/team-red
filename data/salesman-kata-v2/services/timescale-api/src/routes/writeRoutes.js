const express = require("express");
const {
  insertSale,
} = require("../repositories/timescaleRepository");

const router = express.Router();

router.post("/sales", async (req, res, next) => {
  try {
    const result = await insertSale(req.body || {});
    res.status(result.inserted ? 201 : 200).json(result);
  } catch (error) {
    next(error);
  }
});

module.exports = router;
