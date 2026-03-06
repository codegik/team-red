const { Client } = require('pg');

const config = {
  host: process.env.DB_HOST || 'localhost',
  port: process.env.DB_PORT || 5432,
  user: process.env.DB_USER || 'electromart',
  password: process.env.DB_PASSWORD || 'electromart123',
  database: process.env.DB_NAME || 'electromart'
};

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

async function generateSale(client) {
  const products = await client.query('SELECT id, base_price FROM products');
  const salesmen = await client.query('SELECT id FROM salesmen');
  const stores = await client.query('SELECT id FROM stores');

  const product = randomElement(products.rows);
  const salesman = randomElement(salesmen.rows);
  const store = randomElement(stores.rows);

  const quantity = randomInt(1, 5);
  const unitPrice = parseFloat(product.base_price);
  const totalAmount = quantity * unitPrice;
  const status = randomStatus();

  const result = await client.query(
    `INSERT INTO sales (product_id, salesman_id, store_id, quantity, unit_price, total_amount, status)
     VALUES ($1, $2, $3, $4, $5, $6, $7)
     RETURNING sale_id, total_amount, status`,
    [product.id, salesman.id, store.id, quantity, unitPrice, totalAmount, status]
  );

  return result.rows[0];
}

async function generateMultipleSales(client, count) {
  const sales = [];
  for (let i = 0; i < count; i++) {
    const sale = await generateSale(client);
    sales.push(sale);
  }
  return sales;
}

async function main() {
  const client = new Client(config);

  try {
    console.log('Connecting to PostgreSQL...');
    await client.connect();
    console.log('Connected successfully!');

    console.log('Generating initial sales batch...');
    const initialSales = await generateMultipleSales(client, 100);
    console.log(`Generated ${initialSales.length} initial sales`);

    console.log('Starting continuous data generation (every 5 seconds)...');
    console.log('Press Ctrl+C to stop\n');

    let totalGenerated = initialSales.length;

    setInterval(async () => {
      try {
        const count = randomInt(1, 5);
        const newSales = await generateMultipleSales(client, count);
        totalGenerated += count;

        const timestamp = new Date().toISOString();
        console.log(`[${timestamp}] Generated ${count} new sales | Total: ${totalGenerated}`);

        newSales.forEach(sale => {
          console.log(`  -> Sale #${sale.sale_id}: R$${sale.total_amount} (${sale.status})`);
        });

      } catch (err) {
        console.error('Error generating sales:', err.message);
      }
    }, 5000);

  } catch (err) {
    console.error('Fatal error:', err);
    await client.end();
    process.exit(1);
  }
}

process.on('SIGINT', async () => {
  console.log('\nShutting down');
  process.exit(0);
});

process.on('SIGTERM', async () => {
  console.log('\nShutting down');
  process.exit(0);
});

main();
