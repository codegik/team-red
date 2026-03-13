const express = require("express");
const {
  queryTopCities,
  queryTopSalesmen,
} = require("../repositories/timescaleRepository");
const { parseIsoDate, parseLimit } = require("../utils/requestParsers");

const router = express.Router();

router.get("/aggregates/top-sales-per-city", async (req, res, next) => {
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

router.get("/aggregates/top-salesman-country", async (req, res, next) => {
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

router.get("/aggregates/summary", async (req, res, next) => {
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

module.exports = router;
