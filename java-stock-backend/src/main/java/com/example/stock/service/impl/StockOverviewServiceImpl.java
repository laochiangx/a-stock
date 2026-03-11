package com.example.stock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.stock.dao.*;
import com.example.stock.entity.*;
import com.example.stock.service.StockOverviewService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class StockOverviewServiceImpl implements StockOverviewService {

    private static final Logger log = LoggerFactory.getLogger(StockOverviewServiceImpl.class);

    @Autowired
    private ThsHotListDayDao thsHotListDayDao;

    @Autowired
    private DcHotListDayDao dcHotListDayDao;

    @Autowired
    private ClsHotListDayDao clsHotListDayDao;

    @Autowired
    private ThemePlateDataDao themePlateDataDao;

    @Autowired
    private DongcaiThemePlateDao dongcaiThemePlateDao;

    @Autowired
    private WuyangThemeSubjectDao wuyangThemeSubjectDao;

    @Autowired(required = false)
    private StockDailySnapshotDao stockDailySnapshotDao;

    @Autowired(required = false)
    private StockDailySnapshotTagDao stockDailySnapshotTagDao;

    @Autowired(required = false)
    private TagsDao tagsDao;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, Object> listByThsHotRank(String date, String stockType, String listType) {
        Map<String, Object> result = new HashMap<>();
        String st = (stockType == null || stockType.trim().isEmpty()) ? "a" : stockType.trim();
        String lt = (listType == null || listType.trim().isEmpty()) ? "normal" : listType.trim();
        LocalDate queryDate = resolveQueryDate(date, st, lt);
        try {
            List<ThsHotListDay> list = thsHotListDayDao.findByDate(queryDate, st, "day", lt);
            result.put("success", true);
            result.put("date", queryDate.toString());
            result.put("stockType", st);
            result.put("listType", lt);
            result.put("count", list.size());
            result.put("data", list);
            return result;
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            result.put("date", queryDate.toString());
            result.put("stockType", st);
            result.put("listType", lt);
            return result;
        }
    }

    private Map<String, Object> fetchXuangutongThemes(LocalDate queryDate, String stockCode) {
        Map<String, Object> pack = new HashMap<>();
        pack.put("date", queryDate == null ? null : queryDate.toString());
        pack.put("data", Collections.emptyList());
        if (themePlateDataDao == null) {
            return pack;
        }

        List<String> candidates = buildStockCodeCandidates(stockCode);
        List<ThemePlateData> hit = queryThemePlateDataByCandidates(queryDate, candidates);
        if (hit != null && !hit.isEmpty()) {
            pack.put("data", hit);
            return pack;
        }

        LocalDate latest = findLatestThemePlateDataDate(candidates);
        if (latest != null && !latest.equals(queryDate)) {
            List<ThemePlateData> hitLatest = queryThemePlateDataByCandidates(latest, candidates);
            if (hitLatest != null && !hitLatest.isEmpty()) {
                pack.put("date", latest.toString());
                pack.put("data", hitLatest);
                return pack;
            }
        }
        return pack;
    }

    private List<ThemePlateData> queryThemePlateDataByCandidates(LocalDate date, List<String> candidates) {
        if (date == null || candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }
        for (String c : candidates) {
            if (c == null || c.trim().isEmpty()) {
                continue;
            }
            try {
                List<ThemePlateData> list = themePlateDataDao.findByDateAndStockCode(date, c);
                if (list != null && !list.isEmpty()) {
                    return list;
                }
            } catch (Exception ignore) {
            }
        }
        return Collections.emptyList();
    }

    private LocalDate findLatestThemePlateDataDate(List<String> candidates) {
        if (jdbcTemplate == null || candidates == null || candidates.isEmpty()) {
            return null;
        }
        LocalDate best = null;
        for (String c : candidates) {
            if (c == null || c.trim().isEmpty()) {
                continue;
            }
            try {
                java.sql.Date d = jdbcTemplate.queryForObject(
                        "SELECT MAX(data_date) FROM theme_plate_data WHERE stock_code = ?",
                        java.sql.Date.class,
                        c
                );
                if (d == null) {
                    continue;
                }
                LocalDate ld = d.toLocalDate();
                if (best == null || (ld != null && ld.isAfter(best))) {
                    best = ld;
                }
            } catch (Exception ignore) {
            }
        }
        return best;
    }

    private List<String> buildStockCodeCandidates(String stockCode) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        if (stockCode != null) {
            String raw = stockCode.trim();
            if (!raw.isEmpty()) {
                set.add(raw);
                set.add(raw.toUpperCase(Locale.ROOT));
            }
        }
        String norm = normalizeStockCode(stockCode);
        if (!norm.isEmpty()) {
            set.add(norm);
            set.add(norm + ".SZ");
            set.add(norm + ".SH");
            set.add(norm + ".SS");
        }
        return new ArrayList<>(set);
    }

    private String normalizeStockCode(String code) {
        if (code == null) {
            return "";
        }
        String c = code.trim();
        if (c.isEmpty()) {
            return "";
        }
        c = c.toUpperCase(Locale.ROOT);
        c = c.replace(".SZ", "").replace(".SS", "").replace(".SH", "");
        c = c.replace("SZ", "").replace("SH", "");
        return c;
    }

    @Override
    public Map<String, Object> getDetail(String date, String stockCode, String stockType, String thsListType) {
        Map<String, Object> result = new HashMap<>();
        if (stockCode == null || stockCode.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "stockCode required");
            return result;
        }
        String code = normalizeStockCode(stockCode);
        String st = (stockType == null || stockType.trim().isEmpty()) ? "a" : stockType.trim();
        String lt = (thsListType == null || thsListType.trim().isEmpty()) ? "normal" : thsListType.trim();
        LocalDate queryDate = resolveQueryDate(date, st, lt);

        try {
            Map<String, Object> data = new LinkedHashMap<>();

            ThsHotListDay ths = thsHotListDayDao.selectOne(
                    new LambdaQueryWrapper<ThsHotListDay>()
                            .eq(ThsHotListDay::getDataDate, queryDate)
                            .eq(ThsHotListDay::getStockType, st)
                            .eq(ThsHotListDay::getRankType, "day")
                            .eq(ThsHotListDay::getListType, lt)
                            .eq(ThsHotListDay::getStockCode, code)
                            .eq(ThsHotListDay::getDeleted, 0)
            );
            data.put("thsHot", ths);

            DcHotListDay dc = dcHotListDayDao.selectOne(
                    new LambdaQueryWrapper<DcHotListDay>()
                            .eq(DcHotListDay::getDataDate, queryDate)
                            .eq(DcHotListDay::getListType, "stock")
                            .eq(DcHotListDay::getStockCode, code)
                            .eq(DcHotListDay::getDeleted, 0)
            );
            data.put("dcHot", dc);

            ClsHotListDay cls = clsHotListDayDao.selectOne(
                    new LambdaQueryWrapper<ClsHotListDay>()
                            .eq(ClsHotListDay::getDataDate, queryDate)
                            .eq(ClsHotListDay::getListType, "stock")
                            .eq(ClsHotListDay::getStockCode, code)
                            .eq(ClsHotListDay::getDeleted, 0)
            );
            data.put("clsHot", cls);

            Map<String, Object> xgtPack = fetchXuangutongThemes(queryDate, stockCode);
            data.put("xuangutongThemes", xgtPack.get("data"));
            data.put("xuangutongDate", xgtPack.get("date"));

            List<DongcaiThemePlate> dcThemes = Collections.emptyList();
            try {
                dcThemes = dongcaiThemePlateDao.findByDateAndStockCode(queryDate, code);
            } catch (Exception ignore) {
            }
            data.put("dongcaiThemes", dcThemes);

            List<WuyangThemeSubject> wy = Collections.emptyList();
            try {
                wy = wuyangThemeSubjectDao.findByDateAndStockCode(queryDate, code);
            } catch (Exception ignore) {
            }
            data.put("wuyangSubjects", wy);

            Map<String, Object> tagSystem = fetchTagSystem(queryDate, code);
            data.put("tagSystem", tagSystem);

            Map<String, Object> dcHotThemeStocks = fetchDcHotThemeStocks(queryDate, code);
            data.put("dcHotThemeStocks", dcHotThemeStocks);

            result.put("success", true);
            result.put("date", queryDate.toString());
            result.put("stockCode", code);
            result.put("data", data);
            return result;
        } catch (Exception e) {
            log.error("getDetail failed", e);
            result.put("success", false);
            result.put("message", e.getMessage());
            result.put("date", queryDate.toString());
            result.put("stockCode", code);
            return result;
        }
    }

    private LocalDate resolveQueryDate(String date, String stockType, String listType) {
        if (date != null && !date.trim().isEmpty()) {
            return parseDate(date);
        }
        LocalDate latest = findLatestThsDate(stockType, listType);
        return latest != null ? latest : LocalDate.now();
    }

    private LocalDate findLatestThsDate(String stockType, String listType) {
        if (jdbcTemplate == null) {
            return null;
        }
        try {
            java.sql.Date d = jdbcTemplate.queryForObject(
                    "SELECT MAX(data_date) FROM ths_hot_list_day WHERE deleted = 0 AND stock_type = ? AND rank_type = 'day' AND list_type = ?",
                    java.sql.Date.class,
                    stockType,
                    listType
            );
            return d == null ? null : d.toLocalDate();
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> fetchTagSystem(LocalDate queryDate, String stockCode) {
        if (stockDailySnapshotDao == null || stockDailySnapshotTagDao == null || tagsDao == null) {
            return null;
        }
        try {
            StockDailySnapshot snapshot = stockDailySnapshotDao.selectOne(
                    new LambdaQueryWrapper<StockDailySnapshot>()
                            .eq(StockDailySnapshot::getDataDate, queryDate)
                            .eq(StockDailySnapshot::getStockCode, stockCode)
                            .eq(StockDailySnapshot::getDeleted, 0)
            );
            if (snapshot == null || snapshot.getId() == null) {
                return null;
            }

            List<StockDailySnapshotTag> rels = stockDailySnapshotTagDao.selectList(
                    new LambdaQueryWrapper<StockDailySnapshotTag>()
                            .eq(StockDailySnapshotTag::getSnapshotId, snapshot.getId())
                            .eq(StockDailySnapshotTag::getDeleted, 0)
            );
            if (rels == null || rels.isEmpty()) {
                return null;
            }

            List<Long> ids = new ArrayList<>();
            for (StockDailySnapshotTag r : rels) {
                if (r != null && r.getTagId() != null) {
                    ids.add(r.getTagId());
                }
            }
            if (ids.isEmpty()) {
                return null;
            }

            List<Tags> tags = tagsDao.selectBatchIds(ids);
            if (tags == null || tags.isEmpty()) {
                return null;
            }
            tags.sort((a, b) -> {
                int pa = a == null || a.getPriority() == null ? 0 : a.getPriority();
                int pb = b == null || b.getPriority() == null ? 0 : b.getPriority();
                return Integer.compare(pb, pa);
            });

            List<String> theme = new ArrayList<>();
            List<String> signal = new ArrayList<>();
            List<String> source = new ArrayList<>();
            List<String> other = new ArrayList<>();

            for (Tags t : tags) {
                if (t == null || t.getName() == null) continue;
                String n = t.getName().trim();
                if (n.isEmpty()) continue;
                String type = t.getType() == null ? "" : t.getType().trim();
                if ("theme".equalsIgnoreCase(type)) {
                    theme.add(n);
                } else if ("signal".equalsIgnoreCase(type)) {
                    signal.add(n);
                } else if ("source".equalsIgnoreCase(type)) {
                    source.add(n);
                } else {
                    other.add(n);
                }
            }

            Map<String, Object> ts = new LinkedHashMap<>();
            ts.put("snapshot", snapshot);
            ts.put("theme", theme);
            ts.put("signal", signal);
            ts.put("source", source);
            ts.put("other", other);

            JsonNode evidence = null;
            try {
                if (snapshot.getEvidence() != null && !snapshot.getEvidence().trim().isEmpty()) {
                    evidence = objectMapper.readTree(snapshot.getEvidence());
                }
            } catch (Exception ignore) {
            }
            if (evidence != null) {
                ts.put("evidence", evidence);
            }
            return ts;
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> fetchDcHotThemeStocks(LocalDate queryDate, String stockCode) {
        if (jdbcTemplate == null) {
            return null;
        }
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT theme_code, raw_json FROM dc_hot_theme_stock_day WHERE data_date = ? AND stock_code = ? AND deleted = 0 ORDER BY theme_code",
                    java.sql.Date.valueOf(queryDate),
                    stockCode
            );
            List<Map<String, Object>> list = new ArrayList<>();
            for (Map<String, Object> r : rows) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("themeCode", r.get("theme_code"));
                String raw = r.get("raw_json") == null ? null : String.valueOf(r.get("raw_json"));
                item.put("rawJson", raw);
                try {
                    if (raw != null && !raw.trim().isEmpty()) {
                        item.put("raw", objectMapper.readTree(raw));
                    }
                } catch (Exception ignore) {
                }
                list.add(item);
            }
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("count", list.size());
            res.put("data", list);
            return res;
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDate parseDate(String date) {
        if (date == null || date.trim().isEmpty()) {
            return LocalDate.now();
        }
        String d = date.trim();
        if (d.matches("\\d{8}")) {
            return LocalDate.parse(d, DateTimeFormatter.ofPattern("yyyyMMdd"));
        }
        if (d.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE);
        }
        return LocalDate.now();
    }
}
