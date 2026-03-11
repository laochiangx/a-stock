<script setup>
import { ref, onMounted } from 'vue'
import { NCard, NSpace, NButton, NTag, NFlex, NGrid, NGi } from 'naive-ui'

const hotSites = ref([
  {
    name: '通达信热榜',
    url: 'https://pul.tdx.com.cn/site/app/gzhbd/tdx-topsearch/page-main.html?pageName=page_topsearch&tabClickIndex=0&subtabIndex=0&color=black',
    canEmbed: true
  },
  {
    name: '东方财富热榜',
    url: 'https://vipmoney.eastmoney.com/collect/app_ranking/ranking/app.html?hashcode=20220117&market=#/stock',
    canEmbed: true
  },
  {
    name: '财联社热榜',
    url: 'https://api3.cls.cn/quote/toplist?app=cailianpress&sv=835&os=android',
    canEmbed: true
  },
  {
    name: '同花顺热榜',
    url: 'https://eq.10jqka.com.cn/frontend/thsTopRank/index.html?fontzoom=no#/',
    canEmbed: true
  }
])

const activeTab = ref('all') // all: 全部, 或单个站点名称
const selectedSite = ref(null)

// 刷新所有iframe
const refreshAll = () => {
  const iframes = document.querySelectorAll('.hot-iframe')
  iframes.forEach(iframe => {
    iframe.src = iframe.src
  })
}

// 切换显示模式
const showAll = () => {
  activeTab.value = 'all'
  selectedSite.value = null
}

// 显示单个站点
const showSingle = (site) => {
  activeTab.value = site.name
  selectedSite.value = site
}

// 在新窗口打开
const openInNewWindow = (site) => {
  window.open(site.url, '_blank')
}
</script>

<template>
  <div class="hot-rank-container">
    <!-- 顶部操作栏 -->
    <n-card size="small" class="header-card">
      <n-flex justify="space-between" align="center">
        <n-space>
          <n-button 
            size="small" 
            :type="activeTab === 'all' ? 'primary' : 'default'"
            @click="showAll"
          >
            全部显示
          </n-button>
          <n-button 
            v-for="site in hotSites"
            :key="site.name"
            size="small" 
            :type="activeTab === site.name ? 'primary' : 'default'"
            @click="showSingle(site)"
          >
            {{ site.name }}
          </n-button>
        </n-space>
        <n-space>
          <n-button size="small" type="success" @click="refreshAll">
            刷新
          </n-button>
        </n-space>
      </n-flex>
    </n-card>

    <!-- 全部显示模式 - 四列并排 -->
    <div v-if="activeTab === 'all'" class="all-view">
      <div class="iframe-grid">
        <div v-for="site in hotSites" :key="site.name" class="iframe-column">
          <div class="column-header">
            <n-tag type="info" size="small">{{ site.name }}</n-tag>
            <n-button size="tiny" quaternary @click="openInNewWindow(site)">
              新窗口
            </n-button>
          </div>
          <iframe 
            class="hot-iframe"
            :src="site.url"
            frameborder="0"
            width="100%"
            height="100%"
          ></iframe>
        </div>
      </div>
    </div>

    <!-- 单个显示模式 -->
    <div v-else-if="selectedSite" class="single-view">
      <div class="single-header">
        <n-tag type="info">{{ selectedSite.name }}</n-tag>
        <n-button size="small" quaternary @click="openInNewWindow(selectedSite)">
          在新窗口打开
        </n-button>
      </div>
      <iframe 
        class="hot-iframe single-iframe"
        :src="selectedSite.url"
        frameborder="0"
        width="100%"
        height="100%"
      ></iframe>
    </div>
  </div>
</template>

<style scoped>
.hot-rank-container {
  padding: 0;
  background-color: #000;
  height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
}

.header-card {
  margin-bottom: 8px;
  background-color: #1a1a1a !important;
  flex-shrink: 0;
}

:deep(.n-card) {
  background-color: #1a1a1a;
}

.all-view {
  flex: 1;
  overflow: hidden;
}

.iframe-grid {
  display: flex;
  gap: 4px;
  height: 100%;
  padding: 0 4px 4px 4px;
}

.iframe-column {
  flex: 1;
  display: flex;
  flex-direction: column;
  border: 1px solid #333;
  border-radius: 4px;
  overflow: hidden;
  min-width: 0;
}

.column-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 8px;
  background-color: #1a1a1a;
  border-bottom: 1px solid #333;
}

.hot-iframe {
  flex: 1;
  width: 100%;
  border: none;
  background-color: #fff;
}

.single-view {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 0 4px 4px 4px;
}

.single-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px;
  background-color: #1a1a1a;
  border: 1px solid #333;
  border-bottom: none;
  border-radius: 4px 4px 0 0;
}

.single-iframe {
  flex: 1;
  border: 1px solid #333;
  border-radius: 0 0 4px 4px;
}
</style>
