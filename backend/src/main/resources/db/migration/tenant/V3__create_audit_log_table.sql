CREATE TABLE audit_log (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id    UUID,
    actor_role  VARCHAR(20),
    action      VARCHAR(100) NOT NULL,
    outcome     VARCHAR(10) NOT NULL CHECK (outcome IN ('SUCCESS', 'FAILURE')),
    details     TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_log_actor_id ON audit_log (actor_id);
CREATE INDEX idx_audit_log_created_at ON audit_log (created_at);