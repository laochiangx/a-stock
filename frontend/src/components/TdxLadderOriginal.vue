<script setup>
import { ref } from 'vue'
import { NCard, NSpace, NButton, NFlex } from 'naive-ui'

const iframeUrl = ref('http://hot.icfqs.com:7615/site/tdx-pc-hqpage/page-lbtt.html?color=0&bkcolor=141212')

const refreshIframe = () => {
  const iframe = document.getElementById('tdx-ladder-iframe-orig')
  if (iframe) {
    iframe.src = iframe.src
  }
}

const openInNewWindow = () => {
  window.open(iframeUrl.value, '_blank')
}
</script>

<template>
  <div class="tdx-ladder-container">
    <n-card size="small" class="header-card">
      <n-flex justify="space-between" align="center">
        <n-space>
          <span style="color: #FF8C00; font-weight: bold;">连板天梯（原）</span>
          <n-button size="small" @click="refreshIframe">刷新</n-button>
          <n-button size="small" @click="openInNewWindow">新窗口打开</n-button>
        </n-space>
      </n-flex>
    </n-card>

    <div class="iframe-container">
      <iframe
        id="tdx-ladder-iframe-orig"
        :src="iframeUrl"
        frameborder="0"
        width="100%"
        height="100%"
        allowfullscreen
      ></iframe>
    </div>
  </div>
</template>

<style scoped>
.tdx-ladder-container {
  padding: 0;
  background-color: #141212;
  height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
}

.header-card {
  margin-bottom: 4px;
  background-color: #1a1a1a !important;
  flex-shrink: 0;
}

:deep(.n-card) {
  background-color: #1a1a1a;
}

.iframe-container {
  flex: 1;
  border: 1px solid #333;
  border-radius: 4px;
  overflow: hidden;
  background-color: #141212;
}

.iframe-container iframe {
  width: 100%;
  height: 100%;
  border: none;
  background-color: #141212;
}
</style>
