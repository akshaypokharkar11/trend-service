-- Composite index for the most common query pattern
CREATE INDEX idx_trend_metric_gran_ts ON trend_data (metric_name, granularity, timestamp);

-- Composite index for dimension-filtered queries
CREATE INDEX idx_trend_metric_dim_gran_ts ON trend_data (metric_name, dimension, granularity, timestamp);
