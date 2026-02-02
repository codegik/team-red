const fs = require('fs');
const path = require('path');
const mockData = require('./data');

const OUTPUT_DIR = process.env.OUTPUT_DIR || '/data/inbox';
const GENERATION_INTERVAL = parseInt(process.env.GENERATION_INTERVAL) || 10000;
const RECORDS_PER_FILE = parseInt(process.env.RECORDS_PER_FILE) || 50;

function randomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function randomElement(array) {
  return array[Math.floor(Math.random() * array.length)];
}

function generateSaleId() {
  const date = new Date();
  const dateStr = date.toISOString().slice(0, 10).replace(/-/g, '');
  const random = randomInt(10000, 99999);
  return `TB${dateStr}${random}`;
}

function formatDate(date) {
  return date.toISOString().slice(0, 10);
}

function formatAmount(amount) {
  return amount.toFixed(2);
}

function generateRecord() {
  const product = randomElement(mockData.products);
  const seller = randomElement(mockData.sellers);
  const city = randomElement(mockData.cities);
  const quantity = randomInt(1, 3);
  const amount = product.price * quantity;
  
  const date = new Date();
  date.setHours(date.getHours() - randomInt(0, 24));

  return {
    sale_id: generateSaleId(),
    product_code: product.code,
    seller_code: seller.code,
    quantity: quantity,
    amount: formatAmount(amount),
    city: city,
    sale_date: formatDate(date)
  };
}

function generateCSVContent(records) {
  const header = 'sale_id,product_code,seller_code,quantity,amount,city,sale_date';
  const rows = records.map(r => 
    `${r.sale_id},${r.product_code},${r.seller_code},${r.quantity},${r.amount},${r.city},${r.sale_date}`
  );
  return [header, ...rows].join('\n');
}

function getFileName() {
  const date = new Date();
  const timestamp = date.toISOString().replace(/[-:]/g, '').replace('T', '_').slice(0, 15);
  return `sales_${timestamp}.csv`;
}

function ensureOutputDir() {
  if (!fs.existsSync(OUTPUT_DIR)) {
    fs.mkdirSync(OUTPUT_DIR, { recursive: true });
    console.log(`Created output directory: ${OUTPUT_DIR}`);
  }
}

function generateAndSaveFile() {
  const records = [];
  const recordCount = randomInt(Math.floor(RECORDS_PER_FILE / 2), RECORDS_PER_FILE);
  
  for (let i = 0; i < recordCount; i++) {
    records.push(generateRecord());
  }

  const csvContent = generateCSVContent(records);
  const fileName = getFileName();
  const filePath = path.join(OUTPUT_DIR, fileName);

  fs.writeFileSync(filePath, csvContent);

  return { fileName, recordCount, filePath };
}

async function main() {
  ensureOutputDir();

  console.log('Generating initial CSV file');
  const initial = generateAndSaveFile();
  console.log(`Created: ${initial.fileName} (${initial.recordCount} records)`);
  console.log('Starting continuous CSV generation');

  let totalFiles = 1;
  let totalRecords = initial.recordCount;

  setInterval(() => {
    try {
      const result = generateAndSaveFile();
      totalFiles++;
      totalRecords += result.recordCount;

      const timestamp = new Date().toISOString();
      console.log(`[${timestamp}] Created: ${result.fileName}`);
      console.log(`${result.recordCount} records | Total files: ${totalFiles} | Total records: ${totalRecords}`);

    } catch (err) {
      console.error('Error generating CSV:', err.message);
    }
  }, GENERATION_INTERVAL);
}

process.on('SIGINT', () => {
  console.log('\nShutting down');
  process.exit(0);
});

process.on('SIGTERM', () => {
  console.log('\nShutting down');
  process.exit(0);
});

main();