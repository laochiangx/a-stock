package com.example.stock.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.stock.dao.MenuConfigDao;
import com.example.stock.entity.MenuConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/menu")
@CrossOrigin
public class MenuConfigController {

    @Autowired
    private MenuConfigDao menuConfigDao;

    /**
     * 初始化默认菜单配置
     */
    @PostConstruct
    public void initDefaultMenus() {
        try {
            long count = menuConfigDao.selectCount(null);
            if (count == 0) {
                List<MenuConfig> defaultMenus = new ArrayList<>();
                int sort = 0;

                // 股票自选
                defaultMenus.add(createMenu("stock", "股票自选", "StarOutline", "/", null, sort++, true, true));

                // 市场行情
                defaultMenus.add(createMenu("market", "市场行情", "NewspaperOutline", "/market", null, sort++, true, true));
                defaultMenus.add(createMenu("market1", "市场快讯", "NewspaperSharp", "/market?name=市场快讯", "market", 0, true, false));
                defaultMenus.add(createMenu("market2", "全球股指", "BarChartSharp", "/market?name=全球股指", "market", 1, true, false));
                defaultMenus.add(createMenu("market3", "重大指数", "AnalyticsOutline", "/market?name=重大指数", "market", 2, true, false));
                defaultMenus.add(createMenu("market4", "行业排名", "Flag", "/market?name=行业排名", "market", 3, true, false));
                defaultMenus.add(createMenu("market5", "个股资金流向", "Pulse", "/market?name=个股资金流向", "market", 4, true, false));
                defaultMenus.add(createMenu("market6", "龙虎榜", "Dragon", "/market?name=龙虎榜", "market", 5, true, false));
                defaultMenus.add(createMenu("market7", "个股研报", "StockOutlined", "/market?name=个股研报", "market", 6, true, false));
                defaultMenus.add(createMenu("market8", "公司公告", "NotificationFilled", "/market?name=公司公告", "market", 7, true, false));
                defaultMenus.add(createMenu("market9", "行业研究", "ReportSearch", "/market?name=行业研究", "market", 8, true, false));
                defaultMenus.add(createMenu("market10", "当前热门", "Gripfire", "/market?name=当前热门", "market", 9, true, false));
                defaultMenus.add(createMenu("market11", "指标选股", "BoxSearch20Regular", "/market?name=指标选股", "market", 10, true, false));
                defaultMenus.add(createMenu("market12", "名站优选", "FirefoxBrowser", "/market?name=名站优选", "market", 11, true, false));

                // 题材板块
                defaultMenus.add(createMenu("themePlate", "题材板块", "LayersOutline", "/theme-plate", null, sort++, true, false));

                // 复盘
                defaultMenus.add(createMenu("review", "复盘", "CalendarOutline", "/review", null, sort++, true, false));

                // 龙头挖掘
                defaultMenus.add(createMenu("leaderMining", "龙头挖掘", "SwapHorizontalOutline", "/leader-mining", null, sort++, true, false));

                // 龙虎榜
                defaultMenus.add(createMenu("thsLongTiger", "龙虎榜", "FlashOutline", "/ths-longtiger", null, sort++, true, false));

                // 热榜
                defaultMenus.add(createMenu("hotRank", "热榜", "TrendingUpOutline", "/hot-rank", null, sort++, true, false));

                // 人气聚合
                defaultMenus.add(createMenu("popularityRank", "人气聚合", "PeopleOutline", "/popularity-rank", null, sort++, true, false));

                // 市场数据
                defaultMenus.add(createMenu("marketData", "市场数据", "StatsChartOutline", "/market-data", null, sort++, true, false));

                // 连板天梯
                defaultMenus.add(createMenu("tdxLadder", "连板天梯", "StatsChartOutline", "/tdx-ladder", null, sort++, true, false));
                defaultMenus.add(createMenu("tdxLadderOrig", "连板天梯（原）", "StatsChartOutline", "/tdx-ladder-orig", null, sort++, true, false));

                // 基金自选
                defaultMenus.add(createMenu("fund", "基金自选", "SparklesOutline", "/fund", null, sort++, false, false));

                // AI智能体
                defaultMenus.add(createMenu("agent", "AI智能体", "Robot", "/agent", null, sort++, false, false));

                // 系统管理（父菜单）
                defaultMenus.add(createMenu("system", "系统管理", "SettingsOutline", null, null, 100, true, true));
                defaultMenus.add(createMenu("menuManage", "菜单管理", "ReorderTwoOutline", "/menu-manage", "system", 0, true, true));
                defaultMenus.add(createMenu("wordDictionary", "词典管理", "CommentNote20Filled", "/word-dictionary", "system", 1, true, false));
                defaultMenus.add(createMenu("tagManage", "标签管理", "BonfireOutline", "/tag-manage", "system", 2, true, false));
                defaultMenus.add(createMenu("stockTagDaily", "每日热门&打标", "FlameSharp", "/stock-tag-daily", "system", 3, true, false));
                defaultMenus.add(createMenu("settings", "设置", "SettingsOutline", "/settings", "system", 4, true, true));
                defaultMenus.add(createMenu("about", "关于", "LogoGithub", "/about", "system", 5, true, true));

                for (MenuConfig menu : defaultMenus) {
                    menuConfigDao.insert(menu);
                }
                System.out.println("初始化默认菜单配置完成，共 " + defaultMenus.size() + " 个菜单");
            } else {
                ensureMenuExists(createMenu("system", "系统管理", "SettingsOutline", null, null, 100, true, true));
                ensureMenuExists(createMenu("tagManage", "标签管理", "BonfireOutline", "/tag-manage", "system", 2, true, false));
                ensureMenuExists(createMenu("stockTagDaily", "每日热门&打标", "FlameSharp", "/stock-tag-daily", "system", 3, true, false));
                ensureMenuExists(createMenu("marketData", "市场数据", "StatsChartOutline", "/market-data", null, 0, true, false));
                ensureMenuExists(createMenu("stockOverview", "综合信息", "InformationOutline", "/stock-overview", null, 0, true, false));
                ensureMenuExists(createMenu("tdxLadderOrig", "连板天梯（原）", "StatsChartOutline", "/tdx-ladder-orig", null, 0, true, false));
            }
        } catch (Exception e) {
            System.err.println("初始化菜单配置失败: " + e.getMessage());
        }
    }

    private void ensureMenuExists(MenuConfig menu) {
        if (menu == null || menu.getMenuKey() == null) {
            return;
        }
        try {
            MenuConfig existing = menuConfigDao.selectOne(
                    new LambdaQueryWrapper<MenuConfig>().eq(MenuConfig::getMenuKey, menu.getMenuKey())
            );
            if (existing != null) {
                return;
            }
            menuConfigDao.insert(menu);
        } catch (Exception e) {
            System.err.println("补齐菜单失败(" + menu.getMenuKey() + "): " + e.getMessage());
        }
    }

    private MenuConfig createMenu(String key, String name, String icon, String path, String parentKey, int sort, boolean visible, boolean system) {
        MenuConfig menu = new MenuConfig();
        menu.setMenuKey(key);
        menu.setMenuName(name);
        menu.setMenuIcon(icon);
        menu.setMenuPath(path);
        menu.setParentKey(parentKey);
        menu.setSortOrder(sort);
        menu.setIsVisible(visible);
        menu.setIsSystem(system);
        menu.setCreatedAt(LocalDateTime.now());
        menu.setUpdatedAt(LocalDateTime.now());
        return menu;
    }

    /**
     * 获取所有菜单配置（树形结构）
     */
    @GetMapping("/list")
    public Map<String, Object> getMenuList() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<MenuConfig> allMenus = menuConfigDao.selectList(
                new LambdaQueryWrapper<MenuConfig>()
                    .orderByAsc(MenuConfig::getSortOrder)
            );
            
            // 构建树形结构
            List<Map<String, Object>> tree = buildMenuTree(allMenus, null);
            
            result.put("success", true);
            result.put("list", allMenus);
            result.put("tree", tree);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取可见菜单（前端渲染用）
     */
    @GetMapping("/visible")
    public Map<String, Object> getVisibleMenus() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<MenuConfig> visibleMenus = menuConfigDao.selectList(
                new LambdaQueryWrapper<MenuConfig>()
                    .eq(MenuConfig::getIsVisible, true)
                    .orderByAsc(MenuConfig::getSortOrder)
            );
            
            // 构建树形结构
            List<Map<String, Object>> tree = buildMenuTree(visibleMenus, null);
            
            result.put("success", true);
            result.put("menus", tree);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    private List<Map<String, Object>> buildMenuTree(List<MenuConfig> menus, String parentKey) {
        List<Map<String, Object>> tree = new ArrayList<>();
        for (MenuConfig menu : menus) {
            boolean isMatch = (parentKey == null && menu.getParentKey() == null) ||
                             (parentKey != null && parentKey.equals(menu.getParentKey()));
            if (isMatch) {
                Map<String, Object> node = new HashMap<>();
                node.put("key", menu.getMenuKey());
                node.put("name", menu.getMenuName());
                node.put("icon", menu.getMenuIcon());
                node.put("path", menu.getMenuPath());
                node.put("visible", menu.getIsVisible());
                node.put("system", menu.getIsSystem());
                node.put("sort", menu.getSortOrder());
                node.put("id", menu.getId());
                
                List<Map<String, Object>> children = buildMenuTree(menus, menu.getMenuKey());
                if (!children.isEmpty()) {
                    node.put("children", children);
                }
                tree.add(node);
            }
        }
        return tree;
    }

    /**
     * 更新菜单可见性
     */
    @PostMapping("/toggle-visible")
    public Map<String, Object> toggleVisible(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        try {
            String menuKey = (String) request.get("menuKey");
            Boolean visible = (Boolean) request.get("visible");
            
            MenuConfig menu = menuConfigDao.selectOne(
                new LambdaQueryWrapper<MenuConfig>().eq(MenuConfig::getMenuKey, menuKey)
            );
            
            if (menu != null) {
                menu.setIsVisible(visible);
                menu.setUpdatedAt(LocalDateTime.now());
                menuConfigDao.updateById(menu);
                result.put("success", true);
                result.put("message", "更新成功");
            } else {
                result.put("success", false);
                result.put("message", "菜单不存在");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 更新菜单信息
     */
    @PostMapping("/update")
    public Map<String, Object> updateMenu(@RequestBody MenuConfig menu) {
        Map<String, Object> result = new HashMap<>();
        try {
            MenuConfig existing = menuConfigDao.selectById(menu.getId());
            if (existing != null) {
                existing.setMenuName(menu.getMenuName());
                existing.setMenuIcon(menu.getMenuIcon());
                existing.setMenuPath(menu.getMenuPath());
                existing.setSortOrder(menu.getSortOrder());
                existing.setIsVisible(menu.getIsVisible());
                existing.setUpdatedAt(LocalDateTime.now());
                menuConfigDao.updateById(existing);
                result.put("success", true);
                result.put("message", "更新成功");
            } else {
                result.put("success", false);
                result.put("message", "菜单不存在");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 批量更新菜单排序
     */
    @PostMapping("/update-sort")
    public Map<String, Object> updateSort(@RequestBody List<Map<String, Object>> sortList) {
        Map<String, Object> result = new HashMap<>();
        try {
            for (Map<String, Object> item : sortList) {
                String menuKey = (String) item.get("menuKey");
                Integer sortOrder = (Integer) item.get("sortOrder");
                
                MenuConfig menu = menuConfigDao.selectOne(
                    new LambdaQueryWrapper<MenuConfig>().eq(MenuConfig::getMenuKey, menuKey)
                );
                if (menu != null) {
                    menu.setSortOrder(sortOrder);
                    menu.setUpdatedAt(LocalDateTime.now());
                    menuConfigDao.updateById(menu);
                }
            }
            result.put("success", true);
            result.put("message", "排序更新成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
