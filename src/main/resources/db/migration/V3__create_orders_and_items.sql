CREATE SEQUENCE orders_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'PAID', 'FAILED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE SEQUENCE order_item_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE order_item (
    id BIGINT PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id),
    product_id BIGINT NOT NULL REFERENCES product(id),
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(7, 2) NOT NULL
);

CREATE INDEX order_item_order_id_idx ON order_item(order_id);
CREATE INDEX order_item_product_id_idx ON order_item(product_id);
