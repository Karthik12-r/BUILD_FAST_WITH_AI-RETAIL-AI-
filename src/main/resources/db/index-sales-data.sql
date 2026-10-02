CREATE INDEX idx_sales_store_product_date_id
    ON sales_data (store_id, product_id, date, id);

CREATE INDEX idx_sales_store_date
    ON sales_data (store_id, date);