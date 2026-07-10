# 数据库迁移策略（Flyway）

## 现状与分工

| 位置 | 作用 |
|---|---|
| `db/migration/`（本目录） | **唯一的增量结构变更入口**，Flyway 版本化脚本 `V{n}__描述.sql` |
| `resources/db/migration_*.sql` | 历史手工脚本（Flyway 引入前），仅作留档，已生效于既有库，勿再执行 |
| `smart-campus-backend/db/init/` | docker 首次初始化：扩展（PostGIS/pgvector）+ assistant_cache 表 |

## 规则

1. **prod 的 `ddl-auto` 默认 `validate`**：Hibernate 只校验不改表。改实体前先写好对应的 `V{n}` 脚本。
2. **版本号只增不改**：已提交的 `V{n}` 脚本一旦在任何环境执行过，内容不可再改（Flyway 校验 checksum），写错就追加新版本订正。
3. **既有库**首次带 Flyway 启动会自动 baseline（version=0），历史结构不重放；之后按版本依次执行。
4. **全新空库**：首次启动临时设环境变量 `DDL_AUTO=update` 让 Hibernate 建全量表，之后改回（或删掉该变量）。
5. 需要跳过 Flyway（如本地实验库）：`FLYWAY_ENABLED=false`。

## 示例

新增一列：

```sql
-- V2__add_poi_shares_visibility.sql
ALTER TABLE poi_shares ADD COLUMN IF NOT EXISTS visibility SMALLINT NOT NULL DEFAULT 0;
```

同时在实体上加对应字段，validate 启动通过即一致。
