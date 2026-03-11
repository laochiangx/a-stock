-- Seed data required for a fresh environment.
-- Idempotent: safe to run multiple times.

-- Default admin user (password: 123456)
INSERT INTO sys_user (username, password_hash, display_name, is_admin, is_enabled, created_at, updated_at)
VALUES ('admin', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Administrator', TRUE, TRUE, NOW(), NOW())
ON CONFLICT (username) DO NOTHING;

-- Default menu configuration
INSERT INTO menu_config (menu_key, menu_name, menu_icon, menu_path, parent_key, sort_order, is_visible, is_system, created_at, updated_at)
VALUES
  ('stock', '股票自选', 'StarOutline', '/', NULL, 0, TRUE, TRUE, NOW(), NOW()),

  ('market', '市场行情', 'NewspaperOutline', '/market', NULL, 1, TRUE, TRUE, NOW(), NOW()),
  ('market1', '市场快讯', 'NewspaperSharp', '/market?name=市场快讯', 'market', 0, TRUE, FALSE, NOW(), NOW()),
  ('market2', '全球股指', 'BarChartSharp', '/market?name=全球股指', 'market', 1, TRUE, FALSE, NOW(), NOW()),
  ('market3', '重大指数', 'AnalyticsOutline', '/market?name=重大指数', 'market', 2, TRUE, FALSE, NOW(), NOW()),
  ('market4', '行业排名', 'Flag', '/market?name=行业排名', 'market', 3, TRUE, FALSE, NOW(), NOW()),
  ('market5', '个股资金流向', 'Pulse', '/market?name=个股资金流向', 'market', 4, TRUE, FALSE, NOW(), NOW()),
  ('market6', '龙虎榜', 'Dragon', '/market?name=龙虎榜', 'market', 5, TRUE, FALSE, NOW(), NOW()),
  ('market7', '个股研报', 'StockOutlined', '/market?name=个股研报', 'market', 6, TRUE, FALSE, NOW(), NOW()),
  ('market8', '公司公告', 'NotificationFilled', '/market?name=公司公告', 'market', 7, TRUE, FALSE, NOW(), NOW()),
  ('market9', '行业研究', 'ReportSearch', '/market?name=行业研究', 'market', 8, TRUE, FALSE, NOW(), NOW()),
  ('market10', '当前热门', 'Gripfire', '/market?name=当前热门', 'market', 9, TRUE, FALSE, NOW(), NOW()),
  ('market11', '指标选股', 'BoxSearch20Regular', '/market?name=指标选股', 'market', 10, TRUE, FALSE, NOW(), NOW()),
  ('market12', '名站优选', 'FirefoxBrowser', '/market?name=名站优选', 'market', 11, TRUE, FALSE, NOW(), NOW()),

  ('themePlate', '题材板块', 'LayersOutline', '/theme-plate', NULL, 2, TRUE, FALSE, NOW(), NOW()),
  ('review', '复盘', 'CalendarOutline', '/review', NULL, 3, TRUE, FALSE, NOW(), NOW()),
  ('leaderMining', '龙头挖掘', 'SwapHorizontalOutline', '/leader-mining', NULL, 4, TRUE, FALSE, NOW(), NOW()),
  ('thsLongTiger', '龙虎榜', 'FlashOutline', '/ths-longtiger', NULL, 5, TRUE, FALSE, NOW(), NOW()),
  ('hotRank', '热榜', 'TrendingUpOutline', '/hot-rank', NULL, 6, TRUE, FALSE, NOW(), NOW()),
  ('popularityRank', '人气聚合', 'PeopleOutline', '/popularity-rank', NULL, 7, TRUE, FALSE, NOW(), NOW()),
  ('marketData', '市场数据', 'StatsChartOutline', '/market-data', NULL, 8, TRUE, FALSE, NOW(), NOW()),

  ('tdxLadder', '连板天梯', 'StatsChartOutline', '/tdx-ladder', NULL, 9, TRUE, FALSE, NOW(), NOW()),
  ('tdxLadderOrig', '连板天梯（原）', 'StatsChartOutline', '/tdx-ladder-orig', NULL, 10, TRUE, FALSE, NOW(), NOW()),

  ('fund', '基金自选', 'SparklesOutline', '/fund', NULL, 11, FALSE, FALSE, NOW(), NOW()),
  ('agent', 'AI智能体', 'Robot', '/agent', NULL, 12, FALSE, FALSE, NOW(), NOW()),

  ('system', '系统管理', 'SettingsOutline', NULL, NULL, 100, TRUE, TRUE, NOW(), NOW()),
  ('menuManage', '菜单管理', 'ReorderTwoOutline', '/menu-manage', 'system', 0, TRUE, TRUE, NOW(), NOW()),
  ('wordDictionary', '词典管理', 'CommentNote20Filled', '/word-dictionary', 'system', 1, TRUE, FALSE, NOW(), NOW()),
  ('tagManage', '标签管理', 'BonfireOutline', '/tag-manage', 'system', 2, TRUE, FALSE, NOW(), NOW()),
  ('stockTagDaily', '每日热门&打标', 'FlameSharp', '/stock-tag-daily', 'system', 3, TRUE, FALSE, NOW(), NOW()),
  ('settings', '设置', 'SettingsOutline', '/settings', 'system', 4, TRUE, TRUE, NOW(), NOW()),
  ('about', '关于', 'LogoGithub', '/about', 'system', 5, TRUE, TRUE, NOW(), NOW())
ON CONFLICT (menu_key) DO NOTHING;
