-- Capability-scoped development garage. Tokens are hashed, never stored raw.
-- Snapshots intentionally survive later catalog edits; no catalog foreign keys.
CREATE TABLE garage_snapshots (
    id UUID PRIMARY KEY,
    owner_hash VARCHAR(64) NOT NULL,
    kind VARCHAR(10) NOT NULL CHECK (kind IN ('VEHICLE', 'RACE')),
    label VARCHAR(200) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX garage_snapshots_owner_created ON garage_snapshots(owner_hash, created_at DESC);
