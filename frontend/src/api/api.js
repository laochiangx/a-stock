// API 适配层 - 将 Wails 调用转换为 HTTP API 调用
import axios from 'axios';

// 创建 axios 实例
const apiClient = axios.create({
  baseURL: '/api', // 代理前缀，会被 Vite 代理到 Java 后端
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 拦截器 - 处理错误
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('API Error:', error);
    return Promise.reject(error);
  }
);

// 模拟 Wails 函数调用的 API 适配器
export const ApiAdapter = {
  // 获取配置
  GetConfig: () => {
    return apiClient.get('/config').then(response => response.data).catch(() => {
      console.error('Failed to get config, returning default');
      return { enableNews: false, enableFund: false, enableAgent: false, darkTheme: false, tushareToken: false };
    });
  },

  // 获取关注列表
  GetFollowList: (groupId) => {
    return apiClient.get(`/follow/list`, { params: { groupId } }).then(response => response.data);
  },

  // 获取股票列表
  GetStockList: (query) => {
    return apiClient.get('/stocks', { params: { query } }).then(response => response.data);
  },

  // 关注股票
  Follow: (stockCode) => {
    return apiClient.post('/follow', { stockCode }).then(response => response.data);
  },

  // 取消关注股票
  UnFollow: (stockCode) => {
    return apiClient.delete(`/follow/${stockCode}`).then(response => response.data);
  },

  // 获取股票实时数据
  Greet: (stockCode) => {
    return apiClient.get(`/stock/${stockCode}`).then(response => response.data);
  },

  // 获取分组列表
  GetGroupList: () => {
    return apiClient.get('/groups').then(response => response.data).catch(() => {
      console.error('Failed to get group list, returning default');
      return [];
    });
  },

  // 添加分组
  AddGroup: (groupName) => {
    return apiClient.post('/groups', { name: groupName }).then(response => response.data);
  },

  // 删除分组
  RemoveGroup: (groupId) => {
    return apiClient.delete(`/groups/${groupId}`).then(response => response.data);
  },

  // 获取K线数据
  GetStockKLine: (stockCode, stockName, days) => {
    return apiClient.get(`/stock/${stockCode}/kline`, { params: { name: stockName, days: days || 365 } }).then(response => response.data);
  },

  // 获取分时数据
  GetStockMinutePriceLineData: (stockCode, stockName) => {
    return apiClient.get(`/stock/${stockCode}/minute`, { params: { name: stockName } }).then(response => response.data);
  },

  // 获取版本信息
  GetVersionInfo: () => {
    return apiClient.get('/version').then(response => response.data).catch(() => {
      console.error('Failed to get version info, returning default');
      return { version: '1.0.0', officialStatement: null };
    });
  },

  // 获取AI配置
  GetAiConfigs: () => {
    return apiClient.get('/ai/configs').then(response => response.data);
  },

  // 获取提示模板
  GetPromptTemplates: (type, content) => {
    return apiClient.get('/ai/templates', { params: { type, content } }).then(response => response.data);
  },

  // 保存AI响应结果
  SaveAIResponseResult: (stockCode, stockName, result, chatId, question, configId) => {
    return apiClient.post('/ai/results', {
      stockCode,
      stockName,
      result,
      chatId,
      question,
      configId
    }).then(response => response.data);
  },

  // 设置股票排序
  SetStockSort: (sort, stockCode) => {
    return apiClient.put(`/follow/${stockCode}/sort`, { sort }).then(response => response.data);
  },

  // 设置股票价格提醒
  SetAlarmChangePercent: (percent, alarmPrice, stockCode) => {
    return apiClient.put(`/follow/${stockCode}/alarm`, { percent, alarmPrice }).then(response => response.data);
  },

  // 设置成本价和数量
  SetCostPriceAndVolume: (stockCode, costPrice, volume) => {
    return apiClient.put(`/follow/${stockCode}/cost`, { costPrice, volume }).then(response => response.data);
  },

  // 设置AI定时任务
  SetStockAICron: (cron, stockCode) => {
    return apiClient.put(`/follow/${stockCode}/cron`, { cron }).then(response => response.data);
  },

  // 更新分组排序
  UpdateGroupSort: (groupId, sort) => {
    return apiClient.put(`/groups/${groupId}/sort`, { sort }).then(response => response.data);
  },

  // 初始化分组排序
  InitializeGroupSort: () => {
    return apiClient.post('/groups/init-sort').then(response => response.data);
  },

  // 保存为 Markdown
  SaveAsMarkdown: (content) => {
    return apiClient.post('/utils/save-markdown', { content }).then(response => response.data);
  },

  // 保存图片
  SaveImage: (data) => {
    return apiClient.post('/utils/save-image', data).then(response => response.data);
  },

  // 保存 Word 文件
  SaveWordFile: (content) => {
    return apiClient.post('/utils/save-word', { content }).then(response => response.data);
  },

  // 分享分析
  ShareAnalysis: (data) => {
    return apiClient.post('/utils/share', data).then(response => response.data);
  },

  // 发送钉钉消息
  SendDingDingMessageByType: (type, content) => {
    return apiClient.post('/utils/dingding', { type, content }).then(response => response.data);
  },

  // 获取AI响应结果
  GetAIResponseResult: (params) => {
    return apiClient.get('/ai/results', { params }).then(response => response.data);
  },

  // 新建聊天流
  NewChatStream: (data) => {
    // 这可能需要使用 SSE 或 WebSocket，暂时返回模拟数据
    return new Promise((resolve) => {
      resolve({ status: 'stream_started', message: 'Chat stream started' });
    });
  },

  // 打开 URL (这个可能无法完全模拟，返回成功)
  OpenURL: (url) => {
    // 在浏览器中打开 URL
    window.open(url, '_blank');
    return Promise.resolve({ success: true });
  },

  // 添加股票到分组
  AddStockGroup: (stockCode, groupId) => {
    return apiClient.post('/follow/stock-group', { stockCode, groupId }).then(response => response.data);
  },

  // 从分组移除股票
  RemoveStockGroup: (stockCode, groupId) => {
    return apiClient.delete(`/follow/stock-group`, { params: { stockCode, groupId } }).then(response => response.data);
  },

  // 检查更新
  CheckUpdate: (params) => {
    return apiClient.get('/update/check', { params }).then(response => response.data);
  },

  // 获取赞助信息
  GetSponsorInfo: () => {
    return apiClient.get('/sponsor/info').then(response => response.data);
  },

  // 与AI代理聊天
  ChatWithAgent: (question, configId, params) => {
    return apiClient.post('/ai/chat', { question, configId, params }).then(response => response.data);
  },

  // 关注基金
  FollowFund: (fundCode) => {
    return apiClient.post('/fund/follow', { fundCode }).then(response => response.data);
  },

  // 获取已关注基金
  GetFollowedFund: () => {
    return apiClient.get('/fund/followed').then(response => response.data);
  },

  // 获取基金列表
  GetfundList: (query) => {
    return apiClient.get('/funds', { params: { query } }).then(response => response.data);
  },

  // 取消关注基金
  UnFollowFund: (fundCode) => {
    return apiClient.delete(`/fund/unfollow/${fundCode}`).then(response => response.data);
  },

  // 获取行业排名
  GetIndustryRank: (sort, limit) => {
    return apiClient.get('/industry/rank', { params: { sort, limit } }).then(response => response.data);
  },

  // 获取电报列表
  GetTelegraphList: (source) => {
    return apiClient.get('/market-news/telegraph', { params: { source } }).then(response => response.data);
  },

  // 获取全球股指
  GlobalStockIndexes: () => {
    return apiClient.get('/indexes/global').then(response => response.data);
  },

  // 刷新电报列表
  ReFleshTelegraphList: (source) => {
    return apiClient.get(`/market-news/telegraph/refresh`, { params: { source } }).then(response => response.data);
  },

  // 总结股票新闻
  SummaryStockNews: (question, configId, promptId, enableTools, thinkingMode) => {
    return apiClient.post('/news/summary', { 
      question, 
      configId, 
      promptId, 
      enableTools, 
      thinkingMode 
    }).then(response => response.data);
  },

  // 添加提示词
  AddPrompt: (prompt) => {
    return apiClient.post('/prompt', prompt).then(response => response.data);
  },

  // 删除提示词
  DelPrompt: (promptId) => {
    return apiClient.delete(`/prompt/${promptId}`).then(response => response.data);
  },

  // 导出配置
  ExportConfig: () => {
    return apiClient.get('/config/export').then(response => response.data);
  },

  // 更新配置
  UpdateConfig: (config) => {
    return apiClient.put('/config', config).then(response => response.data);
  },

  // 检查赞助码
  CheckSponsorCode: (sponsorCode) => {
    return apiClient.post('/sponsor/check', { sponsorCode }).then(response => response.data);
  },
  
  // 情感分析带权重词频统计
  AnalyzeSentimentWithFreqWeight: (text, source = '') => {
    return apiClient.post('/ai/sentiment-analysis', { text, source }).then(response => response.data);
  },
  
  // 获取龙虎榜数据
  GetDragonTigerList: (date) => {
    const params = date ? { date } : {};
    return apiClient.get('/stock-data/dragon-tiger', { params }).then(response => {
      // 后端返回 { data: [...], count: N, date: "..." }，前端需要 data 数组
      return response.data?.data || [];
    });
  },
  
  // 获取个股研报
  GetStockResearchReport: (stockCode) => {
    return apiClient.get('/stock-data/research-report', {
      params: { stockCode }
    }).then(response => response.data);
  },
  
  // 获取个股资金流向
  GetStockMoneyFlow: (stockCode) => {
    return apiClient.get('/stock-data/money-flow', {
      params: { stockCode }
    }).then(response => response.data);
  },
  
  // 获取公司公告
  GetCompanyNotice: (stockCode) => {
    return apiClient.get('/stock-data/company-notice', {
      params: { stockCode }
    }).then(response => response.data);
  },

  // 获取行业研究报告
  GetIndustryResearchReport: (industryCode) => {
    return apiClient.get('/stock-data/industry-research', {
      params: { industryCode }
    }).then(response => response.data);
  },
  
  // 获取个股资金趋势
  GetStockMoneyTrendByDay: (stockCode, days) => {
    return apiClient.get('/stock-data/money-trend', {
      params: { stockCode, days }
    }).then(response => response.data);
  },
  
  // 获取个股资金流向排名
  GetMoneyRankSina: (sort) => {
    return apiClient.get('/stock-data/money-rank', {
      params: { sort: sort || 'netamount' }
    }).then(response => response.data).catch(error => {
      console.error('GetMoneyRankSina API错误:', error);
      // 如果是404，返回空数组而不是抛出错误，让前端可以优雅处理
      if (error.response && error.response.status === 404) {
        console.warn('API端点 /api/stock-data/money-rank 未找到，请确认后端服务已重启');
        return [];
      }
      throw error; // 其他错误继续抛出
    });
  },
  
  // 获取行业资金排名
  GetIndustryMoneyRankSina: (fenlei, sort) => {
    return apiClient.get('/stock-data/industry-money-rank', {
      params: { fenlei: fenlei || '0', sort: sort || 'netamount' }
    }).then(response => response.data).catch(error => {
      console.error('GetIndustryMoneyRankSina API错误:', error);
      return [];
    });
  },
  
  // 获取股吧热门话题
  GetHotTopic: (size) => {
    return apiClient.get('/stock-data/hot-topic', {
      params: { size: size || 10 }
    }).then(response => response.data).catch(error => {
      console.error('GetHotTopic API错误:', error);
      return [];
    });
  },
  
  // 获取热门选股策略
  GetHotStrategy: () => {
    return apiClient.get('/stock-data/hot-strategy').then(response => response.data).catch(error => {
      console.error('GetHotStrategy API错误:', error);
      return { code: -1, data: [] };
    });
  },
  
  // 智能选股
  SearchStock: (words, pageSize) => {
    return apiClient.post('/stock-data/search-stock', {
      words: words,
      pageSize: pageSize || 5000
    }).then(response => response.data).catch(error => {
      console.error('SearchStock API错误:', error);
      return { code: -1, message: error.message };
    });
  },

  // 财联社日历
  ClsCalendar: () => {
    return apiClient.get('/market-news/cls-calendar').then(response => response.data).catch(error => {
      console.error('ClsCalendar API错误:', error);
      return [];
    });
  },

  // 热门事件
  HotEvent: (size) => {
    return apiClient.get('/stock-data/hot-event', { params: { size: size || 50 } }).then(response => response.data).catch(error => {
      console.error('HotEvent API错误:', error);
      return [];
    });
  },

  // 热门股票
  HotStock: (marketType) => {
    return apiClient.get('/stock-data/hot-stock', { params: { marketType: marketType || '10' } }).then(response => response.data).catch(error => {
      console.error('HotStock API错误:', error);
      return [];
    });
  },

  InvestCalendarTimeLine: (yearMonth) => {
    return apiClient.get('/market-news/invest-calendar', { params: { yearMonth } }).then(response => response.data).catch(error => {
      console.error('InvestCalendarTimeLine API错误:', error);
      return [];
    });
  },

  GetThemePlateData: (date) => {
    return apiClient.get('/theme-plate/full', { params: { date } }).then(response => response.data).catch(error => {
      console.error('GetThemePlateData API错误:', error);
      return { success: false, message: error.message };
    });
  },

  GetPopularityRank: (limit) => {
    return apiClient.get('/popularity-rank', { params: { limit } }).then(response => response.data).catch(error => {
      console.error('GetPopularityRank API错误:', error);
      return { success: false, message: error.message };
    });
  },

  SyncThsHotListDay: (date, force, stockType, listType) => {
    return apiClient.get('/ths-hot-list/day/sync', { params: { date, force: !!force, stockType, listType } }).then(response => response.data).catch(error => {
      console.error('SyncThsHotListDay API错误:', error);
      return { success: false, message: error.message };
    });
  },

  GetThsHotListDayList: (date, stockType, listType) => {
    return apiClient.get('/ths-hot-list/day/list', { params: { date, stockType, listType } }).then(response => response.data).catch(error => {
      console.error('GetThsHotListDayList API错误:', error);
      return { success: false, message: error.message };
    });
  },

  SyncDcHotListDay: (date, force, listType) => {
    return apiClient.get('/dc-hot-list/day/sync', { params: { date, force: !!force, listType } }).then(response => response.data).catch(error => {
      console.error('SyncDcHotListDay API错误:', error);
      return { success: false, message: error.message };
    });
  },

  GetDcHotListDayList: (date, listType) => {
    return apiClient.get('/dc-hot-list/day/list', { params: { date, listType } }).then(response => response.data).catch(error => {
      console.error('GetDcHotListDayList API错误:', error);
      return { success: false, message: error.message };
    });
  },

  SyncClsHotListDay: (date, force, listType) => {
    return apiClient.get('/cls-hot-list/day/sync', { params: { date, force: !!force, listType } }).then(response => response.data).catch(error => {
      console.error('SyncClsHotListDay API错误:', error);
      return { success: false, message: error.message };
    });
  },

  GetClsHotListDayList: (date, listType) => {
    return apiClient.get('/cls-hot-list/day/list', { params: { date, listType } }).then(response => response.data).catch(error => {
      console.error('GetClsHotListDayList API错误:', error);
      return { success: false, message: error.message };
    });
  },

  SyncThemePlateData: (date, force) => {
    return apiClient.post('/theme-plate/sync', null, { params: { date, force: !!force } }).then(response => response.data).catch(error => {
      console.error('SyncThemePlateData API错误:', error);
      return { success: false, message: error.message };
    });
  },

  SyncDongcaiData: (date, force) => {
    return apiClient.post('/theme-plate/dongcai/sync', null, { params: { date, force: !!force } }).then(response => response.data).catch(error => {
      console.error('SyncDongcaiData API错误:', error);
      return { success: false, message: error.message };
    });
  },

  GetDongcaiPlates: (date) => {
    return apiClient.get('/theme-plate/dongcai/plates', { params: { date } }).then(response => response.data).catch(error => {
      console.error('GetDongcaiPlates API错误:', error);
      return { success: false, message: error.message };
    });
  },

  GetDongcaiStocks: (date, plateId) => {
    return apiClient.get('/theme-plate/dongcai/stocks', { params: { date, plateId } }).then(response => response.data).catch(error => {
      console.error('GetDongcaiStocks API错误:', error);
      return { success: false, message: error.message };
    });
  },

  GetDongcaiHotThemes: (bdType) => {
    return apiClient.get('/theme-plate/dongcai/hot-themes', { params: { bdType } }).then(response => response.data).catch(error => {
      console.error('GetDongcaiHotThemes API错误:', error);
      return { success: false, message: error.message };
    });
  },

  GetDongcaiHotThemeStocks: (themeCode, pageNum, pageSize, sort, sortField) => {
    return apiClient.get('/theme-plate/dongcai/hot-theme/stocks', { params: { themeCode, pageNum, pageSize, sort, sortField } }).then(response => response.data).catch(error => {
      console.error('GetDongcaiHotThemeStocks API错误:', error);
      return { success: false, message: error.message };
    });
  },

  GetDongcaiHotBoards: (boardType, pageNumber, pageSize) => {
    return apiClient.get('/theme-plate/dongcai/hot-boards', { params: { boardType, pageNumber, pageSize } }).then(response => response.data).catch(error => {
      console.error('GetDongcaiHotBoards API错误:', error);
      return { success: false, message: error.message };
    });
  },

  GetDongcaiHotBoardStocks: (boardCode, pn, pz) => {
    return apiClient.get('/theme-plate/dongcai/hot-board/stocks', { params: { boardCode, pn, pz } }).then(response => response.data).catch(error => {
      console.error('GetDongcaiHotBoardStocks API错误:', error);
      return { success: false, message: error.message };
    });
  },

  GetStockOverviewList: (date, stockType, listType) => {
    return apiClient.get('/stock-overview/list', { params: { date, stockType, listType } }).then(response => response.data).catch(error => {
      console.error('GetStockOverviewList API错误:', error);
      return { success: false, message: error.message };
    });
  },

  GetStockOverviewDetail: (date, stockCode, stockType, thsListType) => {
    return apiClient.get('/stock-overview/detail', { params: { date, stockCode, stockType, thsListType } }).then(response => response.data).catch(error => {
      console.error('GetStockOverviewDetail API错误:', error);
      return { success: false, message: error.message };
    });
  },

  SyncWuyangData: (date, force) => {
    return apiClient.post('/theme-plate/wuyang/sync', null, { params: { date, force: !!force } }).then(response => response.data).catch(error => {
      console.error('SyncWuyangData API错误:', error);
      return { success: false, message: error.message };
    });
  },

  GetWuyangSubjects: (date) => {
    return apiClient.get('/theme-plate/wuyang/subjects', { params: { date } }).then(response => response.data).catch(error => {
      console.error('GetWuyangSubjects API错误:', error);
      return { success: false, message: error.message };
    });
  },

  GetWuyangStocks: (date, subjectName) => {
    return apiClient.get('/theme-plate/wuyang/stocks', { params: { date, subjectName } }).then(response => response.data).catch(error => {
      console.error('GetWuyangStocks API错误:', error);
      return { success: false, message: error.message };
    });
  },

  SyncMarketDailyStats: (date, force) => {
    return apiClient.get('/market-data/daily/sync', { params: { date, force: !!force } }).then(response => response.data).catch(error => {
      console.error('SyncMarketDailyStats API错误:', error);
      return { success: false, message: error.message };
    });
  },

  GetMarketDailyStats: (date, days) => {
    return apiClient.get('/market-data/daily/list', { params: { date, days } }).then(response => response.data).catch(error => {
      console.error('GetMarketDailyStats API错误:', error);
      return { success: false, message: error.message };
    });
  },

  SyncTdxLbtt: (start, end, force) => {
    return apiClient.get('/tdx-ladder/sync', { params: { start, end, force: !!force } }).then(response => response.data).catch(error => {
      console.error('SyncTdxLbtt API错误:', error);
      return { success: false, message: error.message };
    });
  },

  GetTdxLbtt: (start, end, days) => {
    return apiClient.get('/tdx-ladder/query', { params: { start, end, days } }).then(response => response.data).catch(error => {
      console.error('GetTdxLbtt API错误:', error);
      return { success: false, message: error.message };
    });
  },
};

// 模拟 Wails 事件系统
const EventsEmit = (event, data) => {
  console.log(`Event emitted: ${event}`, data);
};

const EventsOff = (event) => {
  console.log(`Event listener removed: ${event}`);
};

const EventsOn = (event, callback) => {
  console.log(`Event listener added: ${event}`);
};

// 模拟窗口操作函数
const WindowReload = () => {
  window.location.reload();
};

export const {
  GetConfig,
  GetGroupList,
  AddGroup,
  RemoveGroup,
  UpdateGroupSort,
  InitializeGroupSort,
  GetTelegraphList,
  ReFleshTelegraphList,
  GlobalStockIndexes,
  GetDragonTigerList,
  GetFollowList,
  GetStockList,
  Follow,
  UnFollow,
  CheckUpdate,
  GetSponsorInfo,
  GetVersionInfo,
  Greet,
  GetStockKLine,
  GetStockMinutePriceLineData,
  GetStockMoneyTrendByDay,
  GetMoneyRankSina,
  GetIndustryMoneyRankSina,
  GetStockResearchReport,
  GetCompanyNotice,
  GetIndustryResearchReport,
  SearchStock,
  ClsCalendar,
  HotEvent,
  HotStock,
  GetHotTopic,
  GetHotStrategy,
  InvestCalendarTimeLine,
  AddStockGroup,
  RemoveStockGroup,
  GetAiConfigs,
  GetPromptTemplates,
  ChatWithAgent,
  SaveAIResponseResult,
  GetAIResponseResult,
  NewChatStream,
  OpenURL,
  SaveAsMarkdown,
  SaveImage,
  SaveWordFile,
  ShareAnalysis,
  SendDingDingMessageByType,
  AddPrompt,
  DelPrompt,
  ExportConfig,
  UpdateConfig,
  CheckSponsorCode,
  FollowFund,
  GetFollowedFund,
  GetfundList,
  UnFollowFund,
  AnalyzeSentimentWithFreqWeight,
  SetStockSort,
  SetAlarmChangePercent,
  SetCostPriceAndVolume,
  SetStockAICron,
  GetPopularityRank,
  SyncThsHotListDay,
  GetThsHotListDayList,
  SyncDcHotListDay,
  GetDcHotListDayList,
  SyncClsHotListDay,
  GetClsHotListDayList,
  GetThemePlateData,
  GetDongcaiPlates,
  GetDongcaiStocks,
  GetDongcaiHotThemes,
  GetDongcaiHotThemeStocks,
  GetDongcaiHotBoards,
  GetDongcaiHotBoardStocks,
  GetStockOverviewList,
  GetStockOverviewDetail,
  SyncThemePlateData,
  SyncDongcaiData,
  SyncWuyangData,
  GetWuyangSubjects,
  GetWuyangStocks,
  SyncMarketDailyStats,
  GetMarketDailyStats,
  SyncTdxLbtt,
  GetTdxLbtt
} = ApiAdapter;

export {
  EventsEmit,
  EventsOff,
  EventsOn,
  WindowReload
};