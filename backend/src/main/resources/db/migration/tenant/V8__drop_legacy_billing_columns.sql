ALTER TABLE customers
    DROP COLUMN name,
    DROP COLUMN email;

ALTER TABLE subscriptions
    DROP COLUMN plan;

ALTER TABLE invoices
    DROP COLUMN amount;