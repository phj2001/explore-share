-- 邮箱唯一索引：email 承担「找回密码 + 邮箱登录」的身份凭证职责，必须唯一
--（NULL 除外——PG 部分唯一索引天然豁免 NULL，兼容注册时邮箱选填）。
-- 背景：此索引此前仅存在于个别手工建过它的库，从未进过迁移文件（迁移目录里从无 email DDL），
-- 缺索引的环境只剩应用层 existsByEmail 兜底，并发竞态或大小写变体即可注册出同邮箱多账号，
-- 届时 findByEmail 将抛 IncorrectResultSizeDataAccessException（找回密码/邮箱登录直接 500）。
-- 本迁移用 IF NOT EXISTS 让所有环境（已有索引的本地 / 缺索引的线上）收敛到一致状态。
-- 表存在性防御：与 V2 同因——Flyway 先于 Hibernate 执行，全新空库时 users 尚未建表。
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = 'public' AND table_name = 'users') THEN
        -- ① 空串邮箱视同未绑定（应用层注册已归一化，此为存量防御）：
        --    空串在部分唯一索引里会占一个"已占用值"，第二个空串用户将撞索引 500
        UPDATE users SET email = NULL
        WHERE email IS NOT NULL AND btrim(email) = '';

        -- ② 清洗存量重复：按「归一化后相同」判定（btrim+lower，与 Java 侧 normalizeEmail 对齐），
        --    保留先注册者，后注册者置 NULL（回到未绑定状态，可重新补绑）。
        --    必须在 ③ 归一化之前执行：否则大小写变体（Abc@ 与 abc@）lower 后才撞出重复，
        --    在已有唯一索引的环境（本地）会在 ③ 改写时违反索引报错
        UPDATE users u SET email = NULL
        WHERE u.email IS NOT NULL
          AND EXISTS (SELECT 1 FROM users v
                      WHERE v.email IS NOT NULL
                        AND v.id < u.id
                        AND lower(btrim(v.email)) = lower(btrim(u.email)));

        -- ③ 归一化存量值：去空白 + 小写，与应用层入口归一化对齐；
        --    否则历史大小写变体仍会绕过应用层的精确匹配校验
        UPDATE users SET email = lower(btrim(email))
        WHERE email IS NOT NULL AND email <> lower(btrim(email));

        -- ④ 建索引（幂等：已手工建过索引的环境直接跳过）
        CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email
            ON users (email) WHERE email IS NOT NULL;
    END IF;
END $$;
