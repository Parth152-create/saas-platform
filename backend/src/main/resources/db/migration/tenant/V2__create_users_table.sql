ALTER TABLE users RENAME TO legacy_users;

CREATE TABLE users (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email          VARCHAR(255) NOT NULL,
    password_hash  VARCHAR(255),
    auth_provider  VARCHAR(20) NOT NULL DEFAULT 'LOCAL'
                   CHECK (auth_provider IN ('LOCAL', 'GOOGLE')),
    google_subject VARCHAR(255),
    role           VARCHAR(20) NOT NULL
                   CHECK (role IN ('SUPER_ADMIN','ADMIN','MANAGER','USER')),
    status         VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                   CHECK (status IN ('ACTIVE','DISABLED','INVITED')),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_users_email ON users (lower(email));
CREATE UNIQUE INDEX uq_users_google_subject ON users (google_subject) WHERE google_subject IS NOT NULL;