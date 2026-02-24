-- Trend data table
CREATE TABLE IF NOT EXISTS trend_data (
    id              BIGSERIAL PRIMARY KEY,
    metric_name     VARCHAR(255) NOT NULL,
    metric_value    NUMERIC(19, 4) NOT NULL,
    dimension       VARCHAR(255),
    granularity     VARCHAR(20) NOT NULL,
    timestamp       TIMESTAMPTZ NOT NULL,
    source          VARCHAR(100),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes for common query patterns
CREATE INDEX idx_trend_metric_ts ON trend_data (metric_name, timestamp);
CREATE INDEX idx_trend_dimension_ts ON trend_data (dimension, timestamp);
CREATE INDEX idx_trend_granularity ON trend_data (granularity);
CREATE INDEX idx_trend_created_at ON trend_data (created_at);

-- ShedLock table for distributed job locking
CREATE TABLE IF NOT EXISTS shedlock (
    name       VARCHAR(64)  NOT NULL,
    lock_until TIMESTAMPTZ  NOT NULL,
    locked_at  TIMESTAMPTZ  NOT NULL,
    locked_by  VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);

-- Comments
COMMENT ON TABLE trend_data IS 'Stores time-series trend data at various granularities';
COMMENT ON TABLE shedlock IS 'Distributed lock table used by ShedLock for maintenance jobs';
