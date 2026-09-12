ALTER TABLE subscriptions
    ADD COLUMN stripe_subscription_id VARCHAR(255),
    ADD COLUMN stripe_price_id VARCHAR(255),
    ADD COLUMN plan_tier VARCHAR(20),
    ADD COLUMN current_period_start TIMESTAMPTZ,
    ADD COLUMN current_period_end TIMESTAMPTZ,
    ADD COLUMN cancel_at_period_end BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

CREATE UNIQUE INDEX uq_subscriptions_stripe_subscription_id ON subscriptions (stripe_subscription_id);
CREATE INDEX idx_subscriptions_customer_id ON subscriptions (customer_id);
