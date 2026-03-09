const express = require('express');
const { MongoClient } = require('mongodb');
const { generatePaymentConfirmation } = require('./data');

const app = express();
const PORT = process.env.PORT || 8080;
const MONGO_URI = process.env.MONGO_URI || 'mongodb://mongo:27017';
const DB_NAME = 'eletrored_payments';
const COLLECTION = 'confirmations';
const DEFAULT_PAGE_SIZE = 100;

app.use(express.json());
app.use(express.text({ type: 'text/xml' }));

let db;

async function connectMongo() {
  const client = new MongoClient(MONGO_URI);
  await client.connect();
  db = client.db(DB_NAME);
  await db.collection(COLLECTION).createIndex({ paymentId: 1 }, { unique: true });
  console.log('Connected to MongoDB');
}

app.get('/health', (req, res) => {
  if (!db) return res.status(503).json({ status: 'unavailable' });
  res.json({ status: 'ok' });
});

app.post('/notify-payment', async (req, res) => {
  const { saleId, amount } = req.body;

  if (!saleId || amount == null) {
    return res.status(400).json({ error: 'saleId and amount are required' });
  }

  const confirmation = generatePaymentConfirmation(saleId, amount);

  try {
    await db.collection(COLLECTION).insertOne(confirmation);
  } catch (err) {
    if (err.code === 11000) {
      console.log(`Duplicate payment ${confirmation.paymentId} for sale ${saleId}, skipping`);
      return res.json({ paymentId: confirmation.paymentId, status: 'duplicate' });
    }
    throw err;
  }

  console.log(`Payment ${confirmation.paymentId} for sale ${saleId}: ${confirmation.status} (${confirmation.amount} BRL)`);

  res.json({ paymentId: confirmation.paymentId, status: confirmation.status });
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

app.post('/payment-validation', async (req, res) => {
  const { cursor, pageSize } = parseCursorFromXml(req.body || '');

  const query = cursor ? { paymentId: { $gt: cursor } } : {};
  const confirmations = await db.collection(COLLECTION)
    .find(query)
    .sort({ paymentId: 1 })
    .limit(pageSize)
    .toArray();

  const nextCursor = confirmations.length > 0
    ? confirmations[confirmations.length - 1].paymentId
    : cursor || '';

  const hasMore = confirmations.length === pageSize;

  const paymentsXml = confirmations.map(p => {
    let extra = '';
    if (p.rejectionReason) {
      extra = `\n          <pay:rejectionReason>${p.rejectionReason}</pay:rejectionReason>`;
    }
    return `        <pay:payment>
          <pay:paymentId>${p.paymentId}</pay:paymentId>
          <pay:saleId>${p.saleId}</pay:saleId>
          <pay:status>${p.status}</pay:status>
          <pay:amount>${p.amount}</pay:amount>
          <pay:currency>${p.currency}</pay:currency>
          <pay:paymentMethod>${p.paymentMethod}</pay:paymentMethod>
          <pay:bankReference>${p.bankReference}</pay:bankReference>
          <pay:confirmationDate>${p.confirmationDate}</pay:confirmationDate>${extra}
        </pay:payment>`;
  }).join('\n');

  const xml = `<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:pay="http://bank.eletrored.com/payment-validation">
  <soapenv:Body>
    <pay:GetPaymentConfirmationsResponse>
      <pay:totalRecords>${confirmations.length}</pay:totalRecords>
      <pay:nextCursor>${nextCursor}</pay:nextCursor>
      <pay:hasMore>${hasMore}</pay:hasMore>
      <pay:confirmations>
${paymentsXml}
      </pay:confirmations>
    </pay:GetPaymentConfirmationsResponse>
  </soapenv:Body>
</soapenv:Envelope>`;

  res.set('Content-Type', 'text/xml');
  res.send(xml);

  console.log(`SOAP poll: returned ${confirmations.length} confirmations (cursor: ${cursor || 'none'}, hasMore: ${hasMore})`);
});

async function start() {
  await connectMongo();
  app.listen(PORT, () => {
    console.log(`SOAP Payment Service running on port ${PORT}`);
  });
}

start().catch(err => {
  console.error('Failed to start:', err);
  process.exit(1);
});
