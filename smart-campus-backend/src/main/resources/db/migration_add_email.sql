-- 为 users 表添加 email 列（兼容已有数据，允许为空）
ALTER TABLE users ADD COLUMN IF NOT EXISTS email VARCHAR(255);

-- 唯一索引（允许多个 NULL，但不允许重复非空邮箱）
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email ON users (email) WHERE email IS NOT NULL;
