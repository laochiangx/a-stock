<script setup>
import { ref, onMounted } from 'vue'
import { NCard, NSpace, NButton, NScrollbar, NTag, NFlex } from 'naive-ui'

// 板块轮动链接配置
const rotationLinks = ref([
  { name: '今热', url: 'http://page.tdx.com.cn:7615/site/pcwebcall/html/pc_tcld_bkzqb.html?flag=10&pageid=1&color=0&bkcolor=000000', canEmbed: true, color: '#FFB871' },
  { name: '今主题', url: 'http://hot.icfqs.com:7615/site/tdx-pc-hqpage/page-rdzt.html?switchrefresh=1&color=0&bkcolor=000000', canEmbed: true, color: '#80FFFF' },
  { name: '热醒', url: 'http://page.tdx.com.cn:7615/site/pcwebcall/html/pc_tcld_rdbk.html?color=0&bkcolor=000000', canEmbed: true, color: '#07c160' },
  { name: '热播', url: 'http://hot.icfqs.com:7615/site/tdx-pc-find/page_sjqd.html?color=0&bkcolor=000000', canEmbed: true, color: '#FFFFFF' },
  { name: '热榜', url: 'https://pul.tdx.com.cn/site/app/gzhbd/tdx-topsearch/page-main.html?pageName=page_topsearch&tabClickIndex=0&subtabIndex=0&color=black', canEmbed: true, color: '#FC2944' },
  { name: '聚资', url: 'http://excalc.icfqs.com:7616/site/pcwebcall/html/pc_zjlx_zjjj.html?color=0&bkcolor=000000&nomenu=1', canEmbed: true, color: '#FFD700' },
  { name: '涨停', url: 'https://xuangubao.cn/zhangting', canEmbed: false, color: '#FF00FF' },
  { name: '连板', url: 'https://xuangubao.cn/lianban', canEmbed: false, color: '#FFFFFF' },
  { name: '龙虎榜', url: 'http://vis-free.10jqka.com.cn/billboard/indexV3.html#/index', canEmbed: false, color: '#FFB871' },
  { name: '板块', url: 'https://xuangubao.cn/sector', canEmbed: false, color: '#80FFFF' },
  { name: '异动', url: 'https://xuangubao.cn/', canEmbed: false, color: '#FFD700' },
  { name: '资金流', url: 'https://data.eastmoney.com/zjlx/detail.html', canEmbed: false, color: '#07c160' },
  { name: '概念', url: 'http://q.10jqka.com.cn/gn/', canEmbed: false, color: '#FC2944' },
  { name: '行业', url: 'http://q.10jqka.com.cn/thshy/', canEmbed: false, color: '#80FFFF' },
])

const activeLink = ref(null)
const showIframe = ref(false)
const iframeUrl = ref('')

// 点击链接
const handleLinkClick = (link) => {
  activeLink.value = link.name
  if (link.canEmbed) {
    showIframe.value = true
    iframeUrl.value = link.url
  } else {
    window.open(link.url, '_blank')
  }
}

// 关闭iframe
const closeIframe = () => {
  showIframe.value = false
  iframeUrl.value = ''
  activeLink.value = null
}

// 刷新iframe
const refreshIframe = () => {
  const iframe = document.getElementById('rotation-iframe')
  if (iframe) {
    iframe.src = iframe.src
  }
}

onMounted(() => {
  // 默认加载第一个可嵌入的链接
  const firstEmbeddable = rotationLinks.value.find(l => l.canEmbed)
  if (firstEmbeddable) {
    handleLinkClick(firstEmbeddable)
  }
})
</script>

<template>
  <div class="rotation-container">
    <!-- 顶部链接栏 -->
    <n-card size="small" style="margin-bottom: 8px;">
      <n-scrollbar x-scrollable>
        <n-space :wrap="false">
          <n-button
            v-for="link in rotationLinks"
            :key="link.name"
            :type="activeLink === link.name ? 'primary' : 'default'"
            size="small"
            :style="{ borderColor: link.color, color: activeLink === link.name ? '#fff' : link.color }"
            @click="handleLinkClick(link)"
          >
            {{ link.name }}
            <n-tag v-if="!link.canEmbed" size="tiny" type="warning" style="margin-left: 4px;">外链</n-tag>
          </n-button>
        </n-space>
      </n-scrollbar>
    </n-card>

    <!-- 操作栏 -->
    <n-card size="small" style="margin-bottom: 8px;" v-if="showIframe">
      <n-flex justify="space-between" align="center">
        <n-space>
          <n-button size="small" @click="refreshIframe">刷新</n-button>
          <n-button size="small" @click="closeIframe">关闭</n-button>
        </n-space>
        <n-tag type="info" size="small">{{ activeLink }}</n-tag>
      </n-flex>
    </n-card>

    <!-- iframe 内容区 -->
    <div v-if="showIframe" class="iframe-container">
      <iframe
        id="rotation-iframe"
        :src="iframeUrl"
        frameborder="0"
        width="100%"
        height="100%"
        allowfullscreen
      ></iframe>
    </div>

    <!-- 无内容提示 -->
    <n-card v-else style="text-align: center; padding: 40px;">
      <p>点击上方按钮查看板块轮动</p>
      <p style="color: #999; font-size: 12px;">带"外链"标签的链接将在新窗口打开</p>
    </n-card>
  </div>
</template>

<style scoped>
.rotation-container {
  padding: 0;
  height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
}

.iframe-container {
  flex: 1;
  min-height: 500px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  overflow: hidden;
  background-color: #000;
}

.iframe-container iframe {
  width: 100%;
  height: 100%;
  min-height: 500px;
}
</style>
