CREATE TABLE IF NOT EXISTS products (
  id SERIAL PRIMARY KEY,
  name VARCHAR(150) NOT NULL,
  code VARCHAR(50) UNIQUE NOT NULL,
  category VARCHAR(50) NOT NULL,
  brand VARCHAR(50) NOT NULL,
  base_price DECIMAL(10,2) NOT NULL,
  is_active BOOLEAN DEFAULT true,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS salesmen (
  id SERIAL PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  email VARCHAR(255) UNIQUE NOT NULL,
  phone VARCHAR(20),
  region VARCHAR(50),
  hire_date DATE DEFAULT CURRENT_DATE,
  is_active BOOLEAN DEFAULT true,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS stores (
  id SERIAL PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  city VARCHAR(100) NOT NULL,
  country VARCHAR(50) NOT NULL,
  address VARCHAR(255),
  store_type VARCHAR(30) NOT NULL,
  is_active BOOLEAN DEFAULT true,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sales (
  sale_id BIGSERIAL PRIMARY KEY,
  product_id INTEGER NOT NULL REFERENCES products(id),
  salesman_id INTEGER NOT NULL REFERENCES salesmen(id),
  store_id INTEGER NOT NULL REFERENCES stores(id),
  quantity INTEGER NOT NULL,
  unit_price DECIMAL(10,2) NOT NULL,
  total_amount DECIMAL(12,2) NOT NULL,
  sale_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sales_timestamp ON sales(sale_timestamp);
CREATE INDEX IF NOT EXISTS idx_sales_salesman ON sales(salesman_id);
CREATE INDEX IF NOT EXISTS idx_sales_store ON sales(store_id);

INSERT INTO products (name, code, category, brand, base_price) VALUES
  ('iPhone 15 Pro 256GB', 'IPHONE15PRO256', 'SMARTPHONE', 'Apple', 8999.00),
  ('iPhone 15 128GB', 'IPHONE15128', 'SMARTPHONE', 'Apple', 6499.00),
  ('Galaxy S24 Ultra', 'GALAXYS24ULTRA', 'SMARTPHONE', 'Samsung', 7999.00),
  ('Galaxy S24', 'GALAXYS24', 'SMARTPHONE', 'Samsung', 5499.00),
  ('Motorola Edge 40', 'MOTOEDGE40', 'SMARTPHONE', 'Motorola', 3299.00),
  ('Xiaomi 14', 'XIAOMI14', 'SMARTPHONE', 'Xiaomi', 4599.00),
  ('MacBook Pro 14"', 'MACBOOKPRO14', 'LAPTOP', 'Apple', 18999.00),
  ('MacBook Air M3', 'MACBOOKAIRM3', 'LAPTOP', 'Apple', 12999.00),
  ('Dell Inspiron 15', 'DELLINSP15', 'LAPTOP', 'Dell', 4299.00),
  ('Positivo Motion', 'POSMOTION', 'LAPTOP', 'Positivo', 2199.00),
  ('iPad Pro 12.9"', 'IPADPRO12', 'TABLET', 'Apple', 10999.00),
  ('Galaxy Tab S9', 'GALAXYTABS9', 'TABLET', 'Samsung', 6499.00),
  ('Sony WH-1000XM5', 'SONYXM5', 'AUDIO', 'Sony', 2299.00),
  ('AirPods Pro 2', 'AIRPODSPRO2', 'AUDIO', 'Apple', 1899.00),
  ('JBL Tune 520BT', 'JBLTUNE520', 'AUDIO', 'JBL', 299.00),
  ('LG OLED C3 55"', 'LGOLEDC3', 'TV', 'LG', 7999.00),
  ('Samsung Neo QLED 55"', 'SAMSUNGQLED55', 'TV', 'Samsung', 6499.00),
  ('Sony Bravia XR 65"', 'SONYBRAVIAXR65', 'TV', 'Sony', 9999.00),
  ('PlayStation 5', 'PS5CONSOLE', 'GAMING', 'Sony', 4499.00),
  ('Xbox Series X', 'XBOXSERIESX', 'GAMING', 'Microsoft', 4299.00),
  ('Apple Watch Series 9', 'APPLEWATCHS9', 'WEARABLE', 'Apple', 3999.00),
  ('Galaxy Watch 6', 'GALAXYWATCH6', 'WEARABLE', 'Samsung', 2499.00);

INSERT INTO salesmen (name, email, phone, region) VALUES
  ('João Silva', 'joao.silva@electromart.com.br', '+5511987654321', 'São Paulo'),
  ('Maria Oliveira', 'maria.oliveira@electromart.com.br', '+5511976543210', 'São Paulo'),
  ('Carlos Pereira', 'carlos.pereira@electromart.com.br', '+5511965432109', 'São Paulo'),
  ('Beatriz Santos', 'beatriz.santos@electromart.com.br', '+5511954321098', 'São Paulo'),
  ('Ricardo Alves', 'ricardo.alves@electromart.com.br', '+5511943210987', 'São Paulo');

INSERT INTO stores (name, city, country, address, store_type) VALUES
  ('Magazine Luiza Paulista', 'São Paulo', 'Brazil', 'Av. Paulista 1000, Bela Vista', 'RETAIL'),
  ('Casas Bahia Centro SP', 'São Paulo', 'Brazil', 'Rua 25 de Março 500', 'RETAIL'),
  ('Fast Shop Morumbi', 'São Paulo', 'Brazil', 'Shopping Morumbi, Piso 2', 'RETAIL'),
  ('Magazine Luiza Tatuapé', 'São Paulo', 'Brazil', 'Shopping Tatuapé, Loja 201', 'RETAIL'),
  ('Casas Bahia Mooca', 'São Paulo', 'Brazil', 'Rua da Mooca 1200', 'RETAIL');
