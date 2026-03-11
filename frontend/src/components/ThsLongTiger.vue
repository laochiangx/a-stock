<script setup>
import { ref } from 'vue'
import { NCard, NButton, NSpace, NTag } from 'naive-ui'

const iframeUrl = ref('https://vis-free.10jqka.com.cn/billboard/indexV3.html#/index')

// 在新窗口打开
const openInNewTab = () => {
  window.open(iframeUrl.value, '_blank')
}

// 刷新 iframe
const refreshIframe = () => {
  const iframe = document.getElementById('ths-longtiger-iframe')
  if (iframe) {
    iframe.src = iframe.src
  }
}
</script>

<template>
  <div class="ths-longtiger-container">
    <!-- 顶部操作栏 -->
    <n-card size="small" class="header-card">
      <n-space justify="space-between" align="center">
        <n-space>
          <n-tag type="error" size="large">同花顺龙虎榜</n-tag>
        </n-space>
        <n-space>
          <n-button size="small" type="success" @click="refreshIframe">刷新</n-button>
          <n-button size="small" type="info" @click="openInNewTab">新窗口打开</n-button>
        </n-space>
      </n-space>
    </n-card>

    <!-- 嵌入的 iframe -->
    <div class="iframe-wrapper">
      <iframe 
        id="ths-longtiger-iframe"
        :src="iframeUrl" 
        frameborder="0"
        class="longtiger-iframe"
        allow="fullscreen"
      ></iframe>
    </div>
  </div>
</template>

<style scoped>
.ths-longtiger-container {
  padding: 0;
  background-color: #0d1117;
  height: calc(100vh - 80px);
  display: flex;
  flex-direction: column;
}

.header-card {
  margin-bottom: 8px;
  background-color: #161b22 !important;
  flex-shrink: 0;
}

:deep(.n-card) {
  background-color: #161b22;
}

.iframe-wrapper {
  flex: 1;
  overflow: hidden;
}

.longtiger-iframe {
  width: 100%;
  height: 100%;
  border: none;
  background-color: #0d1117;
}
</style>
