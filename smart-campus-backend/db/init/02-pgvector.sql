-- AI 探索助手向量表（M1）
-- 由 docker-compose 挂载到 postgres 容器的 /docker-entrypoint-initdb.d/（与 01-extensions.sql 同目录）
-- 仅在数据卷首次初始化时执行一次（pg_data 卷为空时）

-- pgvector 扩展（由 Dockerfile.db 编译安装；此处幂等启用）
CREATE EXTENSION IF NOT EXISTS vector;

-- POI 语义向量表
-- 设计要点（方案 §6.1）：
--   * 不交给 JPA 管理 —— vector 列 Hibernate 不识别，由独立 SQL + JdbcTemplate 维护；
--   * poi_id 唯一约束，支撑「POI 新增/更新时 upsert 刷新向量」的增量管道；
--   * embedding 维度 1024，对齐 DashScope text-embedding-v4（更换模型须保证维度仍为 1024，否则需同步修改本表 vector(N)）。
CREATE TABLE IF NOT EXISTS poi_embedding (
    id        BIGSERIAL PRIMARY KEY,
    poi_id    BIGINT      NOT NULL UNIQUE,
    content   TEXT        NOT NULL,
    embedding vector(1024) NOT NULL,
    updated_at TIMESTAMP  NOT NULL DEFAULT now()
);

-- HNSW 余弦近邻索引：方案 §6.2 候选集内语义 Top-K 检索的核心索引
-- vector_cosine_ops 对应余弦距离（<=>），与 embedding 相似度语义一致
CREATE INDEX IF NOT EXISTS idx_poi_embedding_hnsw
    ON poi_embedding USING hnsw (embedding vector_cosine_ops);
