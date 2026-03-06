-- Products table
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

-- Salesmen table
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

-- Stores table
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

-- Sales table
CREATE TABLE IF NOT EXISTS sales (
  sale_id BIGSERIAL PRIMARY KEY,
  product_id INTEGER NOT NULL REFERENCES products(id),
  salesman_id INTEGER NOT NULL REFERENCES salesmen(id),
  store_id INTEGER NOT NULL REFERENCES stores(id),
  quantity INTEGER NOT NULL,
  unit_price DECIMAL(10,2) NOT NULL,
  total_amount DECIMAL(12,2) NOT NULL,
  status VARCHAR(20) DEFAULT 'PENDING',
  sale_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_sales_timestamp ON sales(sale_timestamp);
CREATE INDEX IF NOT EXISTS idx_sales_status ON sales(status);
CREATE INDEX IF NOT EXISTS idx_sales_salesman ON sales(salesman_id);
CREATE INDEX IF NOT EXISTS idx_sales_store ON sales(store_id);

-- Seed products
INSERT INTO products (name, code, category, brand, base_price) VALUES
  ('iPhone 15 Pro 256GB',  'IPHONE15PRO256',  'SMARTPHONE', 'Apple',     8999.00),
  ('iPhone 15 128GB',      'IPHONE15128',      'SMARTPHONE', 'Apple',     6499.00),
  ('Galaxy S24 Ultra',     'GALAXYS24ULTRA',   'SMARTPHONE', 'Samsung',   7999.00),
  ('Galaxy S24',           'GALAXYS24',        'SMARTPHONE', 'Samsung',   5499.00),
  ('Motorola Edge 40',     'MOTOEDGE40',       'SMARTPHONE', 'Motorola',  3299.00),
  ('Xiaomi 14',            'XIAOMI14',         'SMARTPHONE', 'Xiaomi',    4599.00),
  ('MacBook Pro 14"',      'MACBOOKPRO14',     'LAPTOP',     'Apple',    18999.00),
  ('MacBook Air M3',       'MACBOOKAIRM3',     'LAPTOP',     'Apple',    12999.00),
  ('Dell Inspiron 15',     'DELLINSP15',       'LAPTOP',     'Dell',      4299.00),
  ('Positivo Motion',      'POSMOTION',        'LAPTOP',     'Positivo',  2199.00),
  ('iPad Pro 12.9"',       'IPADPRO12',        'TABLET',     'Apple',    10999.00),
  ('Galaxy Tab S9',        'GALAXYTABS9',      'TABLET',     'Samsung',   6499.00),
  ('Sony WH-1000XM5',      'SONYXM5',          'AUDIO',      'Sony',      2299.00),
  ('AirPods Pro 2',        'AIRPODSPRO2',      'AUDIO',      'Apple',     1899.00),
  ('JBL Tune 520BT',       'JBLTUNE520',       'AUDIO',      'JBL',        299.00),
  ('LG OLED C3 55"',       'LGOLEDC3',         'TV',         'LG',        7999.00),
  ('Samsung Neo QLED 55"', 'SAMSUNGQLED55',    'TV',         'Samsung',   6499.00),
  ('Sony Bravia XR 65"',   'SONYBRAVIAXR65',   'TV',         'Sony',      9999.00),
  ('PlayStation 5',        'PS5CONSOLE',       'GAMING',     'Sony',      4499.00),
  ('Xbox Series X',        'XBOXSERIESX',      'GAMING',     'Microsoft', 4299.00),
  ('Apple Watch Series 9', 'APPLEWATCHS9',     'WEARABLE',   'Apple',     3999.00),
  ('Galaxy Watch 6',       'GALAXYWATCH6',     'WEARABLE',   'Samsung',   2499.00);

-- Seed salesmen
INSERT INTO salesmen (name, email, phone, region) VALUES
  ('João Silva',       'joao.silva@electrored.com.br',       '+5511987654321', 'São Paulo'),
  ('Maria Oliveira',   'maria.oliveira@electrored.com.br',   '+5511976543210', 'São Paulo'),
  ('Pedro Santos',     'pedro.santos@electrored.com.br',     '+5521987654321', 'Rio de Janeiro'),
  ('Ana Costa',        'ana.costa@electrored.com.br',        '+5521976543210', 'Rio de Janeiro'),
  ('Lucas Ferreira',   'lucas.ferreira@electrored.com.br',   '+5531987654321', 'Minas Gerais'),
  ('Juliana Souza',    'juliana.souza@electrored.com.br',    '+5541987654321', 'Paraná'),
  ('Rafael Almeida',   'rafael.almeida@electrored.com.br',   '+5551987654321', 'Rio Grande do Sul'),
  ('Camila Rodrigues', 'camila.rodrigues@electrored.com.br', '+5561987654321', 'Distrito Federal'),
  ('Bruno Carvalho',   'bruno.carvalho@electrored.com.br',   '+5571987654321', 'Bahia'),
  ('Fernanda Lima',    'fernanda.lima@electrored.com.br',    '+5581987654321', 'Pernambuco'),
  ('Thiago Mendes',    'thiago.mendes@electrored.com.br',    '+5585987654321', 'Ceará'),
  ('Larissa Pereira',  'larissa.pereira@electrored.com.br',  '+5591987654321', 'Pará'),
  ('Gustavo Ribeiro',  'gustavo.ribeiro@electrored.com.br',  '+5548987654321', 'Santa Catarina'),
  ('Isabela Martins',  'isabela.martins@electrored.com.br',  '+5562987654321', 'Goiás'),
  ('Felipe Gomes',     'felipe.gomes@electrored.com.br',     '+5592987654321', 'Amazonas');

-- Seed stores
INSERT INTO stores (name, city, country, address, store_type) VALUES
  ('Magazine Luiza Paulista',      'São Paulo',       'Brazil', 'Av. Paulista 1000, Bela Vista',             'RETAIL'),
  ('Casas Bahia Centro SP',        'São Paulo',       'Brazil', 'Rua 25 de Março 500',                       'RETAIL'),
  ('Fast Shop Morumbi',            'São Paulo',       'Brazil', 'Shopping Morumbi, Piso 2',                  'RETAIL'),
  ('Magazine Luiza Copacabana',    'Rio de Janeiro',  'Brazil', 'Av. Nossa Senhora de Copacabana 800',       'RETAIL'),
  ('Casas Bahia Madureira',        'Rio de Janeiro',  'Brazil', 'Estrada do Portela 222',                    'RETAIL'),
  ('Fast Shop Savassi',            'Belo Horizonte',  'Brazil', 'Rua Pernambuco 1000, Savassi',              'RETAIL'),
  ('Magazine Luiza Centro BH',     'Belo Horizonte',  'Brazil', 'Av. Afonso Pena 1500',                     'RETAIL'),
  ('Casas Bahia Curitiba',         'Curitiba',        'Brazil', 'Rua XV de Novembro 700',                    'RETAIL'),
  ('Magazine Luiza Porto Alegre',  'Porto Alegre',    'Brazil', 'Rua dos Andradas 1200',                     'RETAIL'),
  ('Fast Shop Brasília',           'Brasília',        'Brazil', 'Shopping Conjunto Nacional',                'RETAIL'),
  ('Magazine Luiza Salvador',      'Salvador',        'Brazil', 'Av. Tancredo Neves 3000',                   'RETAIL'),
  ('Casas Bahia Recife',           'Recife',          'Brazil', 'Av. Conde da Boa Vista 500',                'RETAIL'),
  ('Magazine Luiza Fortaleza',     'Fortaleza',       'Brazil', 'Av. Bezerra de Menezes 800',                'RETAIL'),
  ('Fast Shop Belém',              'Belém',           'Brazil', 'Shopping Pátio Belém',                      'RETAIL'),
  ('Magazine Luiza Manaus',        'Manaus',          'Brazil', 'Av. Eduardo Ribeiro 600',                   'RETAIL'),
  ('Casas Bahia Florianópolis',    'Florianópolis',   'Brazil', 'Rua Felipe Schmidt 300',                    'RETAIL'),
  ('Magazine Luiza Goiânia',       'Goiânia',         'Brazil', 'Av. Goiás 1500',                            'RETAIL'),
  ('electrored Online',            'São Paulo',       'Brazil', 'Online',                                    'ONLINE');
