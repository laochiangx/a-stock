<script setup>
import { onBeforeMount, onUnmounted, ref } from 'vue'
import { GetHotTopic } from "../api/api";
import { NList, NListItem, NThing, NAvatar, NFlex, NButton, NTag, NNumberAnimation, NEmpty } from "naive-ui";

const list = ref([])
const task = ref()

onBeforeMount(async () => {
  await fetchHotTopic()
  task.value = setInterval(async () => {
    await fetchHotTopic()
  }, 1000 * 30) // 每30秒刷新一次
})

onUnmounted(() => {
  if (task.value) {
    clearInterval(task.value)
  }
})

async function fetchHotTopic() {
  try {
    const result = await GetHotTopic(10)
    list.value = result || []
  } catch (err) {
    console.error('获取热门话题失败:', err)
    list.value = []
  }
}

function openCenteredWindow(url, width, height) {
  const left = (window.screen.width - width) / 2;
  const top = (window.screen.height - height) / 2;
  window.open(url, 'centeredWindow', `width=${width},height=${height},left=${left},top=${top}`)
}

function showPage(htid) {
  openCenteredWindow(`https://gubatopic.eastmoney.com/topic_v3.html?htid=${htid}`, 1000, 600)
}
</script>

<template>
  <n-list bordered hoverable clickable v-if="list.length > 0">
    <n-list-item v-for="(item, index) in list" :key="index">
      <n-thing :title="item.nickname" :description="item.desc" :description-style="'font-size: 14px;'"
        @click="showPage(item.htid)">
        <template v-if="item.squareImg" #avatar>
          <n-avatar :src="item.squareImg" :size="60">
          </n-avatar>
        </template>
        <template v-if="item.stock_list && item.stock_list.length > 0" #footer>
          <n-flex>
            <n-tag type="info" v-for="(v, i) in item.stock_list" :key="i" :bordered="false" size="small">
              {{ v.name }}
            </n-tag>
          </n-flex>
        </template>
        <template v-if="item.clickNumber" #header-extra>
          <n-flex>
            <n-button secondary type="warning" size="tiny">讨论数：<n-number-animation show-separator :from="0"
                :to="item.postNumber || 0" />
            </n-button>
            <n-tag :bordered="false" type="warning" size="small">浏览量：<n-number-animation show-separator :from="0"
                :to="item.clickNumber || 0" />
            </n-tag>
          </n-flex>
        </template>
      </n-thing>
    </n-list-item>
  </n-list>
  <n-empty v-else description="暂无热门话题数据" />
</template>

<style scoped></style>
