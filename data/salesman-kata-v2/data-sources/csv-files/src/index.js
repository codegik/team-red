const Minio = require('minio');
const mockData = require('./data');

const MINIO_ENDPOINT = process.env.MINIO_ENDPOINT || 'minio';
const MINIO_PORT = parseInt(process.env.MINIO_PORT) || 9000;
const MINIO_ACCESS_KEY = process.env.MINIO_ACCESS_KEY || 'minioadmin';
const MINIO_SECRET_KEY = process.env.MINIO_SECRET_KEY || 'minioadmin123';
const MINIO_BUCKET = process.env.MINIO_BUCKET || 'sales-csv';
const MINIO_USE_SSL = process.env.MINIO_USE_SSL === 'true';
const GENERATION_INTERVAL = parseInt(process.env.GENERATION_INTERVAL) || 10000;
const RECORDS_PER_FILE = parseInt(process.env.RECORDS_PER_FILE) || 50;

const minioClient = new Minio.Client({
  endPoint: MINIO_ENDPOINT,
  port: MINIO_PORT,
  useSSL: MINIO_USE_SSL,
  accessKey: MINIO_ACCESS_KEY,
  secretKey: MINIO_SECRET_KEY
});

function randomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function randomElement(array) {
  return array[Math.floor(Math.random() * array.length)];
}

function randomStatus() {
  const rand = Math.random();
  if (rand < 0.7) return 'PENDING';
  if (rand < 0.95) return 'CONFIRMED';
  return 'CANCELLED';
}

function generateSaleId() {
  const date = new Date();
  const dateStr = date.toISOString().slice(0, 10).replace(/-/g, '');
  const random = randomInt(10000, 99999);
  return `CSV${dateStr}${random}`;
}

function formatDate(date) {
  return date.toISOString().slice(0, 19).replace('T', ' ');
}

function formatAmount(amount) {
  return amount.toFixed(2);
}

function generateRecord() {
  const product = randomElement(mockData.products);
  const salesman = randomElement(mockData.salesmen);
  const store = randomElement(mockData.stores);
  const quantity = randomInt(1, 5);
  const unitPrice = product.price;
  const totalAmount = quantity * unitPrice;

  const date = new Date();
  date.setHours(date.getHours() - randomInt(0, 24));

  return {
    sale_id: generateSaleId(),
    product_code: product.code,
    product_name: product.name,
    category: product.category,
    brand: product.brand,
    salesman_name: salesman.name,
    salesman_email: salesman.email,
    region: salesman.region,
    store_name: store.name,
    city: store.city,
    store_type: store.type,
    quantity: quantity,
    unit_price: formatAmount(unitPrice),
    total_amount: formatAmount(totalAmount),
    status: randomStatus(),
    sale_date: formatDate(date)
  };
}

function generateCSVContent(records) {
  const header = 'sale_id,product_code,product_name,category,brand,salesman_name,salesman_email,region,store_name,city,store_type,quantity,unit_price,total_amount,status,sale_date';
  const rows = records.map(r =>
    `${r.sale_id},${r.product_code},${r.product_name},${r.category},${r.brand},${r.salesman_name},${r.salesman_email},${r.region},${r.store_name},${r.city},${r.store_type},${r.quantity},${r.unit_price},${r.total_amount},${r.status},${r.sale_date}`
  );
  return [header, ...rows].join('\n');
}

function getFileName() {
  const date = new Date();
  const timestamp = date.toISOString().replace(/[-:]/g, '').replace('T', '_').slice(0, 15);
  return `sales_${timestamp}.csv`;
}

async function waitForMinio() {
  console.log('Waiting for MinIO...');
  while (true) {
    try {
      const exists = await minioClient.bucketExists(MINIO_BUCKET);
      if (exists) {
        console.log(`MinIO is ready. Bucket '${MINIO_BUCKET}' exists.`);
        return;
      }
      console.log(`Bucket '${MINIO_BUCKET}' not found yet, waiting...`);
    } catch (err) {
      console.log(`MinIO not ready: ${err.message}. Retrying...`);
    }
    await new Promise(resolve => setTimeout(resolve, 3000));
  }
}

async function uploadToMinio(fileName, content) {
  const buffer = Buffer.from(content, 'utf-8');
  await minioClient.putObject(MINIO_BUCKET, fileName, buffer, buffer.length, {
    'Content-Type': 'text/csv'
  });
  return { fileName, size: buffer.length };
}

async function generateAndUploadFile() {
  const records = [];
  const recordCount = randomInt(1, 5);

  for (let i = 0; i < recordCount; i++) {
    records.push(generateRecord());
  }

  const csvContent = generateCSVContent(records);
  const fileName = getFileName();

  const result = await uploadToMinio(fileName, csvContent);

  return { fileName: result.fileName, recordCount, size: result.size };
}

async function main() {
  console.log('CSV File Generator - MinIO Edition');
  console.log(`Config: endpoint=${MINIO_ENDPOINT}:${MINIO_PORT} | bucket=${MINIO_BUCKET}`);

  await waitForMinio();

  console.log('Generating initial CSV file...');
  const initial = await generateAndUploadFile();
  console.log(`Uploaded: ${initial.fileName} (${initial.recordCount} records, ${initial.size} bytes)`);
  console.log('Starting continuous CSV generation...\\n');

  let totalFiles = 1;
  let totalRecords = initial.recordCount;

  setInterval(async () => {
    try {
      const result = await generateAndUploadFile();
      totalFiles++;
      totalRecords += result.recordCount;

      const timestamp = new Date().toISOString();
      console.log(`[${timestamp}] Uploaded: s3://${MINIO_BUCKET}/${result.fileName}`);
      console.log(`  ${result.recordCount} records | ${result.size} bytes | Total: ${totalFiles} files, ${totalRecords} records`);

    } catch (err) {
      console.error('Error uploading CSV:', err.message);
    }
  }, GENERATION_INTERVAL);
}

process.on('SIGINT', () => {
  console.log('\\nShutting down');
  process.exit(0);
});

process.on('SIGTERM', () => {
  console.log('\\nShutting down');
  process.exit(0);
});

main().catch(console.error);
