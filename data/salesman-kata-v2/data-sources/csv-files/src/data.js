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
    { name: 'Lucas Ferreira', email: 'lucas.ferreira@electromart.com.br', region: 'Minas Gerais' },
    { name: 'Carla Mendes', email: 'carla.mendes@electromart.com.br', region: 'Minas Gerais' },
    { name: 'Roberto Costa', email: 'roberto.costa@electromart.com.br', region: 'Minas Gerais' },
    { name: 'Mariana Silva', email: 'mariana.silva@electromart.com.br', region: 'Minas Gerais' },
    { name: 'Eduardo Oliveira', email: 'eduardo.oliveira@electromart.com.br', region: 'Minas Gerais' }
  ],

  stores: [
    { name: 'Fast Shop Savassi', city: 'Belo Horizonte', type: 'RETAIL' },
    { name: 'Magazine Luiza Centro BH', city: 'Belo Horizonte', type: 'RETAIL' },
    { name: 'Casas Bahia Contagem', city: 'Contagem', type: 'RETAIL' },
    { name: 'Magazine Luiza Betim', city: 'Betim', type: 'RETAIL' },
    { name: 'Fast Shop Uberlândia', city: 'Uberlândia', type: 'RETAIL' }
  ]
};

module.exports = mockData;
