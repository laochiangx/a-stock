package com.example.stock.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.stock.dao.StockDailySnapshotDao;
import com.example.stock.dao.StockDailySnapshotTagDao;
import com.example.stock.dao.TagsDao;
import com.example.stock.entity.StockDailySnapshot;
import com.example.stock.entity.StockDailySnapshotTag;
import com.example.stock.entity.Tags;
import com.example.stock.service.StockTaggingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/stock-tags")
@CrossOrigin
public class StockTagController {

    @Autowired
    private StockDailySnapshotDao stockDailySnapshotDao;

    @Autowired
    private StockDailySnapshotTagDao stockDailySnapshotTagDao;

    @Autowired
    private TagsDao tagsDao;

    @Autowired
    private StockTaggingService stockTaggingService;

    @GetMapping("/daily/list")
    public Map<String, Object> list(@RequestParam(required = false) String date,
                                    @RequestParam(required = false) String source,
                                    @RequestParam(required = false) Long tagId,
                                    @RequestParam(required = false) String keyword,
                                    @RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "50") int pageSize) {
        Map<String, Object> result = new HashMap<>();
        try {
            LocalDate d = (date == null || date.isEmpty()) ? LocalDate.now() : LocalDate.parse(date);

            LambdaQueryWrapper<StockDailySnapshot> qw = new LambdaQueryWrapper<>();
            qw.eq(StockDailySnapshot::getDataDate, d);
            if (keyword != null && !keyword.isEmpty()) {
                qw.and(w -> w.like(StockDailySnapshot::getStockCode, keyword)
                        .or().like(StockDailySnapshot::getStockName, keyword));
            }
            if (source != null && !source.isEmpty()) {
                qw.like(StockDailySnapshot::getSources, source);
            }
            qw.orderByDesc(StockDailySnapshot::getUpdatedAt).orderByAsc(StockDailySnapshot::getId);

            Page<StockDailySnapshot> p = stockDailySnapshotDao.selectPage(new Page<>(page, pageSize), qw);
            List<Map<String, Object>> rows = new ArrayList<>();

            for (StockDailySnapshot s : p.getRecords()) {
                List<Tags> tags = getTagsBySnapshotId(s.getId());
                if (tagId != null) {
                    boolean has = false;
                    for (Tags t : tags) {
                        if (tagId.equals(t.getId())) {
                            has = true;
                            break;
                        }
                    }
                    if (!has) {
                        continue;
                    }
                }
                Map<String, Object> row = new HashMap<>();
                row.put("snapshot", s);
                row.put("tags", tags);
                rows.add(row);
            }

            result.put("success", true);
            result.put("date", d.toString());
            result.put("list", rows);
            result.put("total", p.getTotal());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PostMapping("/daily/set-tags")
    public Map<String, Object> setTags(@RequestBody Map<String, Object> req) {
        Map<String, Object> result = new HashMap<>();
        try {
            String date = (String) req.get("date");
            String stockCode = req.get("stockCode") == null ? null : String.valueOf(req.get("stockCode"));
            String stockName = req.get("stockName") == null ? null : String.valueOf(req.get("stockName"));

            if (stockCode == null || stockCode.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "stockCode is required");
                return result;
            }

            LocalDate d = (date == null || date.isEmpty()) ? LocalDate.now() : LocalDate.parse(date);
            String code = stockCode.trim();

            StockDailySnapshot snapshot = stockDailySnapshotDao.selectOne(
                    new LambdaQueryWrapper<StockDailySnapshot>()
                            .eq(StockDailySnapshot::getDataDate, d)
                            .eq(StockDailySnapshot::getStockCode, code)
            );

            LocalDateTime now = LocalDateTime.now();
            if (snapshot == null) {
                snapshot = new StockDailySnapshot();
                snapshot.setDataDate(d);
                snapshot.setStockCode(code);
                snapshot.setStockName(stockName);
                snapshot.setSources("manual");
                snapshot.setCreatedAt(now);
                snapshot.setUpdatedAt(now);
                snapshot.setDeleted(0);
                stockDailySnapshotDao.insert(snapshot);
            } else {
                if (stockName != null && !stockName.isEmpty()) {
                    snapshot.setStockName(stockName);
                }
                snapshot.setUpdatedAt(now);
                stockDailySnapshotDao.updateById(snapshot);
            }

            @SuppressWarnings("unchecked")
            List<Object> tagIdsRaw = (List<Object>) req.get("tagIds");
            List<Long> tagIds = new ArrayList<>();
            if (tagIdsRaw != null) {
                for (Object o : tagIdsRaw) {
                    if (o instanceof Number) {
                        tagIds.add(((Number) o).longValue());
                    } else {
                        try {
                            tagIds.add(Long.parseLong(String.valueOf(o)));
                        } catch (Exception ignore) {
                        }
                    }
                }
            }

            stockDailySnapshotTagDao.delete(new LambdaQueryWrapper<StockDailySnapshotTag>()
                    .eq(StockDailySnapshotTag::getSnapshotId, snapshot.getId()));

            int inserted = 0;
            for (Long tid : tagIds) {
                if (tid == null) continue;
                StockDailySnapshotTag rel = new StockDailySnapshotTag();
                rel.setSnapshotId(snapshot.getId());
                rel.setTagId(tid);
                rel.setCreatedAt(now);
                rel.setUpdatedAt(now);
                rel.setDeleted(0);
                stockDailySnapshotTagDao.insert(rel);
                inserted++;
            }

            result.put("success", true);
            result.put("inserted", inserted);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PostMapping("/run")
    public Map<String, Object> run(@RequestParam(required = false) String date) {
        LocalDate d = (date == null || date.isEmpty()) ? LocalDate.now() : LocalDate.parse(date);
        return stockTaggingService.tagByDate(d);
    }

    private List<Tags> getTagsBySnapshotId(Long snapshotId) {
        if (snapshotId == null) {
            return new ArrayList<>();
        }
        List<StockDailySnapshotTag> rels = stockDailySnapshotTagDao.selectList(
                new LambdaQueryWrapper<StockDailySnapshotTag>()
                        .eq(StockDailySnapshotTag::getSnapshotId, snapshotId)
        );

        if (rels.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> ids = new ArrayList<>();
        for (StockDailySnapshotTag r : rels) {
            if (r.getTagId() != null) {
                ids.add(r.getTagId());
            }
        }
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }

        List<Tags> tags = tagsDao.selectBatchIds(ids);
        tags.sort((a, b) -> {
            int pa = a.getPriority() == null ? 0 : a.getPriority();
            int pb = b.getPriority() == null ? 0 : b.getPriority();
            return Integer.compare(pb, pa);
        });
        return tags;
    }
}
