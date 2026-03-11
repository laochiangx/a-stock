package com.example.stock.service.impl;

import com.example.stock.dao.MarketDailyStatDao;
import com.example.stock.entity.MarketDailyStat;
import com.example.stock.service.MarketDailyStatService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class MarketDailyStatServiceImpl implements MarketDailyStatService {

    private static final Logger log = LoggerFactory.getLogger(MarketDailyStatServiceImpl.class);

    private static final String WUYANG_REPLAYROBOT_JSON_URL_TEMPLATE = "https://www.wuylh.com/replayrobot/json/%sp.json";
    private static final String WUYANG_RECENTLY_TRADE_DATE_URL = "https://www.wuylh.com/jihehelper/api/recentlyTradeDate";

    @Autowired
    private MarketDailyStatDao marketDailyStatDao;

    private final OkHttpClient client;
    private final ObjectMapper mapper;

    public MarketDailyStatServiceImpl() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(30))
                .build();
        this.mapper = new ObjectMapper();
    }

    @Override
    public Map<String, Object> syncDailyStats(String date, Boolean force) {
        Map<String, Object> result = new HashMap<>();
        boolean f = force != null && force;
        LocalDate queryDate = resolveWuyangDate(date);

        try {
            JsonNode root = null;
            LocalDate fetchDate = queryDate;
            for (int i = 0; i <= 30; i++) {
                try {
                    log.info("fetch wuylh tenDays: date={}, attempt={}", fetchDate, i + 1);
                    root = fetchTenDaysRoot(fetchDate);
                    break;
                } catch (RuntimeException e) {
                    String msg = e.getMessage();
                    boolean is404 = msg != null && msg.contains("HTTP 404");
                    if (is404 && i < 30) {
                        log.warn("wuylh json 404 for date={}, fallback to previous day", fetchDate);
                        fetchDate = fetchDate.minusDays(1);
                        continue;
                    }
                    throw e;
                }
            }

            String fetchUrl = String.format(WUYANG_REPLAYROBOT_JSON_URL_TEMPLATE, fetchDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
            result.put("requestedDate", queryDate.toString());
            result.put("fetchDate", fetchDate.toString());
            result.put("fetchUrl", fetchUrl);

            int existing = marketDailyStatDao.countByDate(fetchDate);
            if (existing > 0 && !f) {
                result.put("success", true);
                result.put("date", fetchDate.toString());
                result.put("message", "exists");
                result.put("count", existing);
                return result;
            }
            if (root == null) {
                result.put("success", false);
                result.put("date", fetchDate.toString());
                result.put("message", "fetch failed");
                return result;
            }

            List<MarketDailyStat> parsed = parseTenDays(root);
            if (parsed.isEmpty()) {
                result.put("success", false);
                result.put("date", fetchDate.toString());
                result.put("message", "no data");
                return result;
            }

            parsed.sort(Comparator.comparing(MarketDailyStat::getDataDate));
            applyScoreRules(parsed);
            applyStageRules(parsed);
            fillRawJson(parsed);

            int upsertCount = 0;
            for (MarketDailyStat stat : parsed) {
                if (stat.getDataDate() == null) {
                    continue;
                }
                if (f) {
                    marketDailyStatDao.physicalDeleteByDate(stat.getDataDate());
                }
                int c = marketDailyStatDao.countByDate(stat.getDataDate());
                if (c > 0 && !f) {
                    continue;
                }
                marketDailyStatDao.physicalDeleteByDate(stat.getDataDate());
                marketDailyStatDao.insert(stat);
                upsertCount++;
            }

            result.put("success", true);
            result.put("date", fetchDate.toString());
            result.put("count", upsertCount);
            return result;
        } catch (Exception e) {
            log.error("syncDailyStats failed", e);
            result.put("success", false);
            result.put("date", queryDate.toString());
            result.put("message", e.getMessage());
            return result;
        }
    }

    @Override
    public Map<String, Object> listDailyStats(String date, Integer days) {
        Map<String, Object> result = new HashMap<>();
        int limit = (days == null || days <= 0) ? 20 : days;
        LocalDate queryDate = resolveWuyangDate(date);
        try {
            List<MarketDailyStat> list = marketDailyStatDao.listRecent(Math.max(limit, 20));
            list.sort((a, b) -> b.getDataDate().compareTo(a.getDataDate()));

            List<MarketDailyStat> filtered = new ArrayList<>();
            for (MarketDailyStat s : list) {
                if (s.getDataDate() == null) continue;
                if (!s.getDataDate().isAfter(queryDate)) {
                    filtered.add(s);
                }
                if (filtered.size() >= limit) break;
            }

            result.put("success", true);
            result.put("date", queryDate.toString());
            result.put("count", filtered.size());
            result.put("data", filtered);
            return result;
        } catch (Exception e) {
            log.error("listDailyStats failed", e);
            result.put("success", false);
            result.put("date", queryDate.toString());
            result.put("message", e.getMessage());
            return result;
        }
    }

    private LocalDate resolveWuyangDate(String dateParam) {
        LocalDate parsed = parseDate(dateParam);
        if (parsed.equals(LocalDate.now())) {
            String trade = fetchWuyangTradeDate();
            if (trade != null && !trade.trim().isEmpty()) {
                String t = trade.trim().replace("-", "");
                if (t.length() == 8) {
                    try {
                        return LocalDate.parse(t, DateTimeFormatter.BASIC_ISO_DATE);
                    } catch (Exception ignore) {
                        return parsed;
                    }
                }
            }
        }
        return parsed;
    }

    private String fetchWuyangTradeDate() {
        try {
            Request request = new Request.Builder()
                    .url(WUYANG_RECENTLY_TRADE_DATE_URL)
                    .addHeader("accept", "application/json, text/plain, */*")
                    .addHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    return null;
                }
                String body = response.body().string();
                JsonNode root = mapper.readTree(body);
                JsonNode data = root.get("data");
                if (data == null || data.isNull()) {
                    return null;
                }
                String v = data.asText();
                if (v == null) {
                    return null;
                }
                return v.replace("-", "");
            }
        } catch (Exception e) {
            log.warn("fetchWuyangTradeDate failed: {}", e.getMessage());
            return null;
        }
    }

    private LocalDate parseDate(String date) {
        if (date == null || date.trim().isEmpty()) {
            return LocalDate.now();
        }
        String d = date.trim();
        try {
            if (d.matches("\\d{8}")) {
                return LocalDate.parse(d, DateTimeFormatter.BASIC_ISO_DATE);
            }
            if (d.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE);
            }
        } catch (Exception ignore) {
            return LocalDate.now();
        }
        return LocalDate.now();
    }

    private JsonNode fetchTenDaysRoot(LocalDate queryDate) throws Exception {
        String dateStr = queryDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String url = String.format(WUYANG_REPLAYROBOT_JSON_URL_TEMPLATE, dateStr);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("accept", "application/json, text/plain, */*")
                .addHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new RuntimeException("HTTP " + response.code() + " " + response.message());
            }
            String body = response.body().string();
            return mapper.readTree(body);
        }
    }

    private List<MarketDailyStat> parseTenDays(JsonNode root) {
        try {
            JsonNode tenDaysNode = root.get("tenDays");
            if (tenDaysNode == null || tenDaysNode.isNull() || !tenDaysNode.isArray() || tenDaysNode.size() < 31) {
                return new ArrayList<>();
            }

            List<List<Object>> tenDays = mapper.convertValue(tenDaysNode, new TypeReference<List<List<Object>>>() {
            });
            if (tenDays.isEmpty()) {
                return new ArrayList<>();
            }

            List<Object> datesObj = tenDays.get(0);
            List<MarketDailyStat> out = new ArrayList<>();

            for (int j = 0; j < datesObj.size(); j++) {
                String dateStr = String.valueOf(datesObj.get(j));
                if (dateStr == null || dateStr.trim().isEmpty()) {
                    continue;
                }

                MarketDailyStat stat = new MarketDailyStat();
                LocalDate parsedDate = parseWuyangTenDaysDate(dateStr);
                if (parsedDate == null) {
                    continue;
                }
                stat.setDataDate(parsedDate);

                stat.setRisingCount(asInt(tenDays, 1, j));
                stat.setFallingCount(asInt(tenDays, 2, j));
                stat.setTotalZt(asInt(tenDays, 3, j));
                stat.setTotalDt(asInt(tenDays, 4, j));
                stat.setLbCount(asInt(tenDays, 5, j));
                stat.setUp10Percent(asInt(tenDays, 6, j));
                stat.setDown9Percent(asInt(tenDays, 7, j));
                stat.setYizi(asInt(tenDays, 8, j));

                Pair firstBoard = asPairCountRate(tenDays, 9, j);
                stat.setFirstBoardCount(firstBoard.count);
                stat.setFirstBoardRate(firstBoard.rate);

                Pair secondBoard = asPairCountRate(tenDays, 10, j);
                stat.setSecondBoardCount(secondBoard.count);
                stat.setSecondBoardRate(secondBoard.rate);

                Pair thirdBoard = asPairCountRate(tenDays, 11, j);
                stat.setThirdBoardCount(thirdBoard.count);
                stat.setThirdBoardRate(thirdBoard.rate);

                Pair fourthBoard = asPairCountRate(tenDays, 12, j);
                stat.setFourthBoardCount(fourthBoard.count);
                stat.setFourthBoardRate(fourthBoard.rate);

                Pair fifthBoardAbove = asPairCountRate(tenDays, 13, j);
                stat.setFifthBoardAboveCount(fifthBoardAbove.count);
                stat.setFifthBoardAboveRate(fifthBoardAbove.rate);

                stat.setZt925(asIntSafe(tenDays, 14, j));

                Pair before10 = asPairCountRate(tenDays, 15, j);
                stat.setBefore10Count(before10.count);
                stat.setBefore10Rate(before10.rate);

                Pair between101130 = asPairCountRate(tenDays, 16, j);
                stat.setBetween101130Count(between101130.count);
                stat.setBetween101130Rate(between101130.rate);

                Pair between1314 = asPairCountRate(tenDays, 17, j);
                stat.setBetween1314Count(between1314.count);
                stat.setBetween1314Rate(between1314.rate);

                Pair between1415 = asPairCountRate(tenDays, 18, j);
                stat.setBetween1415Count(between1415.count);
                stat.setBetween1415Rate(between1415.rate);

                stat.setOpenCount(asInt(tenDays, 19, j));
                stat.setFbRatio(asDecimal(tenDays, 20, j));
                stat.setOneToTwoRatio(asDecimal(tenDays, 21, j));
                stat.setTwoToThreeRatio(asDecimal(tenDays, 22, j));
                stat.setThreeToFourRatio(asDecimal(tenDays, 23, j));
                stat.setLbRatio(asDecimal(tenDays, 24, j));
                stat.setYesterdayLbRatio(asDecimal(tenDays, 25, j));

                stat.setZtAmount(asDecimal(tenDays, 26, j));
                stat.setTotalAmount(asDecimal(tenDays, 27, j));
                stat.setShAmount(asDecimal(tenDays, 28, j));
                stat.setChuangyeAmount(asDecimal(tenDays, 29, j));
                stat.setKcAmount(asDecimal(tenDays, 30, j));

                out.add(stat);
            }

            return out;
        } catch (Exception e) {
            log.error("parseTenDays failed", e);
            return new ArrayList<>();
        }
    }

    private LocalDate parseWuyangTenDaysDate(String dateStr) {
        if (dateStr == null) return null;
        String s = dateStr.trim();
        if (s.isEmpty()) return null;
        try {
            return LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception ignore) {
        }
        try {
            return LocalDate.parse(s, DateTimeFormatter.ofPattern("yyyy-M-d"));
        } catch (Exception ignore) {
            return null;
        }
    }

    private int asInt(List<List<Object>> tenDays, int idx, int j) {
        try {
            if (idx >= tenDays.size()) return 0;
            List<Object> row = tenDays.get(idx);
            if (row == null || j >= row.size()) return 0;
            Object v = row.get(j);
            if (v == null) return 0;
            String s = String.valueOf(v).trim();
            if (s.isEmpty()) return 0;
            if (s.contains(".")) {
                return (int) Double.parseDouble(s);
            }
            return Integer.parseInt(s);
        } catch (Exception ignore) {
            return 0;
        }
    }

    private int asIntSafe(List<List<Object>> tenDays, int idx, int j) {
        try {
            if (idx >= tenDays.size()) return 0;
            List<Object> row = tenDays.get(idx);
            if (row == null || j >= row.size()) return 0;
            Object v = row.get(j);
            if (v == null) return 0;
            String s = String.valueOf(v).trim();
            if (s.isEmpty()) return 0;
            if (s.contains(".")) {
                return (int) Double.parseDouble(s);
            }
            return Integer.parseInt(s);
        } catch (Exception ignore) {
            return 0;
        }
    }

    private BigDecimal asDecimal(List<List<Object>> tenDays, int idx, int j) {
        try {
            if (idx >= tenDays.size()) return BigDecimal.ZERO;
            List<Object> row = tenDays.get(idx);
            if (row == null || j >= row.size()) return BigDecimal.ZERO;
            Object v = row.get(j);
            if (v == null) return BigDecimal.ZERO;
            String s = String.valueOf(v).trim();
            if (s.isEmpty()) return BigDecimal.ZERO;
            return new BigDecimal(s);
        } catch (Exception ignore) {
            return BigDecimal.ZERO;
        }
    }

    private Pair asPairCountRate(List<List<Object>> tenDays, int idx, int j) {
        try {
            if (idx >= tenDays.size()) return new Pair(0, BigDecimal.ZERO);
            List<Object> row = tenDays.get(idx);
            int base = j * 2;
            if (row == null || base + 1 >= row.size()) return new Pair(0, BigDecimal.ZERO);
            int count = 0;
            BigDecimal rate = BigDecimal.ZERO;

            Object c = row.get(base);
            if (c != null) {
                String cs = String.valueOf(c).trim();
                if (!cs.isEmpty()) {
                    count = cs.contains(".") ? (int) Double.parseDouble(cs) : Integer.parseInt(cs);
                }
            }

            Object r = row.get(base + 1);
            if (r != null) {
                String rs = String.valueOf(r).trim();
                if (!rs.isEmpty()) {
                    rate = new BigDecimal(rs);
                }
            }

            return new Pair(count, rate);
        } catch (Exception ignore) {
            return new Pair(0, BigDecimal.ZERO);
        }
    }

    private static class Pair {
        final int count;
        final BigDecimal rate;

        Pair(int count, BigDecimal rate) {
            this.count = count;
            this.rate = rate == null ? BigDecimal.ZERO : rate;
        }
    }

    private static class ScoreResult {
        final int score;
        final String grade;
        final Map<String, Integer> components;

        ScoreResult(int score, String grade, Map<String, Integer> components) {
            this.score = score;
            this.grade = grade;
            this.components = components;
        }
    }

    private void applyScoreRules(List<MarketDailyStat> orderedByDateAsc) {
        if (orderedByDateAsc == null || orderedByDateAsc.isEmpty()) return;

        for (int i = 0; i < orderedByDateAsc.size(); i++) {
            MarketDailyStat prev = i > 0 ? orderedByDateAsc.get(i - 1) : null;
            MarketDailyStat cur = orderedByDateAsc.get(i);
            ScoreResult sr = calculateMarketScore(prev, cur);
            cur.setMarketScore(sr.score);
            cur.setScoreGrade(sr.grade);
            try {
                cur.setScoreComponents(mapper.writeValueAsString(sr.components));
            } catch (Exception ignore) {
                cur.setScoreComponents("{}");
            }
        }
    }

    private ScoreResult calculateMarketScore(MarketDailyStat prev, MarketDailyStat cur) {
        Map<String, Integer> c = new LinkedHashMap<>();

        int rising = safeInt(cur.getRisingCount());
        int falling = safeInt(cur.getFallingCount());
        int totalZt = safeInt(cur.getTotalZt());
        int totalDt = safeInt(cur.getTotalDt());
        int open = safeInt(cur.getOpenCount());

        int firstBoard = safeInt(cur.getFirstBoardCount());
        int secondBoard = safeInt(cur.getSecondBoardCount());
        int thirdBoard = safeInt(cur.getThirdBoardCount());
        int fourthBoard = safeInt(cur.getFourthBoardCount());
        int fifthPlus = safeInt(cur.getFifthBoardAboveCount());

        int before10 = safeInt(cur.getBefore10Count());
        int between101130 = safeInt(cur.getBetween101130Count());
        int between1314 = safeInt(cur.getBetween1314Count());
        int between1415 = safeInt(cur.getBetween1415Count());

        BigDecimal fbRatio = safeDecimal(cur.getFbRatio());
        BigDecimal lbRatio = safeDecimal(cur.getLbRatio());
        BigDecimal yesterdayLbRatio = safeDecimal(cur.getYesterdayLbRatio());

        BigDecimal ztAmount = safeDecimal(cur.getZtAmount());
        BigDecimal totalAmount = safeDecimal(cur.getTotalAmount());
        BigDecimal chuangyeAmount = safeDecimal(cur.getChuangyeAmount());
        BigDecimal kcAmount = safeDecimal(cur.getKcAmount());

        int totalStocks = rising + falling;
        BigDecimal risingPct = percentOf(rising, totalStocks);

        BigDecimal ztDtRatio = totalDt <= 0 ? BigDecimal.valueOf(999) :
                BigDecimal.valueOf(totalZt).divide(BigDecimal.valueOf(totalDt), 2, RoundingMode.HALF_UP);

        BigDecimal ztAmountPct = percentOf(ztAmount, totalAmount);
        BigDecimal before10Pct = percentOf(before10, totalZt);
        BigDecimal morningPct = percentOf(before10 + between101130, totalZt);
        BigDecimal after2Pct = percentOf(between1415, totalZt);
        BigDecimal zhabanPct = percentOf(open, totalZt + open);

        // 市场广度（30分）
        int sRise;
        double rp = risingPct.doubleValue();
        if (rp >= 80) sRise = 15;
        else if (rp >= 60) sRise = 12;
        else if (rp >= 40) sRise = 8;
        else if (rp >= 20) sRise = 4;
        else sRise = 1;
        c.put("上涨家数占比", sRise);

        int sZtDt;
        double rd = ztDtRatio.doubleValue();
        if (rd > 5) sZtDt = 15;
        else if (rd >= 3) sZtDt = 12;
        else if (rd >= 1) sZtDt = 8;
        else if (rd >= 0.5) sZtDt = 4;
        else sZtDt = 1;
        c.put("涨停跌停比", sZtDt);

        // 赚钱效应（40分）
        int sFb;
        double fb = fbRatio.doubleValue();
        if (fb >= 85) sFb = 10;
        else if (fb >= 75) sFb = 8;
        else if (fb >= 65) sFb = 6;
        else if (fb >= 55) sFb = 4;
        else sFb = 2;
        c.put("封板率", sFb);

        int tiers = 0;
        if (secondBoard > 0) tiers++;
        if (thirdBoard > 0) tiers++;
        if (fourthBoard > 0) tiers++;
        if (fifthPlus > 0) tiers++;

        int sLadder;
        if (fifthPlus > 0 && tiers >= 4) sLadder = 15;
        else if (fourthBoard > 0 && tiers >= 3) sLadder = 12;
        else if (thirdBoard > 0 && tiers >= 2) sLadder = 9;
        else if (firstBoard > 0 || secondBoard > 0) sLadder = 6;
        else sLadder = 2;
        c.put("连板梯队完整性", sLadder);

        int sPromote;
        double lbr = lbRatio.doubleValue();
        if (lbr > 60) sPromote = 15;
        else if (lbr >= 40) sPromote = 12;
        else if (lbr >= 25) sPromote = 8;
        else if (lbr >= 10) sPromote = 4;
        else sPromote = 1;
        c.put("连板晋级率", sPromote);

        // 资金活跃度（30分）
        int sZtAmount;
        double zr = ztAmountPct.doubleValue();
        if (zr >= 8) sZtAmount = 15;
        else if (zr >= 5) sZtAmount = 12;
        else if (zr >= 3) sZtAmount = 8;
        else if (zr >= 1) sZtAmount = 4;
        else sZtAmount = 1;
        c.put("涨停金额/总成交额", sZtAmount);

        int sIntraday;
        double bp = before10Pct.doubleValue();
        double mp = morningPct.doubleValue();
        double ap = after2Pct.doubleValue();
        if (bp > 40) {
            sIntraday = 15;
        } else if (mp > 60) {
            sIntraday = 12;
        } else {
            double p1 = ratioOf(before10, totalZt);
            double p2 = ratioOf(between101130, totalZt);
            double p3 = ratioOf(between1314, totalZt);
            double p4 = ratioOf(between1415, totalZt);
            double max = Math.max(Math.max(p1, p2), Math.max(p3, p4));
            double min = Math.min(Math.min(p1, p2), Math.min(p3, p4));
            boolean even = (max - min) <= 0.15;

            if (even) {
                sIntraday = 8;
            } else {
                double afternoon = p3 + p4;
                if (p4 > 0.4) sIntraday = 2;
                else if (afternoon > 0.6) sIntraday = 4;
                else sIntraday = 8;
            }
        }
        c.put("分时资金进攻持续性", sIntraday);

        int baseScore = sRise + sZtDt + sFb + sLadder + sPromote + sZtAmount + sIntraday;
        c.put("基础分", baseScore);

        // 附加项（±10）
        int adjust = 0;

        if (yesterdayLbRatio.doubleValue() > 80) {
            adjust += 3;
            c.put("加分:昨日连板率>80", 3);
        }

        boolean heightBreak = false;
        if (prev != null) {
            int prevFifth = safeInt(prev.getFifthBoardAboveCount());
            int prevFourth = safeInt(prev.getFourthBoardCount());
            if (fifthPlus > 0 && fifthPlus > prevFifth) heightBreak = true;
            if (!heightBreak && fourthBoard > 0 && prevFourth == 0) heightBreak = true;
        } else {
            heightBreak = fifthPlus > 0;
        }
        if (heightBreak) {
            adjust += 3;
            c.put("加分:高度突破", 3);
        }

        if (prev != null) {
            BigDecimal prevAmount = safeDecimal(prev.getTotalAmount());
            if (prevAmount.compareTo(BigDecimal.ZERO) > 0 && totalAmount.compareTo(prevAmount.multiply(BigDecimal.valueOf(1.2))) >= 0) {
                adjust += 2;
                c.put("加分:成交额放大>=20%", 2);
            }
        }

        if (chuangyeAmount.doubleValue() >= 8000 || kcAmount.doubleValue() >= 3500) {
            adjust += 2;
            c.put("加分:创/科成交活跃", 2);
        }

        if (totalDt > 20) {
            adjust -= 5;
            c.put("减分:跌停>20", -5);
        }

        if (zhabanPct.doubleValue() > 40) {
            adjust -= 3;
            c.put("减分:炸板率>40%", -3);
        }

        if (after2Pct.doubleValue() < 5) {
            adjust -= 2;
            c.put("减分:14点后涨停<5%", -2);
        }

        adjust = Math.max(-10, Math.min(10, adjust));
        c.put("调整分", adjust);

        int finalScore = baseScore + adjust;
        finalScore = Math.max(1, Math.min(100, finalScore));
        c.put("综合评分", finalScore);

        return new ScoreResult(finalScore, getScoreGrade(finalScore), c);
    }

    private BigDecimal percentOf(int part, int total) {
        if (total <= 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(part).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal percentOf(BigDecimal part, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        if (part == null) part = BigDecimal.ZERO;
        return part.multiply(BigDecimal.valueOf(100)).divide(total, 4, RoundingMode.HALF_UP);
    }

    private double ratioOf(int part, int total) {
        if (total <= 0) return 0.0;
        return part * 1.0 / total;
    }

    private String getScoreGrade(int score) {
        if (score >= 90) return "A+";
        if (score >= 80) return "A";
        if (score >= 70) return "B+";
        if (score >= 60) return "B";
        if (score >= 50) return "C+";
        if (score >= 40) return "C";
        if (score >= 30) return "D";
        return "E";
    }

    private int safeInt(Integer v) {
        return v == null ? 0 : v;
    }

    private BigDecimal safeDecimal(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private void applyStageRules(List<MarketDailyStat> orderedByDateAsc) {
        if (orderedByDateAsc == null || orderedByDateAsc.isEmpty()) return;

        for (int i = 0; i < orderedByDateAsc.size(); i++) {
            MarketDailyStat prev = i > 0 ? orderedByDateAsc.get(i - 1) : null;
            MarketDailyStat cur = orderedByDateAsc.get(i);
            BigDecimal recentAvgAmount = calcRecentAvgAmount(orderedByDateAsc, i, 10);
            BigDecimal recentMaxAmount = calcRecentMaxAmount(orderedByDateAsc, i, 20);
            StageResult sr = judgeStage(prev, cur, recentAvgAmount, recentMaxAmount);
            cur.setStageCode(sr.code);
            cur.setStageName(sr.name);
            cur.setStageReason(sr.reason);
        }
    }

    private BigDecimal calcRecentAvgAmount(List<MarketDailyStat> list, int endIndexInclusive, int window) {
        if (list == null || list.isEmpty()) return BigDecimal.ZERO;
        int end = Math.min(endIndexInclusive, list.size() - 1);
        int start = Math.max(0, end - Math.max(1, window) + 1);

        BigDecimal sum = BigDecimal.ZERO;
        int n = 0;
        for (int i = start; i <= end; i++) {
            BigDecimal v = safeDecimal(list.get(i).getTotalAmount());
            if (v.compareTo(BigDecimal.ZERO) > 0) {
                sum = sum.add(v);
                n++;
            }
        }
        if (n <= 0) return BigDecimal.ZERO;
        return sum.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcRecentMaxAmount(List<MarketDailyStat> list, int endIndexInclusive, int window) {
        if (list == null || list.isEmpty()) return BigDecimal.ZERO;
        int end = Math.min(endIndexInclusive, list.size() - 1);
        int start = Math.max(0, end - Math.max(1, window) + 1);

        BigDecimal max = BigDecimal.ZERO;
        for (int i = start; i <= end; i++) {
            BigDecimal v = safeDecimal(list.get(i).getTotalAmount());
            if (v.compareTo(max) > 0) {
                max = v;
            }
        }
        return max;
    }

    private int calcHeight(MarketDailyStat s) {
        if (s == null) return 0;
        if (safeInt(s.getFifthBoardAboveCount()) > 0) return 5;
        if (safeInt(s.getFourthBoardCount()) > 0) return 4;
        if (safeInt(s.getThirdBoardCount()) > 0) return 3;
        if (safeInt(s.getSecondBoardCount()) > 0) return 2;
        if (safeInt(s.getFirstBoardCount()) > 0) return 1;
        return 0;
    }

    private static class StageResult {
        final String code;
        final String name;
        final String reason;

        StageResult(String code, String name, String reason) {
            this.code = code;
            this.name = name;
            this.reason = reason;
        }
    }

    private StageResult judgeStage(MarketDailyStat prev, MarketDailyStat cur, BigDecimal recentAvgAmount, BigDecimal recentMaxAmount) {
        int score = safeInt(cur.getMarketScore());
        int rising = safeInt(cur.getRisingCount());
        int totalZt = safeInt(cur.getTotalZt());
        int totalDt = safeInt(cur.getTotalDt());
        int down9 = safeInt(cur.getDown9Percent());

        int firstBoard = safeInt(cur.getFirstBoardCount());
        int secondBoard = safeInt(cur.getSecondBoardCount());
        int thirdBoard = safeInt(cur.getThirdBoardCount());
        int fourthBoard = safeInt(cur.getFourthBoardCount());
        int fifthPlus = safeInt(cur.getFifthBoardAboveCount());

        int height = calcHeight(cur);
        int prevHeight = calcHeight(prev);

        int tiers = 0;
        if (secondBoard > 0) tiers++;
        if (thirdBoard > 0) tiers++;
        if (fourthBoard > 0) tiers++;
        if (fifthPlus > 0) tiers++;

        BigDecimal fb = safeDecimal(cur.getFbRatio());
        BigDecimal lbRatio = safeDecimal(cur.getLbRatio());
        BigDecimal yesterdayLbRatio = safeDecimal(cur.getYesterdayLbRatio());
        BigDecimal oneToTwo = safeDecimal(cur.getOneToTwoRatio());
        BigDecimal twoToThree = safeDecimal(cur.getTwoToThreeRatio());

        BigDecimal totalAmount = safeDecimal(cur.getTotalAmount());
        BigDecimal prevAmount = prev == null ? BigDecimal.ZERO : safeDecimal(prev.getTotalAmount());
        boolean amountUp20 = prevAmount.compareTo(BigDecimal.ZERO) > 0 && totalAmount.compareTo(prevAmount.multiply(BigDecimal.valueOf(1.2))) >= 0;

        boolean amountBelow60pctAvg = recentAvgAmount != null && recentAvgAmount.compareTo(BigDecimal.ZERO) > 0
                && totalAmount.compareTo(recentAvgAmount.multiply(BigDecimal.valueOf(0.6))) < 0;
        boolean amountShrink = recentAvgAmount != null && recentAvgAmount.compareTo(BigDecimal.ZERO) > 0
                && totalAmount.compareTo(recentAvgAmount) < 0;

        boolean ztAfter14Low = percentOf(safeInt(cur.getBetween1415Count()), totalZt).doubleValue() < 5;
        boolean zhabanHigh = percentOf(safeInt(cur.getOpenCount()), totalZt + safeInt(cur.getOpenCount())).doubleValue() > 40;

        boolean promoUp = lbRatio.compareTo(yesterdayLbRatio) > 0 || oneToTwo.compareTo(BigDecimal.ZERO) > 0 || twoToThree.compareTo(BigDecimal.ZERO) > 0;

        String reason = buildStageReason(cur, prev, recentAvgAmount, recentMaxAmount, height, tiers, amountUp20, amountBelow60pctAvg, ztAfter14Low, zhabanHigh);

        // A阶段：困境筑底期（评分1-30分）
        if (score <= 30) {
            if (score <= 10) {
                if (amountBelow60pctAvg && totalZt < 30) return new StageResult("A1", "A1困境筑底", reason);
                return new StageResult("A1", "A1困境筑底", reason);
            }
            if (score <= 20) {
                if (totalDt > 15 && rising < 800) return new StageResult("A2", "A2弱势承压", reason);
                return new StageResult("A2", "A2弱势承压", reason);
            }
            if (score <= 25) {
                if (totalDt > 30) return new StageResult("A3", "A3恐慌蔓延", reason);
                return new StageResult("A3", "A3恐慌蔓延", reason);
            }
            if (amountShrink && fb.doubleValue() > 70) return new StageResult("A4", "A4企稳酝酿", reason);
            return new StageResult("A4", "A4企稳酝酿", reason);
        }

        // B阶段：趋势转折期（评分31-55分）
        if (score <= 55) {
            if (score <= 40) {
                if (firstBoard > 30 && lbRatio.doubleValue() > 40) return new StageResult("B1", "B1信号初现", reason);
                return new StageResult("B1", "B1信号初现", reason);
            }
            if (score <= 45) {
                if (height >= 3) return new StageResult("B2", "B2弱转强", reason);
                return new StageResult("B2", "B2弱转强", reason);
            }
            if (score <= 50) {
                if (amountUp20 && totalZt > 50) return new StageResult("B3", "B3确认突破", reason);
                return new StageResult("B3", "B3确认突破", reason);
            }
            if (fb.doubleValue() > 80 && promoUp) return new StageResult("B4", "B4分歧修复", reason);
            return new StageResult("B4", "B4分歧修复", reason);
        }

        // C阶段：顺畅主升期（评分56-80分）
        if (score <= 80) {
            if (score <= 65) {
                if (height >= 5 && totalZt > 80) return new StageResult("C1", "C1加速启动", reason);
                return new StageResult("C1", "C1加速启动", reason);
            }
            if (score <= 70) {
                if (tiers >= 4) return new StageResult("C2", "C2鼎盛延续", reason);
                return new StageResult("C2", "C2鼎盛延续", reason);
            }
            if (score <= 75) {
                if (fb.doubleValue() > 85 && zhabanHigh) return new StageResult("C3", "C3分歧健康", reason);
                return new StageResult("C3", "C3分歧健康", reason);
            }

            boolean amountNewHigh = recentMaxAmount != null && recentMaxAmount.compareTo(BigDecimal.ZERO) > 0 && totalAmount.compareTo(recentMaxAmount) >= 0;
            boolean lbRatioDown = prev != null && lbRatio.compareTo(safeDecimal(prev.getLbRatio())) < 0;
            if (amountNewHigh && lbRatioDown) return new StageResult("C4", "C4盛极将衰", reason);
            return new StageResult("C4", "C4盛极将衰", reason);
        }

        // D阶段：盛极转衰期（评分81-100分）
        if (score <= 85) {
            boolean leaderCrash = totalDt > 20 && prevHeight >= 4 && height < prevHeight;
            if (leaderCrash) return new StageResult("D1", "D1退潮信号", reason);
            return new StageResult("D1", "D1退潮信号", reason);
        }
        if (score <= 90) {
            boolean heightDown = prevHeight >= 3 && height < prevHeight;
            if (rising < 1000 && heightDown) return new StageResult("D2", "D2持续杀跌", reason);
            return new StageResult("D2", "D2持续杀跌", reason);
        }
        if (score <= 95) {
            if (down9 > 30) return new StageResult("D3", "D3恐慌扩散", reason);
            return new StageResult("D3", "D3恐慌扩散", reason);
        }

        if (fb.doubleValue() > 70 && rising < 1200) return new StageResult("D4", "D4循环重启", reason);
        return new StageResult("D4", "D4循环重启", reason);
    }

    private String buildStageReason(MarketDailyStat cur, MarketDailyStat prev, BigDecimal recentAvgAmount, BigDecimal recentMaxAmount,
                                   int height, int tiers, boolean amountUp20, boolean amountBelow60pctAvg, boolean ztAfter14Low, boolean zhabanHigh) {
        StringBuilder sb = new StringBuilder();
        sb.append("score=").append(safeInt(cur.getMarketScore()))
                .append(",rising=").append(safeInt(cur.getRisingCount()))
                .append(",falling=").append(safeInt(cur.getFallingCount()))
                .append(",zt=").append(safeInt(cur.getTotalZt()))
                .append(",dt=").append(safeInt(cur.getTotalDt()))
                .append(",lb=").append(safeInt(cur.getLbCount()))
                .append(",up10=").append(safeInt(cur.getUp10Percent()))
                .append(",down9=").append(safeInt(cur.getDown9Percent()))
                .append(",yizi=").append(safeInt(cur.getYizi()))
                .append(",zt925=").append(safeInt(cur.getZt925()))
                .append(",first=").append(safeInt(cur.getFirstBoardCount()))
                .append(",second=").append(safeInt(cur.getSecondBoardCount()))
                .append(",third=").append(safeInt(cur.getThirdBoardCount()))
                .append(",fourth=").append(safeInt(cur.getFourthBoardCount()))
                .append(",fifthPlus=").append(safeInt(cur.getFifthBoardAboveCount()))
                .append(",height=").append(height)
                .append(",tiers=").append(tiers)
                .append(",before10=").append(safeInt(cur.getBefore10Count()))
                .append(",10_1130=").append(safeInt(cur.getBetween101130Count()))
                .append(",13_14=").append(safeInt(cur.getBetween1314Count()))
                .append(",14_15=").append(safeInt(cur.getBetween1415Count()))
                .append(",open=").append(safeInt(cur.getOpenCount()))
                .append(",fb=").append(safeDecimal(cur.getFbRatio()))
                .append(",oneToTwo=").append(safeDecimal(cur.getOneToTwoRatio()))
                .append(",twoToThree=").append(safeDecimal(cur.getTwoToThreeRatio()))
                .append(",threeToFour=").append(safeDecimal(cur.getThreeToFourRatio()))
                .append(",lbRatio=").append(safeDecimal(cur.getLbRatio()))
                .append(",yLbRatio=").append(safeDecimal(cur.getYesterdayLbRatio()))
                .append(",ztAmount=").append(safeDecimal(cur.getZtAmount()))
                .append(",amount=").append(safeDecimal(cur.getTotalAmount()))
                .append(",avgAmount10=").append(recentAvgAmount == null ? BigDecimal.ZERO : recentAvgAmount)
                .append(",maxAmount20=").append(recentMaxAmount == null ? BigDecimal.ZERO : recentMaxAmount)
                .append(",amountUp20=").append(amountUp20)
                .append(",amountBelow60pctAvg=").append(amountBelow60pctAvg)
                .append(",zhabanHigh=").append(zhabanHigh)
                .append(",after14Low=").append(ztAfter14Low);

        if (prev != null) {
            sb.append(",prevAmount=").append(safeDecimal(prev.getTotalAmount()))
                    .append(",prevHeight=").append(calcHeight(prev))
                    .append(",prevLbRatio=").append(safeDecimal(prev.getLbRatio()));
        }
        return sb.toString();
    }

    private void fillRawJson(List<MarketDailyStat> orderedByDateAsc) {
        if (orderedByDateAsc == null) return;
        for (MarketDailyStat s : orderedByDateAsc) {
            try {
                s.setRawJson(mapper.writeValueAsString(s));
            } catch (Exception ignore) {
                s.setRawJson("{}");
            }
        }
    }
}
