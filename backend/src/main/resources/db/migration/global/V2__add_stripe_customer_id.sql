ALTER TABLE tenant_registry ADD COLUMN stripe_customer_id VARCHAR(255);

CREATE UNIQUE INDEX uq_tenant_registry_stripe_customer_id
    ON tenant_registry (stripe_customer_id) WHERE stripe_customer_id IS NOT NULL;