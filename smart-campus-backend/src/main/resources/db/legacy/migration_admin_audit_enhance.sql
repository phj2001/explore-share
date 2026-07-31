-- ============================================================
-- 升级项② AOP 操作审计：admin_operation_logs 实体增强
-- 新增 6 列，均允许 NULL（向后兼容，历史数据不受影响）
-- 注：本项目 ddl-auto=update 会自动加列，本脚本为手动执行的备份
--      （与 migration_achievements.sql 等现有脚本风格一致）
-- ============================================================

ALTER TABLE admin_operation_logs ADD COLUMN request_method   VARCHAR(10);
ALTER TABLE admin_operation_logs ADD COLUMN request_uri      VARCHAR(200);
ALTER TABLE admin_operation_logs ADD COLUMN ip_address       VARCHAR(50);
ALTER TABLE admin_operation_logs ADD COLUMN operation_status INT;       -- 1=成功, 0=失败
ALTER TABLE admin_operation_logs ADD COLUMN trace_id         VARCHAR(40);
ALTER TABLE admin_operation_logs ADD COLUMN duration_ms      BIGINT;
