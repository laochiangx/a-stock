<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { NCard, NSpace, NButton, NSpin, NEmpty, NScrollbar, NTag, NFlex, NText } from 'naive-ui'

const loading = ref(false)
const newsList = ref([])
const filterType = ref(0) // 0: 全部, 1: 警示
let refreshTimer = null

// 格式化时间
const formatTime = (timestamp) => {
  let date
  if (typeof timestamp === 'number') {
    date = new Date(timestamp * 1000)
  } else if (typeof timestamp === 'string') {
    date = new Date(timestamp)
  } else {
    date = new Date()
  }
  
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  
  return `${month}-${day} ${hours}:${minutes}:${seconds}`
}

// 获取快讯数据
const fetchNews = async () => {
  loading.value = true
  try {
    const url = 'https://baoer-api.xuangubao.cn/api/v6/message/newsflash?subj_ids=9,10,723,35,469,821&platform=pcweb'
    const response = await fetch(url)
    const data = await response.json()
    
    if (data && data.data && data.data.messages) {
      newsList.value = data.data.messages.map(item => ({
        id: item.id,
        title: item.title,
        summary: item.summary,
        createdAt: item.created_at,
        isHot: item.subj_ids && item.subj_ids.includes(10),
        stocks: item.stocks || []
      }))
    }
  } catch (e) {
    console.error('获取快讯失败:', e)
  } finally {
    loading.value = false
  }
}

// 过滤后的新闻列表
const filteredNews = () => {
  if (filterType.value === 1) {
    return newsList.value.filter(item => item.isHot)
  }
  return newsList.value
}

// 切换过滤类型
const setFilter = (type) => {
  filterType.value = type
}

// 刷新
const refresh = () => {
  fetchNews()
}

// 打开选股宝
const openXuangubao = () => {
  window.open('https://xuangubao.cn/live', '_blank')
}

onMounted(() => {
  fetchNews()
  // 每15秒自动刷新
  refreshTimer = setInterval(fetchNews, 15000)
})

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
})
</script>

<template>
  <div class="stock-news-container">
    <!-- 顶部操作栏 -->
    <n-card size="small" class="header-card">
      <n-flex justify="space-between" align="center">
        <n-space>
          <n-button 
            size="small" 
            :type="filterType === 0 ? 'warning' : 'default'"
            @click="setFilter(0)"
          >
            全部
          </n-button>
          <n-button 
            size="small" 
            :type="filterType === 1 ? 'error' : 'default'"
            @click="setFilter(1)"
          >
            警示
          </n-button>
          <n-button size="small" type="success" @click="refresh" :loading="loading">
            刷新
          </n-button>
          <n-button size="small" type="info" @click="openXuangubao">
            选股宝
          </n-button>
        </n-space>
        <n-space>
          <n-tag type="info" size="small">
            共 {{ filteredNews().length }} 条
          </n-tag>
          <n-tag type="success" size="small">
            自动刷新: 15秒
          </n-tag>
        </n-space>
      </n-flex>
    </n-card>

    <!-- 新闻列表 -->
    <n-spin :show="loading">
      <n-scrollbar style="max-height: calc(100vh - 180px);">
        <div v-if="filteredNews().length > 0" class="news-list">
          <div 
            v-for="news in filteredNews()" 
            :key="news.id" 
            class="news-item"
            :class="{ 'hot-news': news.isHot }"
          >
            <div class="news-header">
              <span class="news-time">{{ formatTime(news.createdAt) }}</span>
              <span class="news-title" :class="{ 'hot-title': news.isHot }">
                {{ news.title }}
              </span>
              <n-tag v-if="news.isHot" type="error" size="tiny" style="margin-left: 8px;">
                警示
              </n-tag>
            </div>
            <div class="news-summary">
              {{ news.summary }}
            </div>
            <div v-if="news.stocks && news.stocks.length > 0" class="news-stocks">
              <span 
                v-for="stock in news.stocks" 
                :key="stock.symbol"
                class="stock-tag"
              >
                {{ stock.name }}
              </span>
            </div>
          </div>
        </div>
        <n-empty v-else-if="!loading" description="暂无快讯" />
      </n-scrollbar>
    </n-spin>
  </div>
</template>

<style scoped>
.stock-news-container {
  padding: 0;
  background-color: #000;
  min-height: calc(100vh - 120px);
}

.header-card {
  margin-bottom: 8px;
  background-color: #1a1a1a !important;
}

:deep(.n-card) {
  background-color: #1a1a1a;
}

.news-list {
  padding: 8px;
}

.news-item {
  padding: 10px;
  margin-bottom: 10px;
  border-bottom: 1px solid #333;
}

.news-item.hot-news {
  border-left: 3px solid #CD2626;
  padding-left: 10px;
}

.news-header {
  margin-bottom: 5px;
}

.news-time {
  font-size: 13px;
  color: #16D8E0;
  margin-right: 10px;
  font-weight: bold;
}

.news-title {
  font-size: 15px;
  color: #EEB422;
}

.news-title.hot-title {
  color: #FF4444;
}

.news-summary {
  font-size: 14px;
  color: #F2F2F2;
  line-height: 1.6;
  margin: 8px 0;
}

.news-stocks {
  margin-top: 8px;
}

.stock-tag {
  display: inline-block;
  font-size: 14px;
  color: #F2F2F2;
  background-color: #1D232D;
  border: 1px solid #16D8E0;
  border-radius: 4px;
  padding: 4px 10px;
  margin-right: 8px;
  margin-bottom: 4px;
  cursor: pointer;
  transition: all 0.2s;
}

.stock-tag:hover {
  color: #EE5C42;
  background-color: #2A3441;
  border-color: #EEB422;
}
</style>
