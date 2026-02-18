CREATE TABLE IF NOT EXISTS sales (
    sale_id VARCHAR(100) PRIMARY KEY,
    timestamp BIGINT NOT NULL,
    salesman_id VARCHAR(100) NOT NULL,
    salesman_name VARCHAR(255) NOT NULL,
    customer_id VARCHAR(100) NOT NULL,
    product_id VARCHAR(100) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    city VARCHAR(100) NOT NULL,
    country VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sales_timestamp ON sales(timestamp);
CREATE INDEX idx_sales_salesman ON sales(salesman_id);
CREATE INDEX idx_sales_city ON sales(city);
CREATE INDEX idx_sales_country ON sales(country);

ALTER TABLE sales REPLICA IDENTITY FULL;

INSERT INTO sales (sale_id, timestamp, salesman_id, salesman_name, customer_id, product_id, product_name, quantity, unit_price, total_amount, city, country)
VALUES
    ('sale-001', EXTRACT(EPOCH FROM NOW()) * 1000, 'sm-001', 'John Doe', 'cust-001', 'prod-001', 'Laptop', 2, 1200.00, 2400.00, 'New York', 'USA'),
    ('sale-002', EXTRACT(EPOCH FROM NOW()) * 1000, 'sm-002', 'Jane Smith', 'cust-002', 'prod-002', 'Mouse', 5, 25.00, 125.00, 'San Francisco', 'USA'),
    ('sale-003', EXTRACT(EPOCH FROM NOW()) * 1000, 'sm-001', 'John Doe', 'cust-003', 'prod-003', 'Keyboard', 3, 75.00, 225.00, 'Los Angeles', 'USA'),
    ('sale-004', EXTRACT(EPOCH FROM NOW()) * 1000, 'sm-003', 'Bob Johnson', 'cust-004', 'prod-001', 'Laptop', 1, 1200.00, 1200.00, 'Chicago', 'USA'),
    ('sale-005', EXTRACT(EPOCH FROM NOW()) * 1000, 'sm-002', 'Jane Smith', 'cust-005', 'prod-004', 'Monitor', 2, 300.00, 600.00, 'Boston', 'USA');
