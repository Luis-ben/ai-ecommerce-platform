CREATE TABLE IF NOT EXISTS schema_migrations (
    version VARCHAR(80) PRIMARY KEY,
    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS agent_runs (
    id VARCHAR(80) PRIMARY KEY,
    store_id VARCHAR(80) NOT NULL,
    actor_id VARCHAR(80),
    channel VARCHAR(30) NOT NULL,
    mode VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    request_hash VARCHAR(128) NOT NULL,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP,
    error_code VARCHAR(80)
);

CREATE TABLE IF NOT EXISTS agent_tool_calls (
    id VARCHAR(80) PRIMARY KEY,
    run_id VARCHAR(80) NOT NULL,
    tool_name VARCHAR(120) NOT NULL,
    access_mode VARCHAR(20) NOT NULL,
    confirmation_required BOOLEAN NOT NULL DEFAULT FALSE,
    confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    input_summary TEXT NOT NULL,
    output_summary TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    duration_ms INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS mcp_invocations (
    id VARCHAR(80) PRIMARY KEY,
    store_id VARCHAR(80) NOT NULL,
    actor_id VARCHAR(80),
    server_name VARCHAR(120) NOT NULL,
    tool_name VARCHAR(120) NOT NULL,
    input_summary TEXT NOT NULL,
    output_summary TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    duration_ms INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS commerce_events (
    id VARCHAR(80) PRIMARY KEY,
    store_id VARCHAR(80) NOT NULL,
    actor_id VARCHAR(80),
    event_type VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id VARCHAR(100) NOT NULL,
    metadata_json TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_agent_runs_store_started ON agent_runs(store_id, started_at DESC);
CREATE INDEX IF NOT EXISTS idx_agent_tool_calls_run ON agent_tool_calls(run_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_mcp_invocations_store_created ON mcp_invocations(store_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_commerce_events_store_type_created ON commerce_events(store_id, event_type, created_at DESC);

INSERT INTO schema_migrations(version) VALUES ('V1__production_agent_audit') ON CONFLICT (version) DO NOTHING;
