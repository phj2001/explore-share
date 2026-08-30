-- 个人主页可见性：0 公开（默认）/ 1 仅关注者 / 2 仅自己
-- 必须带 DEFAULT 0：users 表有存量用户时，无默认值的 NOT NULL 列会被 PG 拒绝
-- （2026-08-29 事故：Hibernate update 生成的 DDL 无 DEFAULT，报 "contains null values" 被静默吞掉）。
-- 表存在性防御：Flyway 先于 Hibernate 执行，全新空库时 users 尚未建表，裸 ALTER 会启动失败。
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = 'public' AND table_name = 'users') THEN
        ALTER TABLE users ADD COLUMN IF NOT EXISTS profile_visibility SMALLINT NOT NULL DEFAULT 0;
    END IF;
END $$;
