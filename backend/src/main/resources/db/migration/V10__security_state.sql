-- Credential epoch invalidates outstanding access tokens after password changes.
ALTER TABLE users ADD COLUMN security_version BIGINT NOT NULL DEFAULT 0;
-- A singleton row provides a database lock and durable one-time bootstrap marker.
CREATE TABLE security_bootstrap (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    completed_at TIMESTAMPTZ,
    admin_user_id BIGINT REFERENCES users(id)
);
INSERT INTO security_bootstrap(id) VALUES (1);
