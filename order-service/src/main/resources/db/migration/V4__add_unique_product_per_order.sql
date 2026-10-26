ALTER TABLE order_item
ADD CONSTRAINT order_item_order_id_product_id_key
UNIQUE (order_id, product_id);

-- This index is redundant now that an index on (order_id, product_id) exists
DROP INDEX order_item_order_id_idx;
