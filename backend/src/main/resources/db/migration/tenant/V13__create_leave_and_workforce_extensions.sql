-- Migration V13: Create Leave and Workforce Extensions
-- Applied per tenant schema

-- 1. Leave Requests Table
CREATE TABLE leave_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    employee_id UUID REFERENCES employees(id) ON DELETE SET NULL,
    employee_name VARCHAR(255) NOT NULL,
    employee_email VARCHAR(255) NOT NULL,
    leave_type VARCHAR(50) NOT NULL DEFAULT 'ANNUAL'
        CHECK (leave_type IN ('ANNUAL', 'SICK', 'CASUAL', 'UNPAID', 'PARENTAL', 'BEREAVEMENT', 'OTHER')),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    days_count NUMERIC(4,1) NOT NULL DEFAULT 1.0,
    reason TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('DRAFT', 'PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    reviewer_id UUID REFERENCES users(id) ON DELETE SET NULL,
    reviewer_name VARCHAR(255),
    review_note TEXT,
    reviewed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_leave_dates CHECK (end_date >= start_date),
    CONSTRAINT chk_days_count CHECK (days_count > 0)
);

CREATE INDEX idx_leave_requests_user ON leave_requests (user_id);
CREATE INDEX idx_leave_requests_employee ON leave_requests (employee_id);
CREATE INDEX idx_leave_requests_status ON leave_requests (status);
CREATE INDEX idx_leave_requests_dates ON leave_requests (start_date, end_date);
CREATE INDEX idx_leave_requests_created ON leave_requests (created_at DESC);

-- 2. Leave Balances Table
CREATE TABLE leave_balances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    year INT NOT NULL,
    leave_type VARCHAR(50) NOT NULL DEFAULT 'ANNUAL'
        CHECK (leave_type IN ('ANNUAL', 'SICK', 'CASUAL', 'UNPAID', 'PARENTAL', 'BEREAVEMENT', 'OTHER')),
    total_days NUMERIC(4,1) NOT NULL DEFAULT 20.0,
    used_days NUMERIC(4,1) NOT NULL DEFAULT 0.0,
    pending_days NUMERIC(4,1) NOT NULL DEFAULT 0.0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_leave_balance_user_year_type UNIQUE (user_id, year, leave_type)
);

CREATE INDEX idx_leave_balances_user_year ON leave_balances (user_id, year);
