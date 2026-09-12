ALTER TABLE customers
    ADD COLUMN stripe_customer_id VARCHAR(255),
    ADD COLUMN billing_email VARCHAR(255),
    ADD COLUMN billing_name VARCHAR(255),
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

CREATE UNIQUE INDEX uq_customers_stripe_customer_id ON customers (stripe_customer_id);
