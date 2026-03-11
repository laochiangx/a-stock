package com.example.stock.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.stock.dao.StockDailySnapshotDao;
import com.example.stock.dao.StockDailySnapshotTagDao;
import com.example.stock.dao.TagsDao;
import com.example.stock.entity.StockDailySnapshot;
import com.example.stock.entity.StockDailySnapshotTag;
import com.example.stock.entity.Tags;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class StockTaggingService {

    @Autowired
    private ThemePlateService themePlateService;

    @Autowired
    private StockDataService stockDataService;

    @Autowired
    private PopularityRankService popularityRankService;

    @Autowired
    private StockDailySnapshotDao stockDailySnapshotDao;

    @Autowired
    private StockDailySnapshotTagDao stockDailySnapshotTagDao;

    @Autowired
    private TagsDao tagsDao;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> tagByDate(LocalDate date) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, SnapshotAgg> agg = new LinkedHashMap<>();

            ensureTagLibrary();

            collectThemeXuangutong(date, agg);
            collectThemeDongcai(date, agg);
            collectDragonTiger(date, agg);
            collectPopularity(date, agg);

            int upsertCount = 0;
            int tagLinkCount = 0;

            for (SnapshotAgg a : agg.values()) {
                StockDailySnapshot snapshot = upsertSnapshot(date, a);
                upsertCount++;

                for (TagDef t : a.tags.values()) {
                    Tags tag = ensureTagExists(t.name, t.type, t.priority);
                    if (tag == null || tag.getId() == null) {
                        continue;
                    }
                    boolean linked = ensureSnapshotTag(snapshot.getId(), tag.getId());
                    if (linked) {
                        tagLinkCount++;
                    }
                }
            }

            result.put("success", true);
            result.put("date", date.toString());
            result.put("snapshotCount", upsertCount);
            result.put("tagLinkCount", tagLinkCount);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    private void ensureTagLibrary() {
        List<TagDef> defs = Arrays.asList(
                new TagDef("选股通题材", "source", 500),
                new TagDef("东财题材", "source", 500),
                new TagDef("龙虎榜", "source", 1000),
                new TagDef("人气聚合", "source", 400),
                new TagDef("热榜Top20", "source", 900),

                new TagDef("强势候选", "signal", 700),
                new TagDef("资金强", "signal", 650),

                new TagDef("涨停", "signal", 950),
                new TagDef("最快涨停", "signal", 945),
                new TagDef("龙一", "signal", 940),
                new TagDef("游资买入", "signal", 880),
                new TagDef("机构参与", "signal", 860),
                new TagDef("北向资金", "signal", 820),
                new TagDef("多头排列", "signal", 760),
                new TagDef("大市值", "signal", 720),

                new TagDef("炸板", "signal", 910),
                new TagDef("回封", "signal", 915),
                new TagDef("换手", "signal", 740),
                new TagDef("放量", "signal", 735),
                new TagDef("缩量", "signal", 730),
                new TagDef("弱转强", "signal", 805),
                new TagDef("反包", "signal", 800),
                new TagDef("突破新高", "signal", 790),
                new TagDef("补涨", "signal", 710),
                new TagDef("卡位", "signal", 705),
                new TagDef("情绪龙头", "signal", 900),
                new TagDef("容量", "signal", 700),

                new TagDef("国产芯片", "theme", 600),
                new TagDef("存储芯片", "theme", 600),
                new TagDef("Chiplet", "theme", 600),
                new TagDef("先进封装", "theme", 600)
        );

        for (TagDef d : defs) {
            ensureTagExists(d.name, d.type, d.priority);
        }
    }

    private void collectThemeXuangutong(LocalDate date, Map<String, SnapshotAgg> agg) {
        String dateStr = date.toString();
        Map<String, Object> res = themePlateService.getFullData(dateStr);
        Object successObj = res.get("success");
        if (!(successObj instanceof Boolean) || !((Boolean) successObj)) {
            return;
        }

        Object dataObj = res.get("data");
        if (!(dataObj instanceof Map)) {
            return;
        }

        Object groupsObj = ((Map<?, ?>) dataObj).get("groups");
        if (!(groupsObj instanceof List)) {
            return;
        }

        for (Object gObj : (List<?>) groupsObj) {
            if (!(gObj instanceof Map)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> g = (Map<String, Object>) gObj;
            String plateName = String.valueOf(g.getOrDefault("plateName", ""));
            Object stocksObj = g.get("stocks");
            if (!(stocksObj instanceof List)) {
                continue;
            }
            for (Object sObj : (List<?>) stocksObj) {
                if (!(sObj instanceof Map)) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> s = (Map<String, Object>) sObj;
                String code = normalizeStockCode(String.valueOf(s.getOrDefault("code", "")));
                if (code.isEmpty()) {
                    continue;
                }
                String name = String.valueOf(s.getOrDefault("name", ""));
                SnapshotAgg a = agg.computeIfAbsent(code, k -> new SnapshotAgg(code));
                if (a.stockName == null || a.stockName.isEmpty()) {
                    a.stockName = name;
                }
                a.sources.add("theme_xgt");
                if (plateName != null && !plateName.isEmpty()) {
                    a.tags.putIfAbsent("theme:" + plateName, new TagDef(plateName, "theme", 600));
                }
                a.tags.putIfAbsent("source:选股通题材", new TagDef("选股通题材", "source", 500));

                String description = String.valueOf(s.getOrDefault("description", ""));
                String lianban = String.valueOf(s.getOrDefault("lianban", ""));
                String text = String.valueOf(plateName) + " " + description + " " + lianban;
                applyInterpretationTags(a, text, "theme_xgt");

                a.evidence.computeIfAbsent("theme_xgt", k -> new ArrayList<Map<String, Object>>());
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> list = (List<Map<String, Object>>) a.evidence.get("theme_xgt");
                Map<String, Object> ev = new HashMap<>();
                ev.put("plateName", plateName);
                ev.put("raw", s);
                list.add(ev);
            }
        }
    }

    private void applyInterpretationTags(SnapshotAgg a, String text, String source) {
        if (a == null || text == null || text.trim().isEmpty()) {
            return;
        }

        String src = source == null ? "" : source;

        if (containsAny(text, "情绪龙头", "情绪龙", "情绪核心")) {
            addTagWithHit(a, "signal:情绪龙头", new TagDef("情绪龙头", "signal", 900), src, "情绪龙头");
        }

        if (containsAny(text, "涨停", "封板", "一字板", "20cm")) {
            addTagWithHit(a, "signal:涨停", new TagDef("涨停", "signal", 950), src, "涨停");
        }
        if (containsAny(text, "龙一", "今日龙一", "龙头")) {
            addTagWithHit(a, "signal:龙一", new TagDef("龙一", "signal", 940), src, "龙一");
        }
        if (containsAny(text, "最快涨停", "最先涨停", "首板最快")) {
            addTagWithHit(a, "signal:最快涨停", new TagDef("最快涨停", "signal", 945), src, "最快涨停");
        }

        if (containsAny(text, "炸板", "开板", "炸", "开板回落")) {
            addTagWithHit(a, "signal:炸板", new TagDef("炸板", "signal", 910), src, "炸板");
        }
        if (containsAny(text, "回封", "回封涨停", "回封板")) {
            addTagWithHit(a, "signal:回封", new TagDef("回封", "signal", 915), src, "回封");
        }

        Matcher m = Pattern.compile("(\\d+)\\s*连板").matcher(text);
        if (m.find()) {
            String tagName = m.group(1) + "连板";
            addTagWithHit(a, "signal:" + tagName, new TagDef(tagName, "signal", 930), src, tagName);
        }

        if (containsAny(text, "游资", "章盟主", "赵老哥", "炒股养家", "上塘路", "席位")) {
            addTagWithHit(a, "signal:游资买入", new TagDef("游资买入", "signal", 880), src, "游资");
        }
        if (containsAny(text, "机构")) {
            addTagWithHit(a, "signal:机构参与", new TagDef("机构参与", "signal", 860), src, "机构");
        }
        if (containsAny(text, "北向")) {
            addTagWithHit(a, "signal:北向资金", new TagDef("北向资金", "signal", 820), src, "北向");
        }

        if (containsAny(text, "多头排列", "均线多头", "多头")) {
            addTagWithHit(a, "signal:多头排列", new TagDef("多头排列", "signal", 760), src, "多头排列");
        }
        if (containsAny(text, "大市值", "千亿", "百亿")) {
            addTagWithHit(a, "signal:大市值", new TagDef("大市值", "signal", 720), src, "大市值");
        }

        if (containsAny(text, "换手", "换手率")) {
            addTagWithHit(a, "signal:换手", new TagDef("换手", "signal", 740), src, "换手");
        }
        if (containsAny(text, "放量", "放量上攻", "放量突破")) {
            addTagWithHit(a, "signal:放量", new TagDef("放量", "signal", 735), src, "放量");
        }
        if (containsAny(text, "缩量", "缩量上涨", "缩量回调")) {
            addTagWithHit(a, "signal:缩量", new TagDef("缩量", "signal", 730), src, "缩量");
        }
        if (containsAny(text, "弱转强", "分歧转一致", "承接强")) {
            addTagWithHit(a, "signal:弱转强", new TagDef("弱转强", "signal", 805), src, "弱转强");
        }
        if (containsAny(text, "反包", "反包涨停")) {
            addTagWithHit(a, "signal:反包", new TagDef("反包", "signal", 800), src, "反包");
        }
        if (containsAny(text, "新高", "突破新高", "历史新高")) {
            addTagWithHit(a, "signal:突破新高", new TagDef("突破新高", "signal", 790), src, "新高");
        }
        if (containsAny(text, "补涨", "补涨龙")) {
            addTagWithHit(a, "signal:补涨", new TagDef("补涨", "signal", 710), src, "补涨");
        }
        if (containsAny(text, "卡位", "卡位龙")) {
            addTagWithHit(a, "signal:卡位", new TagDef("卡位", "signal", 705), src, "卡位");
        }
        if (containsAny(text, "容量", "容量中军", "中军")) {
            addTagWithHit(a, "signal:容量", new TagDef("容量", "signal", 700), src, "容量");
        }

        if (containsAny(text, "国产芯片", "半导体", "芯片")) {
            addTagWithHit(a, "theme:国产芯片", new TagDef("国产芯片", "theme", 600), src, "芯片");
        }

        if (containsAny(text, "存储芯片", "存储", "存储器", "NAND", "DRAM", "HBM", "闪存", "内存")) {
            addTagWithHit(a, "theme:存储芯片", new TagDef("存储芯片", "theme", 600), src, "存储");
        }

        if (containsAny(text, "Chiplet", "chiplet", "先进封装", "封装", "2.5D", "3D封装")) {
            addTagWithHit(a, "theme:Chiplet", new TagDef("Chiplet", "theme", 600), src, "Chiplet/封装");
            if (containsAny(text, "先进封装", "2.5D", "3D封装")) {
                addTagWithHit(a, "theme:先进封装", new TagDef("先进封装", "theme", 600), src, "先进封装");
            }
        }
    }

    private void addTagWithHit(SnapshotAgg a, String tagKey, TagDef def, String source, String keyword) {
        if (a == null || tagKey == null || def == null) {
            return;
        }
        a.tags.putIfAbsent(tagKey, def);

        a.evidence.computeIfAbsent("rule_hits", k -> new ArrayList<Map<String, Object>>());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> hits = (List<Map<String, Object>>) a.evidence.get("rule_hits");
        Map<String, Object> hit = new HashMap<>();
        hit.put("tag", def.name);
        hit.put("keyword", keyword);
        hit.put("source", source);
        hits.add(hit);
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null || keywords == null) {
            return false;
        }
        for (String k : keywords) {
            if (k != null && !k.isEmpty() && text.contains(k)) {
                return true;
            }
        }
        return false;
    }

    private void collectThemeDongcai(LocalDate date, Map<String, SnapshotAgg> agg) {
        String dateStr = date.toString();
        Map<String, Object> res = themePlateService.getDongcaiFullData(dateStr);
        Object successObj = res.get("success");
        if (!(successObj instanceof Boolean) || !((Boolean) successObj)) {
            return;
        }

        Object dataObj = res.get("data");
        if (!(dataObj instanceof Map)) {
            return;
        }

        Object groupsObj = ((Map<?, ?>) dataObj).get("groups");
        if (!(groupsObj instanceof List)) {
            return;
        }

        for (Object gObj : (List<?>) groupsObj) {
            if (!(gObj instanceof Map)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> g = (Map<String, Object>) gObj;
            String plateName = String.valueOf(g.getOrDefault("plateName", ""));
            Object stocksObj = g.get("stocks");
            if (!(stocksObj instanceof List)) {
                continue;
            }
            for (Object sObj : (List<?>) stocksObj) {
                if (!(sObj instanceof Map)) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> s = (Map<String, Object>) sObj;
                String code = normalizeStockCode(String.valueOf(s.getOrDefault("code", "")));
                if (code.isEmpty()) {
                    continue;
                }
                String name = String.valueOf(s.getOrDefault("name", ""));
                SnapshotAgg a = agg.computeIfAbsent(code, k -> new SnapshotAgg(code));
                if (a.stockName == null || a.stockName.isEmpty()) {
                    a.stockName = name;
                }
                a.sources.add("theme_dc");
                if (plateName != null && !plateName.isEmpty()) {
                    a.tags.putIfAbsent("theme:" + plateName, new TagDef(plateName, "theme", 600));
                }
                a.tags.putIfAbsent("source:东财题材", new TagDef("东财题材", "source", 500));

                String description = String.valueOf(s.getOrDefault("description", ""));
                String lianban = String.valueOf(s.getOrDefault("lianban", ""));
                String text = String.valueOf(plateName) + " " + description + " " + lianban;
                applyInterpretationTags(a, text, "theme_dc");

                a.evidence.computeIfAbsent("theme_dc", k -> new ArrayList<Map<String, Object>>());
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> list = (List<Map<String, Object>>) a.evidence.get("theme_dc");
                Map<String, Object> ev = new HashMap<>();
                ev.put("plateName", plateName);
                ev.put("raw", s);
                list.add(ev);
            }
        }
    }

    private void collectDragonTiger(LocalDate date, Map<String, SnapshotAgg> agg) {
        Map<String, Object> res = stockDataService.getDragonTigerList(date.toString());
        Object dataObj = res.get("data");
        if (!(dataObj instanceof List)) {
            return;
        }

        for (Object itemObj : (List<?>) dataObj) {
            if (!(itemObj instanceof Map)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> item = (Map<String, Object>) itemObj;
            String code = normalizeStockCode(String.valueOf(item.getOrDefault("SECURITY_CODE", "")));
            if (code.isEmpty()) {
                continue;
            }
            String name = String.valueOf(item.getOrDefault("SECURITY_NAME_ABBR", ""));
            String explain = String.valueOf(item.getOrDefault("EXPLAIN", ""));

            SnapshotAgg a = agg.computeIfAbsent(code, k -> new SnapshotAgg(code));
            if (a.stockName == null || a.stockName.isEmpty()) {
                a.stockName = name;
            }
            a.sources.add("dragon_tiger");
            a.dragonTiger = true;
            if (explain != null && !explain.isEmpty()) {
                a.dragonTigerExplain = explain;
                applyInterpretationTags(a, explain, "dragon_tiger");
            }

            a.tags.putIfAbsent("source:龙虎榜", new TagDef("龙虎榜", "source", 1000));

            a.evidence.computeIfAbsent("dragon_tiger", k -> new ArrayList<Map<String, Object>>());
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> list = (List<Map<String, Object>>) a.evidence.get("dragon_tiger");
            Map<String, Object> ev = new HashMap<>();
            ev.put("raw", item);
            list.add(ev);
        }
    }

    private void collectPopularity(LocalDate date, Map<String, SnapshotAgg> agg) {
        Map<String, Object> res = popularityRankService.getPopularityRank(200);
        Object successObj = res.get("success");
        if (!(successObj instanceof Boolean) || !((Boolean) successObj)) {
            return;
        }

        Object dataObj = res.get("data");
        if (!(dataObj instanceof List)) {
            return;
        }

        for (Object itemObj : (List<?>) dataObj) {
            if (!(itemObj instanceof Map)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> item = (Map<String, Object>) itemObj;
            String code = normalizeStockCode(String.valueOf(item.getOrDefault("code", "")));
            if (code.isEmpty()) {
                continue;
            }
            String name = String.valueOf(item.getOrDefault("name", ""));

            SnapshotAgg a = agg.computeIfAbsent(code, k -> new SnapshotAgg(code));
            if (a.stockName == null || a.stockName.isEmpty()) {
                a.stockName = name;
            }
            a.sources.add("popularity_rank");
            a.popularity = item;

            a.tags.putIfAbsent("source:人气聚合", new TagDef("人气聚合", "source", 400));

            Integer thspm = toInteger(item.get("thspm"));
            if (thspm != null && thspm > 0 && thspm <= 20) {
                a.tags.putIfAbsent("source:热榜Top20", new TagDef("热榜Top20", "source", 900));
            }

            Boolean preselected = toBoolean(item.get("preselected"));
            if (Boolean.TRUE.equals(preselected)) {
                a.tags.putIfAbsent("signal:强势候选", new TagDef("强势候选", "signal", 700));
            }

            Double zlje = toDouble(item.get("zlje"));
            if (zlje != null && zlje >= 20000000D) {
                a.tags.putIfAbsent("signal:资金强", new TagDef("资金强", "signal", 650));
            }

            a.evidence.put("popularity_rank", item);
        }
    }

    private StockDailySnapshot upsertSnapshot(LocalDate date, SnapshotAgg a) {
        StockDailySnapshot existing = stockDailySnapshotDao.selectOne(
                new LambdaQueryWrapper<StockDailySnapshot>()
                        .eq(StockDailySnapshot::getDataDate, date)
                        .eq(StockDailySnapshot::getStockCode, a.stockCode)
        );

        StockDailySnapshot snapshot = existing != null ? existing : new StockDailySnapshot();
        snapshot.setDataDate(date);
        snapshot.setStockCode(a.stockCode);
        snapshot.setStockName(a.stockName);
        snapshot.setSources(String.join(",", a.sources));

        if (a.popularity != null) {
            snapshot.setDcpm(toInteger(a.popularity.get("dcpm")));
            snapshot.setRqbpm(toInteger(a.popularity.get("rqbpm")));
            snapshot.setThspm(toInteger(a.popularity.get("thspm")));
            snapshot.setShy(toInteger(a.popularity.get("shy")));
            Double zf = toDouble(a.popularity.get("zf"));
            snapshot.setZf(zf == null ? null : BigDecimal.valueOf(zf));
            Double zlje = toDouble(a.popularity.get("zlje"));
            snapshot.setZlje(zlje == null ? null : BigDecimal.valueOf(zlje));
            Double zljzb = toDouble(a.popularity.get("zljzb"));
            snapshot.setZljzb(zljzb == null ? null : BigDecimal.valueOf(zljzb));
            snapshot.setPreselected(toBoolean(a.popularity.get("preselected")));
        }

        if (a.dragonTiger != null) {
            snapshot.setDragonTiger(a.dragonTiger);
        }
        snapshot.setDragonTigerExplain(a.dragonTigerExplain);

        try {
            snapshot.setEvidence(objectMapper.writeValueAsString(a.evidence));
        } catch (Exception ignore) {
            snapshot.setEvidence(null);
        }

        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            snapshot.setCreatedAt(now);
            snapshot.setUpdatedAt(now);
            snapshot.setDeleted(0);
            stockDailySnapshotDao.insert(snapshot);
        } else {
            snapshot.setUpdatedAt(now);
            stockDailySnapshotDao.updateById(snapshot);
        }

        return snapshot;
    }

    private boolean ensureSnapshotTag(Long snapshotId, Long tagId) {
        if (snapshotId == null || tagId == null) {
            return false;
        }
        StockDailySnapshotTag existing = stockDailySnapshotTagDao.selectOne(
                new LambdaQueryWrapper<StockDailySnapshotTag>()
                        .eq(StockDailySnapshotTag::getSnapshotId, snapshotId)
                        .eq(StockDailySnapshotTag::getTagId, tagId)
        );
        if (existing != null) {
            return false;
        }

        StockDailySnapshotTag rel = new StockDailySnapshotTag();
        rel.setSnapshotId(snapshotId);
        rel.setTagId(tagId);
        LocalDateTime now = LocalDateTime.now();
        rel.setCreatedAt(now);
        rel.setUpdatedAt(now);
        rel.setDeleted(0);
        stockDailySnapshotTagDao.insert(rel);
        return true;
    }

    private Tags ensureTagExists(String name, String type, int priority) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        Tags existing = tagsDao.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Tags>()
                        .eq("name", name.trim())
        );

        LocalDateTime now = LocalDateTime.now();
        if (existing != null) {
            boolean changed = false;
            if (type != null && !type.equals(existing.getType())) {
                existing.setType(type);
                changed = true;
            }
            if (existing.getPriority() == null || existing.getPriority() != priority) {
                existing.setPriority(priority);
                changed = true;
            }
            if (changed) {
                existing.setUpdatedAt(now);
                tagsDao.updateById(existing);
            }
            return existing;
        }

        Tags tag = new Tags();
        tag.setName(name.trim());
        tag.setType(type);
        tag.setPriority(priority);
        tag.setCreatedAt(now);
        tag.setUpdatedAt(now);
        tag.setDeleted(0);
        tagsDao.insert(tag);
        return tag;
    }

    private String normalizeStockCode(String code) {
        if (code == null) {
            return "";
        }
        String c = code.trim();
        c = c.replace(".SZ", "").replace(".SS", "").replace(".SH", "");
        c = c.replace("sz", "").replace("sh", "");
        return c;
    }

    private Integer toInteger(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).intValue();
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            return null;
        }
    }

    private Double toDouble(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).doubleValue();
        try {
            return Double.parseDouble(String.valueOf(v));
        } catch (Exception e) {
            return null;
        }
    }

    private Boolean toBoolean(Object v) {
        if (v == null) return null;
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof Number) return ((Number) v).intValue() != 0;
        String s = String.valueOf(v);
        if ("true".equalsIgnoreCase(s)) return true;
        if ("false".equalsIgnoreCase(s)) return false;
        return null;
    }

    private static class SnapshotAgg {
        private final String stockCode;
        private String stockName;
        private final Set<String> sources = new LinkedHashSet<>();
        private final Map<String, TagDef> tags = new LinkedHashMap<>();
        private final Map<String, Object> evidence = new LinkedHashMap<>();

        private Map<String, Object> popularity;
        private Boolean dragonTiger;
        private String dragonTigerExplain;

        private SnapshotAgg(String stockCode) {
            this.stockCode = stockCode;
        }
    }

    private static class TagDef {
        private final String name;
        private final String type;
        private final int priority;

        private TagDef(String name, String type, int priority) {
            this.name = name;
            this.type = type;
            this.priority = priority;
        }
    }
}
