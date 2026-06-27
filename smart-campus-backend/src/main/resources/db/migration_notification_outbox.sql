-- ============================================================
-- 升级项④ 通知 Outbox：notification_event_outbox 建表
-- 注：本项目 ddl-auto=update 会自动建表，本脚本为手动执行备份
--      （与 migration_admin_audit_enhance.sql 等现有脚本风格一致）
-- ============================================================

CREATE SEQUENCE IF NOT EXISTS notification_event_outbox_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS notification_event_outbox (
    id              BIGINT                   NOT NULL DEFAULT nextval('notification_event_outbox_id_seq'),
    aggregate_type  VARCHAR(50),
    aggregate_id    BIGINT,
    notification_id BIGINT,
    event_type      VARCHAR(30),
    routing_key     VARCHAR(100),
    payload_json    TEXT                     NOT NULL,
    delivery_status INT                      NOT NULL,
    attempt_count   INT                      NOT NULL DEFAULT 0,
    last_error      VARCHAR(500),
    next_retry_at   TIMESTAMP WITH TIME ZONE,
    published_at    TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_notification_event_outbox PRIMARY KEY (id)
);

-- Dispatcher 扫描索引：按状态 + 重试时间
CREATE INDEX IF NOT EXISTS idx_outbox_status_retry   ON notification_event_outbox (delivery_status, next_retry_at);
-- 批量重试索引：按状态 + 更新时间
CREATE INDEX IF NOT EXISTS idx_outbox_status_updated ON notification_event_outbox (delivery_status, updated_at);
