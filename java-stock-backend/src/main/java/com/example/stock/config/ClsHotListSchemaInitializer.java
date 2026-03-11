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

@Component
public class ClsHotListSchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ClsHotListSchemaInitializer.class);

    private static final int QUERY_TIMEOUT_SECONDS = 3;

    private final JdbcTemplate jdbcTemplate;

    public ClsHotListSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            ensureDayTable();
        } catch (Exception e) {
            log.warn("CLS schema initializer failed (ignored)", e);
        }
    }

    private void ensureDayTable() {
        String tableFqn = "cls_hot_list_day";

        if (tableExists(tableFqn)) {
            return;
        }

        execIgnoreError(
                "CREATE TABLE IF NOT EXISTS " + tableFqn + " (" +
                        "id SERIAL PRIMARY KEY," +
                        "data_date DATE NOT NULL," +
                        "list_type VARCHAR(20) NOT NULL," +
                        "stock_code VARCHAR(20) NOT NULL," +
                        "stock_name VARCHAR(100)," +
                        "hot_score DECIMAL(20,4)," +
                        "rise_and_fall DECIMAL(20,8)," +
                        "order_num INTEGER," +
                        "raw_json TEXT," +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "deleted INTEGER DEFAULT 0," +
                        "UNIQUE(data_date, list_type, stock_code)" +
                        ")"
        );
        execIgnoreError("CREATE INDEX IF NOT EXISTS idx_cls_hot_list_day_date ON " + tableFqn + " (data_date)");
        execIgnoreError("CREATE INDEX IF NOT EXISTS idx_cls_hot_list_day_order ON " + tableFqn + " (data_date, order_num)");

        log.info("Table {} created/verified.", tableFqn);
    }

    private void execIgnoreError(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            log.warn("execIgnoreError failed, sql.head={}", sql == null ? "" : (sql.length() > 120 ? sql.substring(0, 120) : sql));
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
}
