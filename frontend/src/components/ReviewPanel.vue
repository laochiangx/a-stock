<script setup>
import { ref, onMounted, computed } from 'vue'
import { NCard, NSpace, NButton, NScrollbar, NTag, NFlex } from 'naive-ui'

// 复盘链接配置
const reviewLinks = ref([
  { name: '直播', url: 'https://www.cls.cn/telegraph', canEmbed: false },
  { name: '解读', url: 'https://www.cls.cn/depth', canEmbed: false },
  { name: '股宝', url: 'https://xuangubao.cn/', canEmbed: false },
  { name: '竞价', url: 'https://xuangubao.cn/jingjiafenxi', canEmbed: false },
  { name: '精选', url: 'https://xuangubao.cn/', canEmbed: false },
  { name: '关注', url: 'https://xuangubao.cn/', canEmbed: false },
  { name: '热解', url: 'http://hot.icfqs.com:7615/site/tdx-pc-pcwebcall/page-qxzb.html?color=0&bkcolor=000000', canEmbed: true },
  { name: '涨池', url: 'https://xuangubao.cn/zhangting', canEmbed: false },
  { name: '强势', url: 'https://xuangubao.cn/', canEmbed: false },
  { name: '机构', url: 'https://data.eastmoney.com/stock/lhb/jgmmtj.html', canEmbed: false },
  { name: '风标', url: 'http://hot.icfqs.com:7615/site/tdx-pc-find/page_fxb.html', canEmbed: true },
  { name: '看盘', url: 'https://xuangubao.cn/', canEmbed: false },
  { name: '概念', url: 'https://xuangubao.cn/sector', canEmbed: false },
  { name: '次新', url: 'https://xuangubao.cn/', canEmbed: false },
  { name: '热搜', url: 'https://www.jiuyangongshe.com/', canEmbed: false },
  { name: '情绪', url: 'http://hot.icfqs.com:7615/site/tdx-pc-pcwebcall/page-qxzb.html?color=0&bkcolor=000000', canEmbed: true },
  { name: '大盘', url: 'https://share.gw.com.cn/diagnosis/', canEmbed: true },
  { name: '盘面', url: 'http://hot.icfqs.com:7615/site/kggx/tk_mrfp.html?color=0&bkcolor=000000', canEmbed: true },
  { name: '神器', url: 'http://pro.gw.com.cn/newfpsq/index.html', canEmbed: true },
  { name: '龙虎', url: 'http://vis-free.10jqka.com.cn/billboard/indexV3.html#/index', canEmbed: false },
  { name: '排行', url: 'http://hot.icfqs.com:7615/site/tdx-pc-find/page_lhbphb.html', canEmbed: true },
  { name: '游资', url: 'http://hot.icfqs.com:7615/site/tdx-pc-find/page_yzfp.html', canEmbed: true },
  { name: '动向', url: 'https://xuangubao.cn/', canEmbed: false },
  { name: '打板', url: 'https://data.10jqka.com.cn/datacenterph/limitup/limtupInfo.html#/limtupStrength', canEmbed: false },
  { name: '热门', url: 'https://www.aigupiao.com/pcv/quote/list/type/hot', canEmbed: false },
  { name: '热榜', url: 'https://eq.10jqka.com.cn/webpage/ths-hot-list/index.html?showStatusBar=true#/', canEmbed: false },
  { name: '热点', url: 'http://ddx.gubit.cn/index.php', canEmbed: true },
  { name: '行情', url: 'https://xuangubao.cn/dingpan', canEmbed: false },
  { name: '掘金', url: 'https://data.10jqka.com.cn/datacenterph/bkjj/bkjjInfo.html', canEmbed: false },
  { name: '查股', url: 'http://ddx.gubit.cn/index.php', canEmbed: true },
  { name: '前瞻', url: 'http://pcengine.gw.com.cn/wlds/index.html', canEmbed: true },
  { name: '图据', url: 'http://plugin-ft.10jqka.com.cn/plugingwapi/bff/plugin_news_bff/imgdata/index#/dataImage', canEmbed: false },
  { name: '龙回', url: 'http://www.tetegu.com/longhuitou/', canEmbed: true },
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
    // 无法嵌入的链接，在新窗口打开
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
  const iframe = document.getElementById('review-iframe')
  if (iframe) {
    iframe.src = iframe.src
  }
}

onMounted(() => {
  // 默认加载第一个可嵌入的链接
  const firstEmbeddable = reviewLinks.value.find(l => l.canEmbed)
  if (firstEmbeddable) {
    handleLinkClick(firstEmbeddable)
  }
})
</script>

<template>
  <div class="review-panel-container">
    <!-- 顶部链接栏 -->
    <n-card size="small" style="margin-bottom: 8px;">
      <n-scrollbar x-scrollable>
        <n-space :wrap="false">
          <n-button
            v-for="link in reviewLinks"
            :key="link.name"
            :type="activeLink === link.name ? 'primary' : 'default'"
            size="small"
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
        id="review-iframe"
        :src="iframeUrl"
        frameborder="0"
        width="100%"
        height="100%"
        allowfullscreen
      ></iframe>
    </div>

    <!-- 无内容提示 -->
    <n-card v-else style="text-align: center; padding: 40px;">
      <p>点击上方按钮查看复盘内容</p>
      <p style="color: #999; font-size: 12px;">带"外链"标签的链接将在新窗口打开</p>
    </n-card>
  </div>
</template>

<style scoped>
.review-panel-container {
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
}

.iframe-container iframe {
  width: 100%;
  height: 100%;
  min-height: 500px;
}
</style>
