<script setup>
import { computed, onMounted, ref } from 'vue'
import { NCard, NSpace, NButton, NFlex, NDatePicker, NInputNumber, NSpin, NTag, NText, NModal } from 'naive-ui'
import { GetTdxLbtt, SyncTdxLbtt } from '../api/api'
import KLineChart from './KLineChart.vue'

const loading = ref(false)
const syncing = ref(false)

const days = ref(10)
const endDate = ref(Date.now())

const dayList = ref([])
const itemList = ref([])

const kModalShow = ref(false)
const kModalCode = ref('')
const kModalName = ref('')

const formatDate = (timestamp) => {
  const d = new Date(timestamp)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${dd}`
}

const formatAmountYi = (v) => {
  const n = Number(v)
  if (!Number.isFinite(n) || n === 0) return ''
  return (n / 1e8).toFixed(n >= 1e9 ? 0 : 1)
}

const byDate = computed(() => {
  const map = new Map()
  for (const d of (dayList.value || [])) {
    if (d && d.dataDate) map.set(d.dataDate, d)
  }
  return map
})

const dates = computed(() => {
  const arr = (dayList.value || []).map(d => d?.dataDate).filter(Boolean)
  arr.sort((a, b) => String(b).localeCompare(String(a)))
  return arr
})

const normalizeLevel = (lv) => {
  const n = Number(lv)
  if (!Number.isFinite(n)) return n
  return n >= 8 ? 8 : n
}

const levels = computed(() => {
  const set = new Set()
  for (const it of (itemList.value || [])) {
    if (it && it.level !== null && it.level !== undefined) {
      set.add(normalizeLevel(it.level))
    }
  }
  const arr = Array.from(set)
  arr.sort((a, b) => b - a)
  return arr
})

const cellMap = computed(() => {
  const map = new Map()
  for (const it of (itemList.value || [])) {
    const d = it?.dataDate
    const lv = it?.level
    if (!d || lv === null || lv === undefined) continue
    const key = `${d}__${normalizeLevel(lv)}`
    const list = map.get(key) || []
    list.push(it)
    map.set(key, list)
  }
  for (const [k, list] of map.entries()) {
    list.sort((a, b) => String(a?.stockCode || '').localeCompare(String(b?.stockCode || '')))
    map.set(k, list)
  }
  return map
})

const promoteRateByCell = (d, lv) => {
  const key = `${d}__${lv}`
  const list = cellMap.value.get(key) || []
  for (const it of list) {
    const r = it?.promoteRate
    if (r !== null && r !== undefined && r !== '') {
      const n = Number(r)
      if (Number.isFinite(n)) return n
    }
  }
  return null
}

const toKLineCode = (it) => {
  const code = String(it?.stockCode || '').trim()
  const m = String(it?.market || '').trim()
  if (!code) return ''
  if (code.startsWith('sh') || code.startsWith('sz') || code.startsWith('bj') || code.startsWith('hk') || code.startsWith('gb_')) return code
  if (m === '1') return `sh${code}`
  if (m === '0') return `sz${code}`
  if (m === '2') return `bj${code}`
  return code
}

const openKLine = (it) => {
  kModalCode.value = toKLineCode(it)
  kModalName.value = String(it?.stockName || '').trim()
  if (!kModalCode.value) return
  kModalShow.value = true
}

const fetchData = async () => {
  loading.value = true
  try {
    const endStr = formatDate(endDate.value)
    const res = await GetTdxLbtt(null, endStr, days.value)
    if (res && res.success) {
      dayList.value = res.days || []
      itemList.value = res.items || []
    } else {
      dayList.value = []
      itemList.value = []
    }
  } finally {
    loading.value = false
  }
}

const syncRange = async (force) => {
  syncing.value = true
  try {
    const endStr = formatDate(endDate.value)
    const startTs = new Date(endDate.value)
    startTs.setDate(startTs.getDate() - Math.max(1, Number(days.value || 1)) + 1)
    const startStr = formatDate(startTs.getTime())
    const res = await SyncTdxLbtt(startStr, endStr, !!force)
    if (res && res.success) {
      await fetchData()
    }
  } finally {
    syncing.value = false
  }
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="tdx-ladder-container">
    <!-- 顶部操作栏 -->
    <n-card size="small" class="header-card">
      <n-flex justify="space-between" align="center">
        <n-space>
          <span style="color: #FF8C00; font-weight: bold;">连板天梯</span>
          <n-date-picker v-model:value="endDate" type="date" size="small" />
          <n-input-number v-model:value="days" :min="3" :max="60" size="small" style="width: 120px;" />
          <n-button size="small" :loading="loading" @click="fetchData">刷新</n-button>
          <n-button size="small" type="primary" :loading="syncing" @click="syncRange(false)">同步</n-button>
          <n-button size="small" type="warning" :loading="syncing" @click="syncRange(true)">强制同步</n-button>
        </n-space>
      </n-flex>
    </n-card>

    <n-spin :show="loading || syncing" class="grid-wrap">
      <div class="grid-scroll">
        <table class="ladder-table">
          <thead>
            <tr>
              <th class="sticky-col level-col">板位</th>
              <th v-for="d in dates" :key="d" class="date-col">
                <div class="date-header">
                  <div class="date-title">{{ d }}</div>
                  <div class="date-stats">
                    <n-tag size="small" :bordered="false" type="error">涨 {{ byDate.get(d)?.risingCount ?? '-' }}</n-tag>
                    <n-tag size="small" :bordered="false" type="success">跌 {{ byDate.get(d)?.fallingCount ?? '-' }}</n-tag>
                    <n-tag size="small" :bordered="false" type="warning">涨停 {{ byDate.get(d)?.ztCount ?? '-' }}</n-tag>
                    <n-tag size="small" :bordered="false" type="default">跌停 {{ byDate.get(d)?.dtCount ?? '-' }}</n-tag>
                    <n-text depth="3" style="margin-left: 6px;">
                      成交额(亿) {{ formatAmountYi(byDate.get(d)?.totalAmount) || '-' }}
                    </n-text>
                  </div>
                  <div class="date-hot">
                    <n-text depth="3">
                      {{ byDate.get(d)?.hot1Name ? `${byDate.get(d)?.hot1Name}(${byDate.get(d)?.hot1Count ?? '-'})` : '' }}
                      {{ byDate.get(d)?.hot2Name ? ` | ${byDate.get(d)?.hot2Name}(${byDate.get(d)?.hot2Count ?? '-'})` : '' }}
                      {{ byDate.get(d)?.hot3Name ? ` | ${byDate.get(d)?.hot3Name}(${byDate.get(d)?.hot3Count ?? '-'})` : '' }}
                    </n-text>
                  </div>
                </div>
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="lv in levels" :key="lv">
              <td class="sticky-col level-col">
                <div class="level-title">{{ lv === 8 ? '8板+' : `${lv}板` }}</div>
              </td>
              <td v-for="d in dates" :key="`${d}_${lv}`" class="cell">
                <div class="cell-head" v-if="promoteRateByCell(d, lv) !== null">
                  <n-tag size="small" type="info" :bordered="false">
                    晋级率 {{ (promoteRateByCell(d, lv) * 100).toFixed(0) }}%
                  </n-tag>
                </div>
                <div class="cell-list">
                  <div
                    v-for="it in (cellMap.get(`${d}__${lv}`) || [])"
                    :key="`${it.stockCode}_${it.level}_${d}`"
                    class="stock-item"
                    @click="openKLine(it)"
                  >
                    <div class="stock-line">
                      <span class="stock-name">{{ it.stockName || '-' }}</span>
                      <span class="stock-code">{{ it.stockCode }}</span>
                      <n-tag v-if="it.ztTime" size="tiny" :bordered="false" type="warning">{{ it.ztTime }}</n-tag>
                      <n-tag v-if="it.openTimes && it.openTimes > 0" size="tiny" :bordered="false" type="error">开{{ it.openTimes }}</n-tag>
                      <n-tag v-if="it.sealAmount" size="tiny" :bordered="false" type="info">封{{ formatAmountYi(it.sealAmount) }}</n-tag>
                    </div>
                    <div class="stock-meta" v-if="it.reason || it.industry || it.reason2">
                      <span class="stock-reason">{{ it.reason || '' }}</span>
                      <span v-if="it.reason2" class="stock-reason2"> / {{ it.reason2 }}</span>
                      <span v-if="it.industry" class="stock-industry"> · {{ it.industry }}</span>
                    </div>
                  </div>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </n-spin>

    <n-modal v-model:show="kModalShow" preset="card" style="width: 980px;" :mask-closable="true">
      <k-line-chart
        :key="kModalCode"
        :code="kModalCode"
        :name="kModalName"
        :darkTheme="true"
        :kDays="120"
        :chartHeight="600"
      />
    </n-modal>
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

.grid-wrap {
  flex: 1;
  padding: 0 4px 4px 4px;
}

.grid-scroll {
  height: 100%;
  overflow: auto;
  border: 1px solid #333;
  border-radius: 4px;
  background-color: #141212;
}

.ladder-table {
  width: max-content;
  min-width: 100%;
  border-collapse: collapse;
  color: #ddd;
}

.ladder-table th,
.ladder-table td {
  border: 1px solid #2a2a2a;
  vertical-align: top;
  padding: 6px;
}

.sticky-col {
  position: sticky;
  left: 0;
  z-index: 2;
  background-color: #101010;
}

.level-col {
  width: 90px;
  min-width: 90px;
  text-align: center;
}

.date-col {
  min-width: 360px;
  background-color: #101010;
}

.date-header {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.date-title {
  font-weight: 700;
  color: #FF8C00;
}

.date-stats {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.date-hot {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.level-title {
  font-weight: 700;
  color: #FF8C00;
}

.cell {
  min-width: 360px;
  background-color: #141212;
}

.cell-head {
  margin-bottom: 6px;
}

.cell-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.stock-item {
  padding: 6px;
  border: 1px solid #2a2a2a;
  border-radius: 4px;
  background-color: #0f0f0f;
  cursor: pointer;
}

.stock-line {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.stock-name {
  font-weight: 700;
  color: #fff;
}

.stock-code {
  color: #999;
  font-size: 12px;
}

.stock-meta {
  margin-top: 4px;
  color: #bbb;
  font-size: 12px;
  line-height: 1.4;
}

.stock-industry {
  color: #888;
}
</style>
