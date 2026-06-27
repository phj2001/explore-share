-- PostgreSQL 初始化扩展脚本
-- 由 docker-compose 挂载到 postgres 容器的 /docker-entrypoint-initdb.d/
-- 仅在数据卷首次初始化时执行一次（pg_data 卷为空时）

-- PostGIS：Hibernate Spatial (geometry 类型) + 空间索引的硬性依赖
-- 若缺失，后端启动时 ddl-auto=update 建 POI 空间列将直接报错
CREATE EXTENSION IF NOT EXISTS postgis;

-- pg_trgm：POI 名称模糊搜索（POIDatabaseIndexInitializer 也会幂等创建，此处前置保证可用）
CREATE EXTENSION IF NOT EXISTS pg_trgm;
