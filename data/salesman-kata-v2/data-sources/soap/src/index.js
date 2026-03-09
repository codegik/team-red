const express = require('express');
const { generateSale } = require('./data');

const app = express();
const PORT = process.env.PORT || 8080;
const DEFAULT_PAGE_SIZE = 100;

app.use(express.json());
app.use(express.text({ type: 'text/xml' }));

function randomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function generateSales(count) {
  const sales = [];
  for (let i = 0; i < count; i++) {
    sales.push(generateSale());
  }
  return sales;
}

app.get('/health', (req, res) => {
  res.json({ status: 'ok' });
});

function parseCursorFromXml(xml) {
  let pageSize = DEFAULT_PAGE_SIZE;

  const pageSizeMatch = xml.match(/<pageSize>(\d+)<\/pageSize>/);
  if (pageSizeMatch) pageSize = Math.min(parseInt(pageSizeMatch[1]), 1000);

  return { pageSize };
}

app.post('/sales', (req, res) => {
  const { pageSize } = parseCursorFromXml(req.body || '');

  // Generate a small batch of sales (1-5 records per request)
  const count = randomInt(1, 5);
  const sales = generateSales(count);

  const nextCursor = sales.length > 0
    ? sales[sales.length - 1].saleId
    : '';

  // Most of the time there's no more data (simulate sparse incoming sales)
  const hasMore = Math.random() > 0.8;

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
});

app.listen(PORT, () => {
  console.log(`SOAP Sales Service running on port ${PORT}`);
});
