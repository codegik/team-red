const express = require('express');
const { MongoClient } = require('mongodb');
const { generateSale } = require('./data');

const app = express();
const PORT = process.env.PORT || 8080;
const MONGO_URI = process.env.MONGO_URI || 'mongodb://mongo:27017';
const DB_NAME = 'electromart_sales';
const COLLECTION = 'sales';
const DEFAULT_PAGE_SIZE = 100;
const GENERATION_INTERVAL = parseInt(process.env.GENERATION_INTERVAL) || 5000;

app.use(express.json());
app.use(express.text({ type: 'text/xml' }));

let db;

async function connectMongo() {
  const client = new MongoClient(MONGO_URI);
  await client.connect();
  db = client.db(DB_NAME);
  await db.collection(COLLECTION).createIndex({ saleId: 1 }, { unique: true });
  console.log('Connected to MongoDB');
}

function randomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

async function generateAndStoreSales(count) {
  const sales = [];
  for (let i = 0; i < count; i++) {
    const sale = generateSale();
    await db.collection(COLLECTION).insertOne(sale);
    sales.push(sale);
  }
  return sales;
}

app.get('/health', (req, res) => {
  if (!db) return res.status(503).json({ status: 'unavailable' });
  res.json({ status: 'ok' });
});

function parseCursorFromXml(xml) {
  let cursor = null;
  let pageSize = DEFAULT_PAGE_SIZE;

  const cursorMatch = xml.match(/<cursor>(.*?)<\/cursor>/);
  if (cursorMatch) cursor = cursorMatch[1];

  const pageSizeMatch = xml.match(/<pageSize>(\d+)<\/pageSize>/);
  if (pageSizeMatch) pageSize = Math.min(parseInt(pageSizeMatch[1]), 1000);

  return { cursor, pageSize };
}

app.post('/sales', async (req, res) => {
  const { cursor, pageSize } = parseCursorFromXml(req.body || '');

  const query = cursor ? { saleId: { $gt: cursor } } : {};
  const sales = await db.collection(COLLECTION)
    .find(query)
    .sort({ saleId: 1 })
    .limit(pageSize)
    .toArray();

  const nextCursor = sales.length > 0
    ? sales[sales.length - 1].saleId
    : cursor || '';

  const hasMore = sales.length === pageSize;

  const salesXml = sales.map(s => `        <sale:record>
          <sale:saleId>${s.saleId}</sale:saleId>
          <sale:productCode>${s.productCode}</sale:productCode>
          <sale:productName>${s.productName}</sale:productName>
          <sale:category>${s.category}</sale:category>
          <sale:brand>${s.brand}</sale:brand>
          <sale:salesmanName>${s.salesmanName}</sale:salesmanName>
          <sale:salesmanEmail>${s.salesmanEmail}</sale:salesmanEmail>
          <sale:region>${s.region}</sale:region>
          <sale:storeName>${s.storeName}</sale:storeName>
          <sale:city>${s.city}</sale:city>
          <sale:storeType>${s.storeType}</sale:storeType>
          <sale:quantity>${s.quantity}</sale:quantity>
          <sale:unitPrice>${s.unitPrice}</sale:unitPrice>
          <sale:totalAmount>${s.totalAmount}</sale:totalAmount>
          <sale:status>${s.status}</sale:status>
          <sale:saleTimestamp>${s.saleTimestamp}</sale:saleTimestamp>
        </sale:record>`).join('\n');

  const xml = `<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:sale="http://electromart.com/sales">
  <soapenv:Body>
    <sale:GetSalesResponse>
      <sale:totalRecords>${sales.length}</sale:totalRecords>
      <sale:nextCursor>${nextCursor}</sale:nextCursor>
      <sale:hasMore>${hasMore}</sale:hasMore>
      <sale:sales>
${salesXml}
      </sale:sales>
    </sale:GetSalesResponse>
  </soapenv:Body>
</soapenv:Envelope>`;

  res.set('Content-Type', 'text/xml');
  res.send(xml);

  console.log(`SOAP poll: returned ${sales.length} sales (cursor: ${cursor || 'none'}, hasMore: ${hasMore})`);
});

async function start() {
  await connectMongo();

  console.log('Generating initial sales batch...');
  const initialSales = await generateAndStoreSales(100);
  console.log(`Generated ${initialSales.length} initial sales`);

  let totalGenerated = initialSales.length;

  console.log(`Starting continuous sales generation (every ${GENERATION_INTERVAL / 1000}s)...`);
  setInterval(async () => {
    try {
      const count = randomInt(1, 5);
      const newSales = await generateAndStoreSales(count);
      totalGenerated += count;

      const timestamp = new Date().toISOString();
      console.log(`[${timestamp}] Generated ${count} new sales | Total: ${totalGenerated}`);
      newSales.forEach(s => {
        console.log(`  -> Sale ${s.saleId}: ${s.productName} x${s.quantity} = R$${s.totalAmount} (${s.status})`);
      });
    } catch (err) {
      console.error('Error generating sales:', err.message);
    }
  }, GENERATION_INTERVAL);

  app.listen(PORT, () => {
    console.log(`SOAP Sales Service running on port ${PORT}`);
  });
}

start().catch(err => {
  console.error('Failed to start:', err);
  process.exit(1);
});
