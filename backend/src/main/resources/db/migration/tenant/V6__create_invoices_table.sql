ALTER TABLE invoices
    ADD COLUMN subscription_id BIGINT REFERENCES subscriptions (id),
    ADD COLUMN stripe_invoice_id VARCHAR(255),
    ADD COLUMN amount_due_cents BIGINT,
    ADD COLUMN amount_paid_cents BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN currency VARCHAR(3),
    ADD COLUMN hosted_invoice_url TEXT,
    ADD COLUMN invoice_pdf_url TEXT,
    ADD COLUMN paid_at TIMESTAMPTZ;

CREATE UNIQUE INDEX uq_invoices_stripe_invoice_id ON invoices (stripe_invoice_id);
CREATE INDEX idx_invoices_customer_id ON invoices (customer_id);
