<script setup>
import { onMounted, onUnmounted, ref, watch, nextTick, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as echarts from "echarts";
import { 
  GetTelegraphList, GetConfig, 
  GlobalStockIndexes, AnalyzeSentimentWithFreqWeight 
} from "../api/api";
import NewsList from "./newsList.vue";
import LongTigerRankList from "./LongTigerRankList.vue";
import HotTopics from "./HotTopics.vue";
import SelectStock from "./SelectStock.vue";
import IndustryMoneyRank from "./industryMoneyRank.vue";
import StockMoneyRank from "./StockMoneyRank.vue";
import StockResearchReportList from "./StockResearchReportList.vue";
import StockNoticeList from "./StockNoticeList.vue";
import IndustryResearchReportList from "./IndustryResearchReportList.vue";
import EmbeddedUrl from "./EmbeddedUrl.vue";
import { 
  NCard, NGrid, NGi, useMessage, NTag, NText, NCollapse, NCollapseItem, NFlex, 
  NImage, NNumberAnimation, NButton, NScrollbar, NEmpty, NSpace, NIcon
} from "naive-ui";

const message = useMessage()
const route = useRoute()
const router = useRouter()

const telegraphList = ref([])
const sinaNewsList = ref([])
const ggtNewsList = ref([])
const thsNewsList = ref([])
const nowTab = ref(route.query.name || "市场快讯")
const stockFlowSort = ref(route.query.sort || "netamount")
const industryRankSort = ref(route.query.sort || "0")
const darkTheme = ref(false)

const hotwordSource = ref('全部')
const hotwordSources = ['全部', '财联社电报', '新浪财经', '股通快速', '同花顺快讯']

// 主菜单配置
const mainMenus = [
  { key: '市场快讯', label: '市场快讯' },
  { key: '全球股指', label: '全球股指' },
  { key: '重大指数', label: '重大指数' },
  { key: '行业排名', label: '行业排名' },
  { key: '个股资金流向', label: '个股资金流向' },
  { key: '龙虎榜', label: '龙虎榜' },
  { key: '个股研报', label: '个股研报' },
  { key: '公司公告', label: '公司公告' },
  { key: '行业研究', label: '行业研究' },
  { key: '当前热门', label: '当前热门' },
  { key: '指标选股', label: '指标选股' },
  { key: '名站优选', label: '名站优选' },
]

// 子菜单配置
const subMenus = {
  '个股资金流向': [
    { key: 'netamount', label: '净流入额排名' },
    { key: 'outamount', label: '流出资金排名' },
    { key: 'ratioamount', label: '净流入率排名' },
    { key: 'r0_net', label: '主力净流入额排名' },
    { key: 'r0_out', label: '主力流出排名' },
    { key: 'r0_ratio', label: '主力净流入率排名' },
    { key: 'r3_net', label: '散户净流入额排名' },
    { key: 'r3_out', label: '散户流出排名' },
    { key: 'r3_ratio', label: '散户净流入率排名' },
  ],
  '行业排名': [
    { key: '0', label: '行业排名' },
    { key: '1', label: '概念排名' },
    { key: '2', label: '地域排名' },
  ],
}

const currentSubMenus = computed(() => subMenus[nowTab.value] || [])
const currentSubKey = computed(() => {
  if (nowTab.value === '个股资金流向') return stockFlowSort.value
  if (nowTab.value === '行业排名') return industryRankSort.value
  return ''
})

const switchMainMenu = (key) => {
  nowTab.value = key
  router.push({ name: 'market', query: { name: key } })
}

const switchSubMenu = (key) => {
  if (nowTab.value === '个股资金流向') {
    stockFlowSort.value = key
    router.push({ name: 'market', query: { name: nowTab.value, sort: key } })
  } else if (nowTab.value === '行业排名') {
    industryRankSort.value = key
    router.push({ name: 'market', query: { name: nowTab.value, sort: key } })
  }
}

// 全球股指相关数据
const america = ref([])
const europe = ref([])
const asia = ref([])
const other = ref([])
const common = ref([])   // 重大指数（common字段）
const mainIndex = ref([])  // 主要指数（用于顶部显示）

// 图表相关
const chartRef = ref(null)
const gaugeChartRef = ref(null)
const chartHeight = 380
let handleChartInterval = null
let handleIndexInterval = null
let treemapChart = null
let gaugeChart = null

// 获取全球股指数据
const getIndex = () => {
  GlobalStockIndexes().then((res) => {
    america.value = res["america"] || []
    europe.value = res["europe"] || []
    asia.value = res["asia"] || []
    other.value = res["other"] || []
    common.value = res["common"] || []  // 重大指数
    
    // 主要指数（全球重要指数）
    const mainLocations = ['上海', '深圳', '香港', '台湾', '北京', '东京', '首尔', '纽约', '纳斯达克']
    mainIndex.value = [
      ...(asia.value || []).filter(item => mainLocations.some(loc => item.name?.includes(loc))),
      ...(america.value || []).filter(item => mainLocations.some(loc => item.name?.includes(loc)))
    ]
  }).catch(error => console.error('Error in getIndex:', error))
}

// 处理图表
const handleChart = () => {
  if (!chartRef.value || !gaugeChartRef.value) return
  AnalyzeSentimentWithFreqWeight("", hotwordSource.value).then((res) => {
    if (!res || !res.frequencies) return
    if (!treemapChart) treemapChart = echarts.init(chartRef.value)
    if (!gaugeChart) gaugeChart = echarts.init(gaugeChartRef.value)
    
    // 根据词汇类型设置颜色（中国股市：红涨绿跌）
    let data = res['frequencies'].map(item => {
      let itemColor = '#5470c6' // 默认蓝色
      if (item.WordType === 'positive') itemColor = '#ee6666' // 红色（正面/利好）
      else if (item.WordType === 'negative') itemColor = '#91cc75' // 绿色（负面/利空）
      else if (item.WordType === 'industry') itemColor = '#fac858' // 黄色（行业）
      else if (item.WordType === 'concept') itemColor = '#73c0de' // 青色（概念）
      
      return { 
        name: item.Word, 
        frequency: item.Frequency, 
        weight: item.Weight, 
        value: item.Score,
        wordType: item.WordType,
        sentiment: item.SentimentValue,
        industry: item.Industry,
        itemStyle: { color: itemColor }
      }
    })
    
    treemapChart.setOption({
      darkMode: darkTheme.value,
      title: { 
        text: hotwordSource.value && hotwordSource.value !== '全部' ? `最近24小时热词（${hotwordSource.value}）` : '最近24小时热词', 
        left: 'center', 
        textStyle: { color: darkTheme.value ? '#ccc' : '#456' } 
      },
      legend: { 
        show: true,
        bottom: 5,
        data: [
          { name: '正面', itemStyle: { color: '#ee6666' } },
          { name: '负面', itemStyle: { color: '#91cc75' } },
          { name: '行业', itemStyle: { color: '#fac858' } },
          { name: '概念', itemStyle: { color: '#73c0de' } },
          { name: '普通', itemStyle: { color: '#5470c6' } }
        ],
        textStyle: { color: darkTheme.value ? '#ccc' : '#456' }
      },
      toolbox: { 
        left: '20px', 
        feature: { 
          saveAsImage: { title: '保存图片' }, 
          restore: { title: '默认' } 
        } 
      },
      tooltip: { 
        formatter: (info) => {
          let typeText = info.data.wordType === 'positive' ? '正面' : 
                        info.data.wordType === 'negative' ? '负面' : 
                        info.data.wordType === 'industry' ? '行业' : 
                        info.data.wordType === 'concept' ? '概念' : '普通'
          let html = '<div style="font-weight:bold">' + info.name + '</div>'
          html += '<div>类型: ' + typeText + '</div>'
          html += '<div>热度: ' + info.value.toFixed(2) + '</div>'
          html += '<div>频次: ' + info.data.frequency + '</div>'
          html += '<div>权重: ' + info.data.weight + '</div>'
          if (info.data.sentiment !== 0) {
            html += '<div>情感: ' + (info.data.sentiment > 0 ? '+' : '') + info.data.sentiment.toFixed(2) + '</div>'
          }
          if (info.data.industry) {
            html += '<div>行业: ' + info.data.industry + '</div>'
          }
          return html
        }
      },
      series: [{ 
        type: 'treemap', 
        breadcrumb: { show: false }, 
        left: '0', 
        top: '40', 
        right: '0', 
        bottom: '30', 
        data: data,
        label: {
          show: true,
          formatter: '{b}'
        },
        upperLabel: {
          show: true,
          height: 30
        }
      }]
    })
    
    // 情绪仪表盘增强
    let sentimentScore = res.result.Score || 0
    let positiveCount = res.result.PositiveCount || 0
    let negativeCount = res.result.NegativeCount || 0
    let ratio = res.result.Ratio || 0
    
    gaugeChart.setOption({
      darkMode: darkTheme.value,
      title: {
        text: `正面: ${positiveCount} | 负面: ${negativeCount} | 比例: ${ratio.toFixed(2)}`,
        left: 'center',
        top: '85%',
        textStyle: { 
          fontSize: 14,
          color: darkTheme.value ? '#ccc' : '#456'
        }
      },
      series: [{
        type: 'gauge', startAngle: 180, endAngle: 0, center: ['50%', '75%'], radius: '90%', min: -100, max: 100, splitNumber: 8,
        axisLine: { lineStyle: { width: 6, color: [[0.25, '#91cc75'], [0.5, '#58e1f9'], [0.75, '#fac858'], [1, '#ee6666']] } },
        pointer: { icon: 'path://M12.8,0.7l12,40.1H0.7L12.8,0.7z', length: '12%', width: 20, offsetCenter: [0, '-60%'], itemStyle: { color: 'auto' } },
        axisTick: { length: 12, lineStyle: { color: 'auto', width: 2 } },
        splitLine: { length: 20, lineStyle: { color: 'auto', width: 5 } },
        axisLabel: { color: darkTheme.value ? '#ccc' : '#456', fontSize: 18, distance: -45, rotate: 'tangential', formatter: (v) => v === 100 ? '极热' : v === 50 ? '乐观' : v === 0 ? '中性' : v === -50 ? '谨慎' : v === -100 ? '冰点' : '' },
        title: { offsetCenter: [0, '-10%'], fontSize: 18 },
        detail: { fontSize: 28, offsetCenter: [0, '-35%'], valueAnimation: true, formatter: (v) => v.toFixed(2), color: 'inherit' },
        data: [{ value: sentimentScore * 0.2, name: '市场情绪强弱' }]
      }]
    })
  }).catch(error => console.error('Error in handleChart:', error))
}

const fetchData = async () => {
  try {
    const [telegraphData, sinaData, ggtData, thsData] = await Promise.all([
      GetTelegraphList("财联社电报"), 
      GetTelegraphList("新浪财经"), 
      GetTelegraphList("股通快速"),
      GetTelegraphList("同花顺快讯")
    ])
    telegraphList.value = telegraphData || []
    sinaNewsList.value = sinaData || []
    ggtNewsList.value = ggtData || []
    thsNewsList.value = thsData || []
  } catch (error) { 
    telegraphList.value = []
    sinaNewsList.value = []
    ggtNewsList.value = []
    thsNewsList.value = []
  }
}

const fetchConfig = async () => {
  try { const config = await GetConfig(); darkTheme.value = config.darkTheme || false } 
  catch (error) { darkTheme.value = false }
}

const cleanupCharts = () => { if (treemapChart) { treemapChart.dispose(); treemapChart = null } if (gaugeChart) { gaugeChart.dispose(); gaugeChart = null } }
const cleanupIntervals = () => { if (handleChartInterval) { clearInterval(handleChartInterval); handleChartInterval = null } if (handleIndexInterval) { clearInterval(handleIndexInterval); handleIndexInterval = null } }

const initMarketNews = async () => {
  await fetchData()
  await nextTick()
  setTimeout(() => handleChart(), 200)
  handleChartInterval = setInterval(handleChart, 60000)
}

const initGlobalIndex = () => {
  getIndex()
  handleIndexInterval = setInterval(getIndex, 5000)
}

const initMajorIndex = () => {
  getIndex()
  handleIndexInterval = setInterval(getIndex, 5000)
}

onMounted(async () => {
  await fetchConfig()
  const currentTab = route.query.name || "市场快讯"
  nowTab.value = currentTab
  if (route.query.sort) {
    if (currentTab === '个股资金流向') stockFlowSort.value = route.query.sort
    if (currentTab === '行业排名') industryRankSort.value = route.query.sort
  }
  
  if (currentTab === '市场快讯') await initMarketNews()
  else if (currentTab === '全球股指') initGlobalIndex()
  else if (currentTab === '重大指数') initMajorIndex()
})

onUnmounted(() => { cleanupIntervals(); cleanupCharts() })

watch(() => route.query.name, async (newName) => {
  const currentTab = newName || "市场快讯"
  nowTab.value = currentTab
  cleanupIntervals(); cleanupCharts()
  
  if (currentTab === '市场快讯') { await nextTick(); await initMarketNews() }
  else if (currentTab === '全球股指') initGlobalIndex()
  else if (currentTab === '重大指数') initMajorIndex()
}, { immediate: false })

watch(() => route.query.sort, (newSort) => {
  if (newSort) {
    if (nowTab.value === '个股资金流向') stockFlowSort.value = newSort
    if (nowTab.value === '行业排名') industryRankSort.value = newSort
  }
}, { immediate: false })

watch(hotwordSource, () => {
  if (nowTab.value === '市场快讯') {
    handleChart()
  }
}, { immediate: false })

const favSites = [
  // 可嵌入的网站
  { name: 'TopHub今日热榜', url: 'https://tophub.today/', desc: '全网热点聚合', canEmbed: true },
  { name: '摸鱼', url: 'https://moyu.im/', desc: '摸鱼热榜', canEmbed: true },
  { name: '韭研公社', url: 'https://www.jiuyangongshe.com/', desc: '投资研究社区', canEmbed: true },
  // 不可嵌入的网站（会被X-Frame-Options阻止）
  { name: '东方财富', url: 'https://www.eastmoney.com/', desc: '财经门户', canEmbed: false },
  { name: '同花顺', url: 'https://www.10jqka.com.cn/', desc: '股票软件', canEmbed: false },
  { name: '雪球', url: 'https://xueqiu.com/', desc: '投资社区', canEmbed: false },
  { name: '新浪财经', url: 'https://finance.sina.com.cn/', desc: '财经新闻', canEmbed: false },
  { name: '腾讯财经', url: 'https://finance.qq.com/', desc: '财经资讯', canEmbed: false },
  { name: '网易财经', url: 'https://money.163.com/', desc: '财经频道', canEmbed: false },
  { name: '巨潮资讯', url: 'http://www.cninfo.com.cn/', desc: '上市公司公告', canEmbed: false },
  { name: '上交所', url: 'http://www.sse.com.cn/', desc: '上海证券交易所', canEmbed: false },
  { name: '深交所', url: 'http://www.szse.cn/', desc: '深圳证券交易所', canEmbed: false },
  { name: '中国证监会', url: 'http://www.csrc.gov.cn/', desc: '监管机构', canEmbed: false },
  { name: '选股通', url: 'https://xuangutong.com/', desc: '智能选股工具', canEmbed: false },
  { name: '百度股市通', url: 'https://gushitong.baidu.com/', desc: '百度股票行情', canEmbed: false },
  { name: '东财大盘星图', url: 'https://emdatah5.eastmoney.com/dc/xingtu/index.html', desc: '大盘资金流向', canEmbed: false },
  { name: '财联社行情数据', url: 'https://www.cls.cn/stock', desc: '财联社股票行情', canEmbed: false },
  { name: '财联社消息墙', url: 'https://www.cls.cn/telegraph', desc: '财联社电报', canEmbed: false },
  { name: '理杏仁', url: 'https://www.lixinger.com/', desc: '价值投资工具', canEmbed: false },
  { name: '集思录', url: 'https://www.jisilu.cn/', desc: '低风险投资', canEmbed: false },
]

// 内嵌网页相关
const selectedSite = ref(null)
const embeddedUrl = ref('')

const selectSite = (site) => {
  if (site.canEmbed) {
    selectedSite.value = site
    embeddedUrl.value = site.url
  } else {
    // 不可嵌入的网站直接新窗口打开
    window.open(site.url, '_blank')
  }
}

const closeSite = () => {
  selectedSite.value = null
  embeddedUrl.value = ''
}

const openInNewTab = (url) => window.open(url, '_blank')
</script>

<template>
  <div>
    <!-- 顶部主菜单 -->
    <n-space style="margin-bottom: 8px; padding: 8px 0; border-bottom: 1px solid #e8e8e8;">
      <n-button v-for="menu in mainMenus" :key="menu.key" 
        :type="nowTab === menu.key ? 'primary' : 'default'" 
        text size="small" @click="switchMainMenu(menu.key)">
        {{ menu.label }}
      </n-button>
    </n-space>

    <!-- 子菜单 -->
    <n-space v-if="currentSubMenus.length > 0" style="margin-bottom: 12px; padding: 4px 0;">
      <n-button v-for="sub in currentSubMenus" :key="sub.key"
        :type="currentSubKey === sub.key ? 'success' : 'default'"
        text size="small" @click="switchSubMenu(sub.key)">
        {{ sub.label }}
      </n-button>
    </n-space>

    <!-- 市场快讯：情绪仪表盘 + 热词图 + 新闻 -->
    <div v-if="nowTab === '市场快讯'">
      <n-space style="margin-bottom: 8px;">
        <n-button v-for="s in hotwordSources" :key="s" :type="hotwordSource === s ? 'primary' : 'default'" text size="small" @click="hotwordSource = s">
          {{ s }}
        </n-button>
      </n-space>
      <n-grid :cols="24" :y-gap="0" style="margin-bottom: 12px;">
        <n-gi span="6"><div ref="gaugeChartRef" :style="{ height: chartHeight + 'px' }"></div></n-gi>
        <n-gi span="18"><div ref="chartRef" :style="{ height: chartHeight + 'px' }"></div></n-gi>
      </n-grid>
      <n-grid :cols="4" :x-gap="12">
        <n-gi>
          <n-card title="财联社电报" size="small">
            <n-scrollbar style="max-height: 400px;"><NewsList :news-list="telegraphList" /></n-scrollbar>
          </n-card>
        </n-gi>
        <n-gi>
          <n-card title="新浪财经" size="small">
            <n-scrollbar style="max-height: 400px;"><NewsList :news-list="sinaNewsList" /></n-scrollbar>
          </n-card>
        </n-gi>
        <n-gi>
          <n-card title="股通快速新闻" size="small">
            <n-scrollbar style="max-height: 400px;"><NewsList :news-list="ggtNewsList" /></n-scrollbar>
          </n-card>
        </n-gi>
        <n-gi>
          <n-card title="同花顺快讯" size="small">
            <n-scrollbar style="max-height: 400px;"><NewsList :news-list="thsNewsList" /></n-scrollbar>
          </n-card>
        </n-gi>
      </n-grid>
    </div>

    <!-- 全球股指：亚洲、美洲、欧洲、其他 -->
    <div v-else-if="nowTab === '全球股指'">
      <n-collapse :trigger-areas="['main','extra','arrow']" :default-expanded-names="['asia', 'america', 'europe', 'other']">
        <n-collapse-item name="asia" v-if="asia.length > 0">
          <template #header><n-text strong>亚洲股指</n-text></template>
          <n-grid :cols="6" :x-gap="8" :y-gap="8">
            <n-gi v-for="item in asia" :key="item.code">
              <n-card size="small" :bordered="true">
                <n-flex align="center" :wrap="false">
                  <n-image :width="24" :src="item.img" preview-disabled style="flex-shrink: 0;" />
                  <div style="flex: 1; min-width: 0;">
                    <n-text style="font-size: 13px; display: block;">{{ item.name }}</n-text>
                    <n-flex align="center">
                      <n-text strong>{{ item.zxj }}</n-text>
                      <n-text :type="item.zdf >= 0 ? 'error' : 'success'" style="font-size: 12px;">{{ item.zdf >= 0 ? '+' : '' }}{{ item.zdf?.toFixed(2) }}%</n-text>
                    </n-flex>
                  </div>
                </n-flex>
              </n-card>
            </n-gi>
          </n-grid>
        </n-collapse-item>
        <n-collapse-item name="america" v-if="america.length > 0">
          <template #header><n-text strong>美洲股指</n-text></template>
          <n-grid :cols="6" :x-gap="8" :y-gap="8">
            <n-gi v-for="item in america" :key="item.code">
              <n-card size="small" :bordered="true">
                <n-flex align="center" :wrap="false">
                  <n-image :width="24" :src="item.img" preview-disabled style="flex-shrink: 0;" />
                  <div style="flex: 1; min-width: 0;">
                    <n-text style="font-size: 13px; display: block;">{{ item.name }}</n-text>
                    <n-flex align="center">
                      <n-text strong>{{ item.zxj }}</n-text>
                      <n-text :type="item.zdf >= 0 ? 'error' : 'success'" style="font-size: 12px;">{{ item.zdf >= 0 ? '+' : '' }}{{ item.zdf?.toFixed(2) }}%</n-text>
                    </n-flex>
                  </div>
                </n-flex>
              </n-card>
            </n-gi>
          </n-grid>
        </n-collapse-item>
        <n-collapse-item name="europe" v-if="europe.length > 0">
          <template #header><n-text strong>欧洲股指</n-text></template>
          <n-grid :cols="6" :x-gap="8" :y-gap="8">
            <n-gi v-for="item in europe" :key="item.code">
              <n-card size="small" :bordered="true">
                <n-flex align="center" :wrap="false">
                  <n-image :width="24" :src="item.img" preview-disabled style="flex-shrink: 0;" />
                  <div style="flex: 1; min-width: 0;">
                    <n-text style="font-size: 13px; display: block;">{{ item.name }}</n-text>
                    <n-flex align="center">
                      <n-text strong>{{ item.zxj }}</n-text>
                      <n-text :type="item.zdf >= 0 ? 'error' : 'success'" style="font-size: 12px;">{{ item.zdf >= 0 ? '+' : '' }}{{ item.zdf?.toFixed(2) }}%</n-text>
                    </n-flex>
                  </div>
                </n-flex>
              </n-card>
            </n-gi>
          </n-grid>
        </n-collapse-item>
        <n-collapse-item name="other" v-if="other.length > 0">
          <template #header><n-text strong>其他股指</n-text></template>
          <n-grid :cols="6" :x-gap="8" :y-gap="8">
            <n-gi v-for="item in other" :key="item.code">
              <n-card size="small" :bordered="true">
                <n-flex align="center" :wrap="false">
                  <n-image :width="24" :src="item.img" preview-disabled style="flex-shrink: 0;" />
                  <div style="flex: 1; min-width: 0;">
                    <n-text style="font-size: 13px; display: block;">{{ item.name }}</n-text>
                    <n-flex align="center">
                      <n-text strong>{{ item.zxj }}</n-text>
                      <n-text :type="item.zdf >= 0 ? 'error' : 'success'" style="font-size: 12px;">{{ item.zdf >= 0 ? '+' : '' }}{{ item.zdf?.toFixed(2) }}%</n-text>
                    </n-flex>
                  </div>
                </n-flex>
              </n-card>
            </n-gi>
          </n-grid>
        </n-collapse-item>
      </n-collapse>
    </div>

    <!-- 重大指数：common字段的指数 -->
    <div v-else-if="nowTab === '重大指数'">
      <n-card title="重大指数" size="small">
        <n-flex :wrap="true">
          <n-tag size="large" :bordered="false" v-for="item in common" :key="item.code" :type="item.zdf > 0 ? 'error' : 'success'" style="margin: 4px;">
            <n-flex align="center">
              <n-image v-if="item.img" :width="24" :src="item.img" preview-disabled />
              <n-text style="font-size: 14px; margin-left: 4px;" :type="item.zdf > 0 ? 'error' : 'success'">{{ item.name }}</n-text>
              <n-text strong style="font-size: 16px; margin-left: 8px;">{{ item.zxj }}</n-text>
              <n-number-animation :precision="2" :from="0" :to="item.zdf" style="font-size: 14px; margin-left: 8px;" />
              <n-text style="font-size: 14px" :type="item.zdf > 0 ? 'error' : 'success'">%</n-text>
            </n-flex>
          </n-tag>
        </n-flex>
      </n-card>
    </div>

    <!-- 行业排名 -->
    <div v-else-if="nowTab === '行业排名'">
      <IndustryMoneyRank :header-title="industryRankSort === '0' ? '行业资金排名' : industryRankSort === '1' ? '概念资金排名' : '地域资金排名'" :fenlei="industryRankSort" sort="netamount" />
    </div>

    <!-- 个股资金流向 -->
    <div v-else-if="nowTab === '个股资金流向'"><StockMoneyRank :sort-type="stockFlowSort" /></div>

    <!-- 龙虎榜 -->
    <div v-else-if="nowTab === '龙虎榜'"><LongTigerRankList /></div>

    <!-- 个股研报 -->
    <div v-else-if="nowTab === '个股研报'"><StockResearchReportList /></div>

    <!-- 公司公告 -->
    <div v-else-if="nowTab === '公司公告'"><StockNoticeList /></div>

    <!-- 行业研究 -->
    <div v-else-if="nowTab === '行业研究'"><IndustryResearchReportList /></div>

    <!-- 当前热门 -->
    <div v-else-if="nowTab === '当前热门'"><HotTopics /></div>

    <!-- 指标选股 -->
    <div v-else-if="nowTab === '指标选股'"><SelectStock /></div>

    <!-- 名站优选 -->
    <div v-else-if="nowTab === '名站优选'">
      <n-grid :cols="5" :x-gap="12" :y-gap="12">
        <n-gi v-for="site in favSites" :key="site.name">
          <n-card hoverable style="cursor: pointer;" 
            :class="{ 'selected-site': selectedSite?.name === site.name }"
            @click="selectSite(site)">
            <n-flex justify="space-between" align="start">
              <div>
                <n-text strong>{{ site.name }}</n-text>
                <n-text depth="3" style="display: block; font-size: 12px;">{{ site.desc }}</n-text>
              </div>
              <n-tag v-if="!site.canEmbed" size="small" type="info">外链</n-tag>
            </n-flex>
          </n-card>
        </n-gi>
      </n-grid>
      
      <!-- 内嵌网页区域 -->
      <div v-if="selectedSite" style="margin-top: 16px;">
        <n-card size="small">
          <template #header>
            <n-flex justify="space-between" align="center">
              <n-text strong>{{ selectedSite.name }}</n-text>
              <n-space>
                <n-button size="small" @click="openInNewTab(selectedSite.url)">新窗口打开</n-button>
                <n-button size="small" @click="closeSite">关闭</n-button>
              </n-space>
            </n-flex>
          </template>
          <EmbeddedUrl :url="embeddedUrl" height="600px" />
        </n-card>
      </div>
    </div>

    <div v-else><n-empty description="请选择一个页面" /></div>
  </div>
</template>

<style scoped>
.n-card { border-radius: 8px; }
.selected-site { border: 2px solid #18a058; background-color: rgba(24, 160, 88, 0.1); }
</style>
