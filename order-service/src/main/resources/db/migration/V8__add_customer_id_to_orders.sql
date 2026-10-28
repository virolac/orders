DELETE FROM order_item;
DELETE FROM orders;

ALTER TABLE orders ADD COLUMN
    customer_id VARCHAR(36) NOT NULL;

CREATE INDEX orders_customer_id_created_at_idx ON orders(customer_id, created_at);
