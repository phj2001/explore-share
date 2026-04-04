package com.smartcampus.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

@Slf4j
@Component
@RequiredArgsConstructor
public class POIDatabaseIndexInitializer implements ApplicationRunner {

    private static final String[] STANDARD_INDEX_SQL = {
            "CREATE INDEX IF NOT EXISTS idx_pois_category ON pois (category)",
            "CREATE INDEX IF NOT EXISTS idx_pois_category_id ON pois (category, id)",
            "CREATE INDEX IF NOT EXISTS idx_pois_latitude_longitude ON pois (latitude, longitude)",
            "CREATE INDEX IF NOT EXISTS idx_poi_check_ins_poi_id ON poi_check_ins (poi_id)",
            "CREATE INDEX IF NOT EXISTS idx_poi_check_ins_user_id ON poi_check_ins (user_id)",
            "CREATE INDEX IF NOT EXISTS idx_poi_check_ins_poi_created_at ON poi_check_ins (poi_id, created_at DESC)",
            "CREATE INDEX IF NOT EXISTS idx_poi_shares_poi_created_at ON poi_shares (poi_id, created_at DESC, id DESC)",
            "CREATE INDEX IF NOT EXISTS idx_poi_share_replies_share_created_at ON poi_share_replies (share_id, created_at ASC, id ASC)",
            "CREATE INDEX IF NOT EXISTS idx_poi_share_likes_share_id ON poi_share_likes (share_id)",
            "CREATE INDEX IF NOT EXISTS idx_poi_share_likes_user_share ON poi_share_likes (user_id, share_id)"
    };

    private static final String CREATE_TRIGRAM_EXTENSION_SQL = "CREATE EXTENSION IF NOT EXISTS pg_trgm";
    private static final String CREATE_TRIGRAM_INDEX_SQL =
            "CREATE INDEX IF NOT EXISTS idx_pois_name_trgm ON pois USING gin (LOWER(name) gin_trgm_ops)";

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) {
        if (!isPostgreSql()) {
            log.info("当前数据库不是 PostgreSQL，跳过 POI 索引初始化");
            return;
        }

        createStandardIndexes();
        createNameSearchIndex();
    }

    private boolean isPostgreSql() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            return metaData.getDatabaseProductName() != null
                    && metaData.getDatabaseProductName().toLowerCase().contains("postgresql");
        } catch (Exception exception) {
            log.warn("检测数据库类型失败，跳过 POI 索引初始化: {}", exception.getMessage());
            return false;
        }
    }

    private void createStandardIndexes() {
        for (String sql : STANDARD_INDEX_SQL) {
            try {
                jdbcTemplate.execute(sql);
            } catch (Exception exception) {
                log.warn("执行索引初始化 SQL 失败: {}，原因: {}", sql, exception.getMessage());
            }
        }
    }

    private void createNameSearchIndex() {
        try {
            jdbcTemplate.execute(CREATE_TRIGRAM_EXTENSION_SQL);
            jdbcTemplate.execute(CREATE_TRIGRAM_INDEX_SQL);
        } catch (Exception exception) {
            log.warn("创建 POI 名称模糊搜索索引失败，名称搜索性能将不会被优化: {}", exception.getMessage());
        }
    }
}
