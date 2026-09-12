CREATE TABLE processed_webhook_events (
    stripe_event_id  VARCHAR(255) PRIMARY KEY,
    event_type       VARCHAR(100) NOT NULL,
    processed_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
