package com.example.stock.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * Ensure required tables exist even if schema.sql was not executed successfully.
 */
@Component
public class ThsHotListSchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ThsHotListSchemaInitializer.class);

    private static final int QUERY_TIMEOUT_SECONDS = 3;

    private final JdbcTemplate jdbcTemplate;

    public ThsHotListSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            log.info("DB current_user={}, current_schema={}, search_path={}",
                    safeQueryString("select current_user"),
                    safeQueryString("select current_schema"),
                    safeQueryString("show search_path"));

            String schema = safeQueryString("select current_schema");
            if (!isSafeIdentifier(schema)) {
                schema = "public";
            }
            String tableFqn = schema + ".ths_hot_list_day";

            if (tableExists(tableFqn)) {
                return;
            }

            log.warn("Table ths_hot_list_day does not exist, creating it automatically...");

            // Create in current schema so unqualified SQL can find it via search_path
            execIgnoreError(
                    "CREATE TABLE IF NOT EXISTS " + tableFqn + " (\n" +
                            "  id SERIAL PRIMARY KEY,\n" +
                            "  data_date DATE NOT NULL,\n" +
                            "  stock_type VARCHAR(20) NOT NULL,\n" +
                            "  rank_type VARCHAR(20) NOT NULL,\n" +
                            "  list_type VARCHAR(20) NOT NULL,\n" +
                            "  market INTEGER,\n" +
                            "  stock_code VARCHAR(20) NOT NULL,\n" +
                            "  stock_name VARCHAR(100),\n" +
                            "  rate DECIMAL(20,4),\n" +
                            "  rise_and_fall DECIMAL(20,8),\n" +
                            "  analyse_title VARCHAR(500),\n" +
                            "  analysis TEXT,\n" +
                            "  hot_rank_chg INTEGER,\n" +
                            "  topic_code VARCHAR(100),\n" +
                            "  topic_title VARCHAR(500),\n" +
                            "  topic_ios_jump_url VARCHAR(1000),\n" +
                            "  topic_android_jump_url VARCHAR(1000),\n" +
                            "  concept_tags TEXT,\n" +
                            "  popularity_tag VARCHAR(200),\n" +
                            "  order_num INTEGER,\n" +
                            "  raw_json TEXT,\n" +
                            "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
                            "  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
                            "  deleted INTEGER DEFAULT 0,\n" +
                            "  UNIQUE(data_date, stock_type, rank_type, list_type, stock_code)\n" +
                            ")"
            );

            if (!tableExists(tableFqn)) {
                log.warn("Table {} still does not exist after initialization attempt.", tableFqn);
                return;
            }

            execIgnoreError("CREATE INDEX IF NOT EXISTS idx_ths_hot_list_day_date ON " + tableFqn + " (data_date)");
            execIgnoreError("CREATE INDEX IF NOT EXISTS idx_ths_hot_list_day_order ON " + tableFqn + " (data_date, order_num)");

            log.info("Table {} created/verified.", tableFqn);
        } catch (Exception e) {
            log.error("Failed to ensure ths_hot_list_day schema", e);
        }
    }

    private String safeQueryString(String sql) {
        try {
            PreparedStatementCreator psc = new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Connection con) throws java.sql.SQLException {
                    PreparedStatement ps = con.prepareStatement(sql);
                    ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
                    return ps;
                }
            };
            return jdbcTemplate.query(psc, rs -> rs.next() ? rs.getString(1) : "<unknown>");
        } catch (Exception e) {
            return "<unknown>";
        }
    }

    private boolean tableExists(String tableFqn) {
        try {
            PreparedStatementCreator psc = new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Connection con) throws java.sql.SQLException {
                    PreparedStatement ps = con.prepareStatement("SELECT to_regclass(?) IS NOT NULL");
                    ps.setString(1, tableFqn);
                    ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
                    return ps;
                }
            };
            Boolean exists = jdbcTemplate.query(psc, rs -> rs.next() ? rs.getBoolean(1) : Boolean.FALSE);
            return exists != null && exists;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isSafeIdentifier(String s) {
        return s != null && s.matches("[A-Za-z0-9_]+$");
    }

    private void execIgnoreError(String ddl) {
        try {
            jdbcTemplate.execute(ddl);
        } catch (Exception e) {
            log.warn("DDL failed (ignored): {}", ddl, e);
        }
    }
}
