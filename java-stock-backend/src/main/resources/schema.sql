-- 创建数据库表结构

-- 创建标签表
CREATE TABLE IF NOT EXISTS tags (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    type VARCHAR(50),
    priority INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

 ALTER TABLE tags ADD COLUMN IF NOT EXISTS priority INTEGER DEFAULT 0;

-- 创建指数基本信息表
CREATE TABLE IF NOT EXISTS index_basic (
    id SERIAL PRIMARY KEY,
    ts_code VARCHAR(20) NOT NULL,
    name VARCHAR(255) NOT NULL,
    market VARCHAR(50),
    publisher VARCHAR(100),
    publish_date DATE,
    base_date DATE,
    base_point DECIMAL(15,4),
    category VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建股票基本信息表
CREATE TABLE IF NOT EXISTS stock_basic (
    id SERIAL PRIMARY KEY,
    ts_code VARCHAR(20) NOT NULL,
    symbol VARCHAR(20),
    name VARCHAR(255) NOT NULL,
    area VARCHAR(100),
    industry VARCHAR(100),
    fullname VARCHAR(500),
    enname VARCHAR(500),
    cnspell VARCHAR(100),
    market VARCHAR(50),
    exchange VARCHAR(50),
    curr_type VARCHAR(20),
    list_status VARCHAR(20),
    list_date DATE,
    delist_date DATE,
    is_hs VARCHAR(10),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建电报列表表
CREATE TABLE IF NOT EXISTS telegraph_list (
    id SERIAL PRIMARY KEY,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0,
    time VARCHAR(20),
    data_time TIMESTAMP,
    title VARCHAR(500),
    content TEXT,
    is_red BOOLEAN DEFAULT FALSE,
    url VARCHAR(1000),
    source VARCHAR(100),
    sentiment_result VARCHAR(100),
    subjects TEXT,
    stocks TEXT
);

-- 创建电报标签关联表
CREATE TABLE IF NOT EXISTS telegraph_tags (
    id SERIAL PRIMARY KEY,
    telegraph_id INTEGER NOT NULL,
    tag_id INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0,
    FOREIGN KEY (telegraph_id) REFERENCES telegraph_list(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
);

-- 创建自选股表
CREATE TABLE IF NOT EXISTS followed_stock (
    id SERIAL PRIMARY KEY,
    stock_code VARCHAR(20) NOT NULL UNIQUE,
    stock_name VARCHAR(100),
    cost_price DECIMAL(15,4) DEFAULT 0,
    volume INTEGER DEFAULT 0,
    alarm_change_percent DECIMAL(10,4) DEFAULT 0,
    alarm_price DECIMAL(15,4) DEFAULT 0,
    sort INTEGER DEFAULT 999,
    cron VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建股票分组表
CREATE TABLE IF NOT EXISTS stock_group (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    sort INTEGER DEFAULT 999,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建股票分组关联表
CREATE TABLE IF NOT EXISTS stock_group_relation (
    id SERIAL PRIMARY KEY,
    stock_code VARCHAR(20) NOT NULL,
    group_id INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(stock_code, group_id)
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_telegraph_list_source ON telegraph_list(source);
CREATE INDEX IF NOT EXISTS idx_telegraph_list_time ON telegraph_list(time);
CREATE INDEX IF NOT EXISTS idx_telegraph_list_data_time ON telegraph_list(data_time);
CREATE INDEX IF NOT EXISTS idx_stock_basic_ts_code ON stock_basic(ts_code);
CREATE INDEX IF NOT EXISTS idx_index_basic_ts_code ON index_basic(ts_code);
CREATE INDEX IF NOT EXISTS idx_followed_stock_code ON followed_stock(stock_code);
CREATE INDEX IF NOT EXISTS idx_stock_group_relation_stock ON stock_group_relation(stock_code);
CREATE INDEX IF NOT EXISTS idx_stock_group_relation_group ON stock_group_relation(group_id);

-- 创建题材板块数据表
CREATE TABLE IF NOT EXISTS theme_plate_data (
    id SERIAL PRIMARY KEY,
    data_date DATE NOT NULL,
    plate_name VARCHAR(100) NOT NULL,
    plate_description TEXT,
    stock_code VARCHAR(20) NOT NULL,
    stock_name VARCHAR(100),
    price DECIMAL(15,4),
    change_rate DECIMAL(10,6),
    market_value DECIMAL(20,4),
    turnover_ratio DECIMAL(10,4),
    enter_time BIGINT,
    is_up_limit BOOLEAN DEFAULT FALSE,
    m_days_n_boards VARCHAR(50),
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(data_date, plate_name, stock_code)
);

-- 创建题材板块汇总表（每天的板块列表）
CREATE TABLE IF NOT EXISTS theme_plate_summary (
    id SERIAL PRIMARY KEY,
    data_date DATE NOT NULL,
    plate_id VARCHAR(50),
    plate_name VARCHAR(100) NOT NULL,
    plate_description TEXT,
    stock_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(data_date, plate_name)
);

CREATE TABLE IF NOT EXISTS xuangutong_theme_plate (
    id SERIAL PRIMARY KEY,
    data_date DATE NOT NULL,
    plate_id VARCHAR(50) NOT NULL,
    plate_name VARCHAR(200) NOT NULL,
    plate_description TEXT,
    stock_code VARCHAR(20) NOT NULL,
    stock_name VARCHAR(100) NOT NULL,
    price DECIMAL(10,2),
    change_rate DECIMAL(10,6),
    turnover_ratio DECIMAL(10,6),
    market_value BIGINT,
    is_up_limit BOOLEAN,
    enter_time BIGINT,
    m_days_n_boards VARCHAR(20),
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0,
    UNIQUE(data_date, plate_id, stock_code)
);

CREATE INDEX IF NOT EXISTS idx_xg_date ON xuangutong_theme_plate (data_date);
CREATE INDEX IF NOT EXISTS idx_xg_plate ON xuangutong_theme_plate (data_date, plate_id);

CREATE TABLE IF NOT EXISTS dongcai_theme_plate (
    id SERIAL PRIMARY KEY,
    data_date DATE NOT NULL,
    plate_id BIGINT NOT NULL,
    plate_name VARCHAR(200) NOT NULL,
    plate_description TEXT,
    stock_code VARCHAR(20) NOT NULL,
    stock_name VARCHAR(100) NOT NULL,
    change_rate DECIMAL(10,6),
    description TEXT,
    lianban VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0,
    UNIQUE(data_date, plate_id, stock_code)
);

CREATE INDEX IF NOT EXISTS idx_dc_date ON dongcai_theme_plate (data_date);

CREATE TABLE IF NOT EXISTS wuyang_theme_subject (
    id SERIAL PRIMARY KEY,
    data_date DATE NOT NULL,
    subject_name VARCHAR(200) NOT NULL,
    subject_detail TEXT,
    stock_code VARCHAR(20) NOT NULL,
    stock_name VARCHAR(100) NOT NULL,
    lb_count VARCHAR(20),
    first_zt_time VARCHAR(20),
    last_zt_time VARCHAR(20),
    price DECIMAL(15,4),
    percent DECIMAL(10,6),
    amount DECIMAL(20,6),
    reason TEXT,
    is_zt BOOLEAN,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0,
    UNIQUE(data_date, subject_name, stock_code)
);

CREATE INDEX IF NOT EXISTS idx_wy_date ON wuyang_theme_subject (data_date);
CREATE INDEX IF NOT EXISTS idx_wy_subject ON wuyang_theme_subject (data_date, subject_name);
CREATE INDEX IF NOT EXISTS idx_dc_plate ON dongcai_theme_plate (data_date, plate_id);
CREATE INDEX IF NOT EXISTS idx_theme_plate_data_date ON theme_plate_data(data_date);
CREATE INDEX IF NOT EXISTS idx_theme_plate_data_plate ON theme_plate_data(plate_name);
CREATE INDEX IF NOT EXISTS idx_theme_plate_summary_date ON theme_plate_summary(data_date);


 CREATE TABLE IF NOT EXISTS stock_daily_snapshot (
     id SERIAL PRIMARY KEY,
     data_date DATE NOT NULL,
     stock_code VARCHAR(20) NOT NULL,
     stock_name VARCHAR(100),
     sources TEXT,
     dcpm INTEGER,
     rqbpm INTEGER,
     thspm INTEGER,
     shy INTEGER,
     zf DECIMAL(10,4),
     zlje DECIMAL(20,4),
     zljzb DECIMAL(10,4),
     preselected BOOLEAN DEFAULT FALSE,
     dragon_tiger BOOLEAN DEFAULT FALSE,
     dragon_tiger_explain TEXT,
     evidence TEXT,
     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
     deleted INTEGER DEFAULT 0,
     UNIQUE(data_date, stock_code)
 );

 CREATE INDEX IF NOT EXISTS idx_stock_daily_snapshot_date ON stock_daily_snapshot(data_date);
 CREATE INDEX IF NOT EXISTS idx_stock_daily_snapshot_code ON stock_daily_snapshot(stock_code);


 CREATE TABLE IF NOT EXISTS stock_daily_snapshot_tag (
     id SERIAL PRIMARY KEY,
     snapshot_id INTEGER NOT NULL,
     tag_id INTEGER NOT NULL,
     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
     deleted INTEGER DEFAULT 0,
     UNIQUE(snapshot_id, tag_id),
     FOREIGN KEY (snapshot_id) REFERENCES stock_daily_snapshot(id) ON DELETE CASCADE,
     FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
 );

 CREATE INDEX IF NOT EXISTS idx_stock_daily_snapshot_tag_snapshot ON stock_daily_snapshot_tag(snapshot_id);
 CREATE INDEX IF NOT EXISTS idx_stock_daily_snapshot_tag_tag ON stock_daily_snapshot_tag(tag_id);


-- 创建24小时热词表（增强版）
CREATE TABLE IF NOT EXISTS hot_word (
    id SERIAL PRIMARY KEY,
    word VARCHAR(100) NOT NULL,
    frequency INTEGER DEFAULT 0,
    weight DECIMAL(10,4) DEFAULT 1.0,
    score DECIMAL(15,4) DEFAULT 0,
    source VARCHAR(100),
    data_date DATE NOT NULL,
    data_hour INTEGER DEFAULT 0,
    -- 新增字段：更细维度统计
    word_type VARCHAR(50) DEFAULT 'normal',  -- 词汇类型: positive/negative/industry/concept/stock/normal
    sentiment_score DECIMAL(10,4) DEFAULT 0,  -- 情感得分
    industry VARCHAR(100),                    -- 所属行业
    related_stocks TEXT,                      -- 关联股票代码，逗号分隔
    first_appear_time TIMESTAMP,              -- 首次出现时间
    last_appear_time TIMESTAMP,               -- 最后出现时间
    appear_count INTEGER DEFAULT 1,           -- 出现次数（跨小时累计）
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_hot_word ON hot_word (word, source, data_date, data_hour);
CREATE INDEX IF NOT EXISTS idx_hot_word_date ON hot_word (data_date);
CREATE INDEX IF NOT EXISTS idx_hot_word_source ON hot_word (source);
CREATE INDEX IF NOT EXISTS idx_hot_word_score ON hot_word (score DESC);
CREATE INDEX IF NOT EXISTS idx_hot_word_type ON hot_word (word_type);

-- 创建动态词典表（自动学习新词汇）
CREATE TABLE IF NOT EXISTS word_dictionary (
    id SERIAL PRIMARY KEY,
    word VARCHAR(100) NOT NULL UNIQUE,
    word_type VARCHAR(50) DEFAULT 'normal',   -- positive/negative/industry/concept/stock/normal
    base_weight DECIMAL(10,4) DEFAULT 1.0,    -- 基础权重
    sentiment_value DECIMAL(10,4) DEFAULT 0,  -- 情感值：正数为正面，负数为负面
    industry VARCHAR(100),                    -- 所属行业（如果是行业词）
    is_system BOOLEAN DEFAULT FALSE,          -- 是否系统预置词
    frequency_total INTEGER DEFAULT 0,        -- 历史总出现次数
    last_seen_date DATE,                      -- 最后出现日期
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_word_dict_type ON word_dictionary (word_type);
CREATE INDEX IF NOT EXISTS idx_word_dict_industry ON word_dictionary (industry);
CREATE INDEX IF NOT EXISTS idx_word_dict_weight ON word_dictionary (base_weight DESC);

-- 创建热词统计汇总表（按天/周/月汇总）
CREATE TABLE IF NOT EXISTS hot_word_summary (
    id SERIAL PRIMARY KEY,
    word VARCHAR(100) NOT NULL,
    summary_type VARCHAR(20) NOT NULL,        -- daily/weekly/monthly
    summary_date DATE NOT NULL,
    total_frequency INTEGER DEFAULT 0,
    avg_weight DECIMAL(10,4) DEFAULT 1.0,
    total_score DECIMAL(15,4) DEFAULT 0,
    source_count INTEGER DEFAULT 1,           -- 出现在多少个来源
    sources TEXT,                             -- 来源列表，逗号分隔
    word_type VARCHAR(50),
    trend VARCHAR(20),                        -- up/down/stable 趋势
    rank_change INTEGER DEFAULT 0,            -- 排名变化
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_hot_word_summary ON hot_word_summary (word, summary_type, summary_date);
CREATE INDEX IF NOT EXISTS idx_hot_word_summary_date ON hot_word_summary (summary_date);
CREATE INDEX IF NOT EXISTS idx_hot_word_summary_type ON hot_word_summary (summary_type);


CREATE TABLE IF NOT EXISTS ths_hot_list_day (
    id SERIAL PRIMARY KEY,
    data_date DATE NOT NULL,
    stock_type VARCHAR(20) NOT NULL,
    rank_type VARCHAR(20) NOT NULL,
    list_type VARCHAR(20) NOT NULL,
    market INTEGER,
    stock_code VARCHAR(20) NOT NULL,
    stock_name VARCHAR(100),
    rate DECIMAL(20,4),
    rise_and_fall DECIMAL(20,8),
    analyse_title VARCHAR(500),
    analysis TEXT,
    hot_rank_chg INTEGER,
    topic_code VARCHAR(100),
    topic_title VARCHAR(500),
    topic_ios_jump_url VARCHAR(1000),
    topic_android_jump_url VARCHAR(1000),
    concept_tags TEXT,
    popularity_tag VARCHAR(200),
    order_num INTEGER,
    raw_json TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0,
    UNIQUE(data_date, stock_type, rank_type, list_type, stock_code)
);

CREATE INDEX IF NOT EXISTS idx_ths_hot_list_day_date ON ths_hot_list_day (data_date);
CREATE INDEX IF NOT EXISTS idx_ths_hot_list_day_order ON ths_hot_list_day (data_date, order_num);


CREATE TABLE IF NOT EXISTS dc_hot_list_day (
    id SERIAL PRIMARY KEY,
    data_date DATE NOT NULL,
    list_type VARCHAR(20) NOT NULL,
    stock_code VARCHAR(20) NOT NULL,
    stock_name VARCHAR(100),
    hot_score DECIMAL(20,4),
    rise_and_fall DECIMAL(20,8),
    order_num INTEGER,
    raw_json TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0,
    UNIQUE(data_date, list_type, stock_code)
);

CREATE INDEX IF NOT EXISTS idx_dc_hot_list_day_date ON dc_hot_list_day (data_date);
CREATE INDEX IF NOT EXISTS idx_dc_hot_list_day_order ON dc_hot_list_day (data_date, order_num);

CREATE TABLE IF NOT EXISTS cls_hot_list_day (
    id SERIAL PRIMARY KEY,
    data_date DATE NOT NULL,
    list_type VARCHAR(20) NOT NULL,
    stock_code VARCHAR(20) NOT NULL,
    stock_name VARCHAR(100),
    hot_score DECIMAL(20,4),
    rise_and_fall DECIMAL(20,8),
    order_num INTEGER,
    raw_json TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0,
    UNIQUE(data_date, list_type, stock_code)
);

CREATE INDEX IF NOT EXISTS idx_cls_hot_list_day_date ON cls_hot_list_day (data_date);
CREATE INDEX IF NOT EXISTS idx_cls_hot_list_day_order ON cls_hot_list_day (data_date, order_num);

-- 创建菜单配置表
CREATE TABLE IF NOT EXISTS menu_config (
    id SERIAL PRIMARY KEY,
    menu_key VARCHAR(100) NOT NULL UNIQUE,    -- 菜单唯一标识
    menu_name VARCHAR(100) NOT NULL,          -- 菜单显示名称
    menu_icon VARCHAR(100),                   -- 菜单图标
    menu_path VARCHAR(200),                   -- 路由路径
    parent_key VARCHAR(100),                  -- 父菜单key，null表示顶级菜单
    sort_order INTEGER DEFAULT 0,             -- 排序
    is_visible BOOLEAN DEFAULT TRUE,          -- 是否显示
    is_system BOOLEAN DEFAULT FALSE,          -- 是否系统菜单（不可删除）
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_menu_config_parent ON menu_config (parent_key);
CREATE INDEX IF NOT EXISTS idx_menu_config_visible ON menu_config (is_visible);
CREATE INDEX IF NOT EXISTS idx_menu_config_sort ON menu_config (sort_order);


CREATE TABLE IF NOT EXISTS market_daily_stat (
    id SERIAL PRIMARY KEY,
    data_date DATE NOT NULL,
    rising_count INTEGER,
    falling_count INTEGER,
    total_zt INTEGER,
    total_dt INTEGER,
    lb_count INTEGER,
    up_10_percent INTEGER,
    down_9_percent INTEGER,
    yizi INTEGER,
    first_board_count INTEGER,
    first_board_rate DECIMAL(10,4),
    second_board_count INTEGER,
    second_board_rate DECIMAL(10,4),
    third_board_count INTEGER,
    third_board_rate DECIMAL(10,4),
    fourth_board_count INTEGER,
    fourth_board_rate DECIMAL(10,4),
    fifth_board_above_count INTEGER,
    fifth_board_above_rate DECIMAL(10,4),
    zt_925 INTEGER,
    before_10_count INTEGER,
    before_10_rate DECIMAL(10,4),
    between_10_1130_count INTEGER,
    between_10_1130_rate DECIMAL(10,4),
    between_13_14_count INTEGER,
    between_13_14_rate DECIMAL(10,4),
    between_14_15_count INTEGER,
    between_14_15_rate DECIMAL(10,4),
    open_count INTEGER,
    fb_ratio DECIMAL(10,4),
    one_to_two_ratio DECIMAL(10,4),
    two_to_three_ratio DECIMAL(10,4),
    three_to_four_ratio DECIMAL(10,4),
    lb_ratio DECIMAL(10,4),
    yesterday_lb_ratio DECIMAL(10,4),
    zt_amount DECIMAL(20,4),
    total_amount DECIMAL(20,4),
    sh_amount DECIMAL(20,4),
    chuangye_amount DECIMAL(20,4),
    kc_amount DECIMAL(20,4),
    market_score INTEGER,
    score_grade VARCHAR(10),
    score_components TEXT,
    stage_code VARCHAR(10),
    stage_name VARCHAR(50),
    stage_reason TEXT,
    raw_json TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0,
    UNIQUE(data_date)
);

CREATE INDEX IF NOT EXISTS idx_market_daily_stat_date ON market_daily_stat (data_date);


CREATE TABLE IF NOT EXISTS tdx_lbtt_day (
    id SERIAL PRIMARY KEY,
    data_date DATE NOT NULL,
    rising_count INTEGER,
    falling_count INTEGER,
    zt_all_count INTEGER,
    zt_count INTEGER,
    dt_count INTEGER,
    total_amount DECIMAL(20,0),
    last_total_amount DECIMAL(20,0),
    hot1_name VARCHAR(200),
    hot1_count INTEGER,
    hot2_name VARCHAR(200),
    hot2_count INTEGER,
    hot3_name VARCHAR(200),
    hot3_count INTEGER,
    raw_json TEXT,
    pbsdstat_json TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0,
    UNIQUE(data_date)
);

CREATE INDEX IF NOT EXISTS idx_tdx_lbtt_day_date ON tdx_lbtt_day (data_date);


CREATE TABLE IF NOT EXISTS tdx_lbtt_item (
    id SERIAL PRIMARY KEY,
    data_date DATE NOT NULL,
    max_level INTEGER,
    level INTEGER,
    stock_code VARCHAR(20) NOT NULL,
    market VARCHAR(5),
    stock_name VARCHAR(100),
    reason VARCHAR(500),
    reason2 VARCHAR(500),
    zt_time VARCHAR(20),
    seal_amount DECIMAL(20,0),
    open_times INTEGER,
    industry VARCHAR(200),
    zt_state INTEGER,
    promote_rate DECIMAL(10,6),
    raw_json TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0,
    UNIQUE(data_date, level, stock_code)
);

CREATE INDEX IF NOT EXISTS idx_tdx_lbtt_item_date ON tdx_lbtt_item (data_date);
CREATE INDEX IF NOT EXISTS idx_tdx_lbtt_item_level ON tdx_lbtt_item (data_date, level);
