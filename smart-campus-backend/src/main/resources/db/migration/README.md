# 数据库迁移策略（Flyway）

## 现状与分工

| 位置 | 作用 |
|---|---|
| `db/migration/`（本目录） | **唯一的增量结构变更入口**，Flyway 版本化脚本 `V{n}__描述.sql` |
| `db/legacy/` | 历史手工脚本归档（Flyway 引入前），仅作审计留档，已生效于既有库，勿再执行/勿再迁移 |
| `smart-campus-backend/db/init/` | docker 首次初始化：扩展（PostGIS/pgvector）+ assistant_cache 表 |

## 规则

1. **prod 的 `ddl-auto` 默认 `validate`**：Hibernate 只校验不改表。改实体前先写好对应的 `V{n}` 脚本。
2. **版本号只增不改**：已提交的 `V{n}` 脚本一旦在任何环境执行过，内容不可再改（Flyway 校验 checksum），写错就追加新版本订正。
3. **既有库**首次带 Flyway 启动会自动 baseline（version=0），历史结构不重放；之后按版本依次执行。
4. **全新空库**：首次启动临时设环境变量 `DDL_AUTO=update` 让 Hibernate 建全量表，之后改回（或删掉该变量）。
5. 需要跳过 Flyway（如本地实验库）：`FLYWAY_ENABLED=false`。

## 为什么是双轨制（Hibernate 建表 + Flyway 管增量），而非纯 Flyway

本项目 Flyway 是在"既有库"之上引入的——表结构已由 Hibernate（`ddl-auto=update` 时期）+ 手工脚本演进完成。
曾考虑把全量 schema 写成 `V2__init.sql`（`CREATE TABLE ...`）实现"纯 Flyway 从零重建"，但权衡后放弃，原因：

1. **一致性风险**：30+ 实体的 DDL 必须与 Hibernate `validate` 完全一致，任何字段类型/精度/索引偏差都让 validate 失败或数据错位；
2. **既有库冲突**：既有库已有表，`V2` 需全部用 `CREATE TABLE IF NOT EXISTS`，而 `IF NOT EXISTS` 会掩盖真实的结构差异（既有库若被手工改过，差异被吞掉，反而危险）；
3. **双份真相源**：实体一改就要同步改 `V2`，实体与 DDL 两份 schema 真相，易漂移。

权衡后采用双轨制：
- **建表**交给 Hibernate（`ddl-auto=update` 仅全新库首次启动用，之后 `validate`）——单一真相源是实体；
- **结构变更**交给 Flyway（`V{n}` 增量）——版本化、可审计、checksum 校验。

全新库搭建流程：`DDL_AUTO=update` 首次建表 → 改回 `validate` → 之后所有变更走 `V{n}`。
这是"既有库引入版本化迁移"的工程妥协，而非 Flyway 教科书用法，但在当前阶段是收益/风险比最高的选择。

> 若未来要纯 Flyway 化：连干净库跑 `ddl-auto=create` → `pg_dump --schema-only` 导出 → 整理成 `V{n}__init.sql` → 在 CI 用空库验证 `validate` 通过。

## 示例

新增一列：

```sql
-- V2__add_poi_shares_visibility.sql
ALTER TABLE poi_shares ADD COLUMN IF NOT EXISTS visibility SMALLINT NOT NULL DEFAULT 0;
```

同时在实体上加对应字段，validate 启动通过即一致。
