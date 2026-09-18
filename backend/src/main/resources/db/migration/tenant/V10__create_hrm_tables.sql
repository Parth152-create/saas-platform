CREATE TABLE departments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    lead VARCHAR(100),
    budget_utilization NUMERIC(5,2) DEFAULT 0.0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE employees (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    department VARCHAR(100) NOT NULL,
    position VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'ON_LEAVE', 'INACTIVE', 'PROBATION', 'TERMINATED')),
    hire_date DATE NOT NULL DEFAULT CURRENT_DATE,
    phone VARCHAR(50),
    work_model VARCHAR(20) NOT NULL DEFAULT 'Hybrid'
        CHECK (work_model IN ('Remote', 'Hybrid', 'On-Site')),
    location VARCHAR(100),
    manager VARCHAR(100),
    avatar_color VARCHAR(100) DEFAULT 'bg-zinc-800 text-zinc-100',
    attendance_rate NUMERIC(5,2) DEFAULT 100.0,
    billable_hours INT DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_employees_department ON employees (department);
CREATE INDEX idx_employees_status ON employees (status);

-- Seed initial departments
INSERT INTO departments (name, lead, budget_utilization) VALUES
('Engineering', 'Alexander Chen', 78.0),
('Product & Design', 'Elena Rostova', 64.0),
('Operations', 'Marcus Sterling', 82.0),
('Sales & Accounts', 'David Okafor', 91.0),
('Human Resources', 'Emily Thornton', 45.0)
ON CONFLICT DO NOTHING;

-- Seed initial tenant employees
INSERT INTO employees (employee_id, name, email, department, position, status, hire_date, phone, work_model, location, manager, avatar_color, attendance_rate, billable_hours) VALUES
('EMP-0491', 'Alexander Chen', 'alexander.chen@workspace.io', 'Engineering', 'Lead Systems Architect', 'ACTIVE', '2023-04-12', '+1 (555) 234-5678', 'Hybrid', 'San Francisco, CA', 'Sarah Jenkins', 'bg-zinc-800 text-zinc-100', 98.5, 164),
('EMP-0512', 'Elena Rostova', 'elena.rostova@workspace.io', 'Product & Design', 'Principal UI/UX Designer', 'ACTIVE', '2023-08-15', '+1 (555) 345-6789', 'Remote', 'New York, NY', 'Michael Vance', 'bg-zinc-700 text-zinc-100', 99.1, 152),
('EMP-0628', 'Marcus Sterling', 'marcus.sterling@workspace.io', 'Operations', 'Senior Operations Lead', 'ON_LEAVE', '2024-01-10', '+1 (555) 456-7890', 'On-Site', 'Austin, TX', 'Sarah Jenkins', 'bg-zinc-900 text-zinc-100', 92.4, 130),
('EMP-0734', 'Sophia Martinez', 'sophia.martinez@workspace.io', 'Engineering', 'Senior Backend Engineer', 'ACTIVE', '2024-03-22', '+1 (555) 567-8901', 'Remote', 'Chicago, IL', 'Alexander Chen', 'bg-zinc-800 text-zinc-100', 97.8, 168),
('EMP-0845', 'David Okafor', 'david.okafor@workspace.io', 'Sales & Accounts', 'Enterprise Account Executive', 'ACTIVE', '2024-06-01', '+1 (555) 678-9012', 'Hybrid', 'Seattle, WA', 'Rachel Adams', 'bg-zinc-700 text-zinc-100', 96.2, 145),
('EMP-0919', 'Emily Thornton', 'emily.thornton@workspace.io', 'Human Resources', 'People Operations Manager', 'PROBATION', '2026-08-01', '+1 (555) 789-0123', 'Hybrid', 'Denver, CO', 'Sarah Jenkins', 'bg-zinc-900 text-zinc-100', 100.0, 120)
ON CONFLICT DO NOTHING;
