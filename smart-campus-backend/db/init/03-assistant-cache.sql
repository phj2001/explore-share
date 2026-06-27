-- ============================================================
-- AI 探索助手 · 语义缓存表（M4，方案 §8）
-- 由 db/init 在数据卷首次初始化时执行；既有库需手动执行本脚本。
-- 依赖 02-pgvector.sql 已 CREATE EXTENSION vector。
-- ============================================================

CREATE TABLE IF NOT EXISTS assistant_cache (
    id          BIGSERIAL PRIMARY KEY,
    loc_bucket  VARCHAR(32)  NOT NULL,         -- 经纬度粗粒度分桶（约 100m），缓存按区域隔离
    query       TEXT         NOT NULL,         -- 原始问题（排查用）
    embedding   vector(1024) NOT NULL,         -- 问题向量，与 poi_embedding 同维同模型
    response    TEXT         NOT NULL,         -- 缓存的助手回答
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- 向量近邻索引（余弦）
CREATE INDEX IF NOT EXISTS idx_assistant_cache_hnsw
    ON assistant_cache USING hnsw (embedding vector_cosine_ops);

-- 分桶 + 时间过滤索引
CREATE INDEX IF NOT EXISTS idx_assistant_cache_bucket_time
    ON assistant_cache (loc_bucket, created_at);
