const mockData = {
  brands: ['Apple', 'Samsung', 'Sony', 'LG', 'Xiaomi', 'Microsoft', 'Dell', 'Positivo', 'Motorola', 'JBL'],
  
  categories: ['SMARTPHONE', 'LAPTOP', 'TV', 'APPLIANCE', 'AUDIO', 'GAMING', 'TABLET', 'WEARABLE'],
  
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
    { name: 'João Silva', email: 'joao.silva@electrored.com.br', phone: '+5511987654321', region: 'São Paulo' },
    { name: 'Maria Oliveira', email: 'maria.oliveira@electrored.com.br', phone: '+5511976543210', region: 'São Paulo' },
    { name: 'Pedro Santos', email: 'pedro.santos@electrored.com.br', phone: '+5521987654321', region: 'Rio de Janeiro' },
    { name: 'Ana Costa', email: 'ana.costa@electrored.com.br', phone: '+5521976543210', region: 'Rio de Janeiro' },
    { name: 'Lucas Ferreira', email: 'lucas.ferreira@electrored.com.br', phone: '+5531987654321', region: 'Minas Gerais' },
    { name: 'Juliana Souza', email: 'juliana.souza@electrored.com.br', phone: '+5541987654321', region: 'Paraná' },
    { name: 'Rafael Almeida', email: 'rafael.almeida@electrored.com.br', phone: '+5551987654321', region: 'Rio Grande do Sul' },
    { name: 'Camila Rodrigues', email: 'camila.rodrigues@electrored.com.br', phone: '+5561987654321', region: 'Distrito Federal' },
    { name: 'Bruno Carvalho', email: 'bruno.carvalho@electrored.com.br', phone: '+5571987654321', region: 'Bahia' },
    { name: 'Fernanda Lima', email: 'fernanda.lima@electrored.com.br', phone: '+5581987654321', region: 'Pernambuco' },
    { name: 'Thiago Mendes', email: 'thiago.mendes@electrored.com.br', phone: '+5585987654321', region: 'Ceará' },
    { name: 'Larissa Pereira', email: 'larissa.pereira@electrored.com.br', phone: '+5591987654321', region: 'Pará' },
    { name: 'Gustavo Ribeiro', email: 'gustavo.ribeiro@electrored.com.br', phone: '+5548987654321', region: 'Santa Catarina' },
    { name: 'Isabela Martins', email: 'isabela.martins@electrored.com.br', phone: '+5562987654321', region: 'Goiás' },
    { name: 'Felipe Gomes', email: 'felipe.gomes@electrored.com.br', phone: '+5592987654321', region: 'Amazonas' }
  ],
  
  stores: [
    { name: 'Magazine Luiza Paulista', city: 'São Paulo', country: 'Brazil', address: 'Av. Paulista 1000, Bela Vista', type: 'RETAIL' },
    { name: 'Casas Bahia Centro SP', city: 'São Paulo', country: 'Brazil', address: 'Rua 25 de Março 500', type: 'RETAIL' },
    { name: 'Fast Shop Morumbi', city: 'São Paulo', country: 'Brazil', address: 'Shopping Morumbi, Piso 2', type: 'RETAIL' },
    { name: 'Magazine Luiza Copacabana', city: 'Rio de Janeiro', country: 'Brazil', address: 'Av. Nossa Senhora de Copacabana 800', type: 'RETAIL' },
    { name: 'Casas Bahia Madureira', city: 'Rio de Janeiro', country: 'Brazil', address: 'Estrada do Portela 222', type: 'RETAIL' },
    { name: 'Fast Shop Savassi', city: 'Belo Horizonte', country: 'Brazil', address: 'Rua Pernambuco 1000, Savassi', type: 'RETAIL' },
    { name: 'Magazine Luiza Centro BH', city: 'Belo Horizonte', country: 'Brazil', address: 'Av. Afonso Pena 1500', type: 'RETAIL' },
    { name: 'Casas Bahia Curitiba', city: 'Curitiba', country: 'Brazil', address: 'Rua XV de Novembro 700', type: 'RETAIL' },
    { name: 'Magazine Luiza Porto Alegre', city: 'Porto Alegre', country: 'Brazil', address: 'Rua dos Andradas 1200', type: 'RETAIL' },
    { name: 'Fast Shop Brasília', city: 'Brasília', country: 'Brazil', address: 'Shopping Conjunto Nacional', type: 'RETAIL' },
    { name: 'Magazine Luiza Salvador', city: 'Salvador', country: 'Brazil', address: 'Av. Tancredo Neves 3000', type: 'RETAIL' },
    { name: 'Casas Bahia Recife', city: 'Recife', country: 'Brazil', address: 'Av. Conde da Boa Vista 500', type: 'RETAIL' },
    { name: 'Magazine Luiza Fortaleza', city: 'Fortaleza', country: 'Brazil', address: 'Av. Bezerra de Menezes 800', type: 'RETAIL' },
    { name: 'Fast Shop Belém', city: 'Belém', country: 'Brazil', address: 'Shopping Pátio Belém', type: 'RETAIL' },
    { name: 'Magazine Luiza Manaus', city: 'Manaus', country: 'Brazil', address: 'Av. Eduardo Ribeiro 600', type: 'RETAIL' },
    { name: 'Casas Bahia Florianópolis', city: 'Florianópolis', country: 'Brazil', address: 'Rua Felipe Schmidt 300', type: 'RETAIL' },
    { name: 'Magazine Luiza Goiânia', city: 'Goiânia', country: 'Brazil', address: 'Av. Goiás 1500', type: 'RETAIL' },
    { name: 'electrored Online', city: 'São Paulo', country: 'Brazil', address: 'Online', type: 'ONLINE' }
  ]
};


module.exports = mockData;