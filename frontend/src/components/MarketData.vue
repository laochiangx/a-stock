<script setup>
import { h, onMounted, ref } from 'vue'
import { NCard, NDataTable, NButton, NSpace, NTag, NText, NInputNumber, useMessage, NSpin } from 'naive-ui'
import { SyncMarketDailyStats, GetMarketDailyStats } from '../api/api'

const message = useMessage()
const loading = ref(false)
const syncing = ref(false)
const days = ref(20)
const dataList = ref([])

const columns = [
  {
    title: '日期',
    key: 'dataDate',
    width: 110,
  },
  {
    title: '阶段',
    key: 'stageName',
    width: 120,
    render: (row) => {
      const name = row.stageName || ''
      const code = row.stageCode || ''
      const type = code.startsWith('C') ? 'error' : code.startsWith('B') ? 'warning' : code.startsWith('A') ? 'success' : 'default'
      return h(NTag, { type, size: 'small' }, { default: () => name || code || '-' })
    }
  },
  {
    title: '市场评分',
    key: 'marketScore',
    width: 110,
    render: (row) => {
      const score = row.marketScore ?? ''
      const grade = row.scoreGrade ?? ''
      return h(NText, {}, { default: () => `${score}${grade ? ` (${grade})` : ''}` })
    }
  },
  { title: '上涨', key: 'risingCount', width: 80 },
  { title: '下跌', key: 'fallingCount', width: 80 },
  { title: '涨停', key: 'totalZt', width: 70 },
  { title: '跌停', key: 'totalDt', width: 70 },
  { title: '连板', key: 'lbCount', width: 70 },
  { title: '封板率', key: 'fbRatio', width: 80 },
  { title: '开板', key: 'openCount', width: 70 },
  { title: '一进二%', key: 'oneToTwoRatio', width: 80 },
  { title: '二进三%', key: 'twoToThreeRatio', width: 80 },
  { title: '三进四%', key: 'threeToFourRatio', width: 80 },
  { title: '连板率%', key: 'lbRatio', width: 80 },
  { title: '涨停额(亿)', key: 'ztAmount', width: 100 },
  { title: '成交额(亿)', key: 'totalAmount', width: 100 },
]

const fetchList = async () => {
  loading.value = true
  try {
    const res = await GetMarketDailyStats(null, days.value)
    if (res && res.success) {
      dataList.value = res.data || []
    } else {
      message.error(res?.message || '加载失败')
    }
  } catch (e) {
    message.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const syncToday = async (force) => {
  syncing.value = true
  try {
    const res = await SyncMarketDailyStats(null, !!force)
    if (res && res.success) {
      message.success(res.message === 'exists' ? '今日已存在，未触发爬取' : '同步成功')
      await fetchList()
    } else {
      message.error(res?.message || '同步失败')
    }
  } catch (e) {
    message.error(e.message || '同步失败')
  } finally {
    syncing.value = false
  }
}

onMounted(() => {
  fetchList()
})
</script>

<template>
  <div style="padding: 16px;">
    <n-card title="市场数据" size="small">
      <template #header-extra>
        <n-space>
          <n-input-number v-model:value="days" :min="5" :max="60" size="small" style="width: 120px;" />
          <n-button size="small" :loading="loading" @click="fetchList">刷新</n-button>
          <n-button size="small" type="primary" :loading="syncing" @click="syncToday(false)">同步今日</n-button>
          <n-button size="small" type="warning" :loading="syncing" @click="syncToday(true)">强制同步</n-button>
        </n-space>
      </template>

      <n-spin :show="loading">
        <n-data-table
          :columns="columns"
          :data="dataList"
          :max-height="'calc(100vh - 200px)'"
          size="small"
          :scroll-x="1600"
        />
      </n-spin>
    </n-card>
  </div>
</template>
