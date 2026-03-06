const mockData = {
  products: [
    { name: 'iPhone 15 Pro 256GB', code: 'IPHONE15PRO256', category: 'SMARTPHONE', brand: 'Apple', price: 8999.00 },
    { name: 'iPhone 15 128GB', code: 'IPHONE15128', category: 'SMARTPHONE', brand: 'Apple', price: 6499.00 },
    { name: 'Galaxy S24 Ultra', code: 'GALAXYS24ULTRA', category: 'SMARTPHONE', brand: 'Samsung', price: 7999.00 },
    { name: 'Galaxy S24', code: 'GALAXYS24', category: 'SMARTPHONE', brand: 'Samsung', price: 5499.00 },
    { name: 'Motorola Edge 40', code: 'MOTOEDGE40', category: 'SMARTPHONE', brand: 'Motorola', price: 3299.00 },
    { name: 'Xiaomi 14', code: 'XIAOMI14', category: 'SMARTPHONE', brand: 'Xiaomi', price: 4599.00 },
    { name: 'MacBook Pro 14"', code: 'MACBOOKPRO14', category: 'LAPTOP', brand: 'Apple', price: 18999.00 },
    { name: 'MacBook Air M3', code: 'MACBOOKAIRM3', category: 'LAPTOP', brand: 'Apple', price: 12999.00 },
    { name: 'Dell Inspiron 15', code: 'DELLINSP15', category: 'LAPTOP', brand: 'Dell', price: 4299.00 },
    { name: 'Positivo Motion', code: 'POSMOTION', category: 'LAPTOP', brand: 'Positivo', price: 2199.00 },
    { name: 'iPad Pro 12.9"', code: 'IPADPRO12', category: 'TABLET', brand: 'Apple', price: 10999.00 },
    { name: 'Galaxy Tab S9', code: 'GALAXYTABS9', category: 'TABLET', brand: 'Samsung', price: 6499.00 },
    { name: 'Sony WH-1000XM5', code: 'SONYXM5', category: 'AUDIO', brand: 'Sony', price: 2299.00 },
    { name: 'AirPods Pro 2', code: 'AIRPODSPRO2', category: 'AUDIO', brand: 'Apple', price: 1899.00 },
    { name: 'JBL Tune 520BT', code: 'JBLTUNE520', category: 'AUDIO', brand: 'JBL', price: 299.00 },
    { name: 'LG OLED C3 55"', code: 'LGOLEDC3', category: 'TV', brand: 'LG', price: 7999.00 },
    { name: 'Samsung Neo QLED 55"', code: 'SAMSUNGQLED55', category: 'TV', brand: 'Samsung', price: 6499.00 },
    { name: 'Sony Bravia XR 65"', code: 'SONYBRAVIAXR65', category: 'TV', brand: 'Sony', price: 9999.00 },
    { name: 'PlayStation 5', code: 'PS5CONSOLE', category: 'GAMING', brand: 'Sony', price: 4499.00 },
    { name: 'Xbox Series X', code: 'XBOXSERIESX', category: 'GAMING', brand: 'Microsoft', price: 4299.00 },
    { name: 'Apple Watch Series 9', code: 'APPLEWATCHS9', category: 'WEARABLE', brand: 'Apple', price: 3999.00 },
    { name: 'Galaxy Watch 6', code: 'GALAXYWATCH6', category: 'WEARABLE', brand: 'Samsung', price: 2499.00 }
  ],

  salesmen: [
    { name: 'Pedro Santos', email: 'pedro.santos@electromart.com.br', region: 'Rio de Janeiro' },
    { name: 'Ana Costa', email: 'ana.costa@electromart.com.br', region: 'Rio de Janeiro' },
    { name: 'Rodrigo Almeida', email: 'rodrigo.almeida@electromart.com.br', region: 'Rio de Janeiro' },
    { name: 'Juliana Pereira', email: 'juliana.pereira@electromart.com.br', region: 'Rio de Janeiro' },
    { name: 'Marcelo Lima', email: 'marcelo.lima@electromart.com.br', region: 'Rio de Janeiro' }
  ],

  stores: [
    { name: 'Magazine Luiza Copacabana', city: 'Rio de Janeiro', country: 'Brazil', type: 'RETAIL' },
    { name: 'Casas Bahia Madureira', city: 'Rio de Janeiro', country: 'Brazil', type: 'RETAIL' },
    { name: 'Fast Shop Barra', city: 'Rio de Janeiro', country: 'Brazil', type: 'RETAIL' },
    { name: 'Magazine Luiza Niterói', city: 'Niterói', country: 'Brazil', type: 'RETAIL' },
    { name: 'Casas Bahia Duque de Caxias', city: 'Duque de Caxias', country: 'Brazil', type: 'RETAIL' }
  ]
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

let saleCounter = 0;

function generateSaleId() {
  saleCounter++;
  const date = new Date().toISOString().slice(0, 10).replace(/-/g, '');
  const seq = String(saleCounter).padStart(5, '0');
  return `SOAP-${date}-${seq}`;
}

function generateSale() {
  const product = randomElement(mockData.products);
  const salesman = randomElement(mockData.salesmen);
  const store = randomElement(mockData.stores);
  const quantity = randomInt(1, 5);
  const unitPrice = product.price;
  const totalAmount = (quantity * unitPrice).toFixed(2);

  return {
    saleId: generateSaleId(),
    productCode: product.code,
    productName: product.name,
    category: product.category,
    brand: product.brand,
    salesmanName: salesman.name,
    salesmanEmail: salesman.email,
    region: salesman.region,
    storeName: store.name,
    city: store.city,
    storeType: store.type,
    quantity,
    unitPrice: unitPrice.toFixed(2),
    totalAmount,
    status: randomStatus(),
    saleTimestamp: new Date().toISOString()
  };
}

module.exports = { generateSale, mockData };
