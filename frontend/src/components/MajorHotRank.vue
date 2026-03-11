<script setup>
import { ref, onMounted, watch } from 'vue'
import {
  NCard,
  NButton,
  NSpace,
  NDatePicker,
  NSpin,
  NEmpty,
  NTable,
  NTag,
  NPopover,
  NText,
  NFlex,
  NTabs,
  NTabPane,
  useMessage,
  NModal
} from 'naive-ui'
import KLineChart from './KLineChart.vue'
import {
  SyncThsHotListDay,
  GetThsHotListDayList,
  SyncDcHotListDay,
  GetDcHotListDayList,
  SyncClsHotListDay,
  GetClsHotListDayList
} from '../api/api'

const message = useMessage()

const loading = ref(false)
const syncing = ref(false)
const selectedDate = ref(Date.now())

const activeTab = ref('ths')

const thsRows = ref([])
const dcRows = ref([])
const clsRows = ref([])

const kModalShow = ref(false)
const kModalCode = ref('')
const kModalName = ref('')

const normalizeMarketCode = (market, code) => {
  const c = String(code || '').trim()
  if (!c) return ''
  if (c.startsWith('sh') || c.startsWith('sz') || c.startsWith('bj') || c.startsWith('hk') || c.startsWith('gb_')) return c
  const m = market === null || market === undefined ? '' : String(market)
  if (m === '1') return `sh${c}`
  if (m === '0') return `sz${c}`
  if (m === '2') return `bj${c}`
  if (/^6\d{5}$/.test(c)) return `sh${c}`
  if (/^[03]\d{5}$/.test(c)) return `sz${c}`
  if (/^[48]\d{5}$/.test(c)) return `bj${c}`
  return c
}

const openKLine = (row) => {
  kModalCode.value = normalizeMarketCode(row?.market, row?.stockCode)
  kModalName.value = String(row?.stockName || '').trim()
  if (!kModalCode.value) return
  kModalShow.value = true
}

const formatDate = (timestamp) => {
  const date = new Date(timestamp)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const safeParseConceptTags = (v) => {
  if (!v) return []
  if (Array.isArray(v)) return v
  try {
    const arr = JSON.parse(String(v))
    return Array.isArray(arr) ? arr : []
  } catch (e) {
    return []
  }
}

const safeParseDcRaw = (v) => {
  if (!v) return null
  if (typeof v === 'object') return v
  try {
    return JSON.parse(String(v))
  } catch (e) {
    return null
  }
}

const safeParseClsRaw = (v) => {
  return safeParseDcRaw(v)
}

const safeParseClsTags = (v) => {
  const raw = safeParseClsRaw(v)
  const tags = raw?.tags
  return Array.isArray(tags) ? tags : []
}

const safeParseDcThemePlates = (v) => {
  const raw = safeParseDcRaw(v)
  const tags = raw?.tags
  const arr = tags?.themePlates
  return Array.isArray(arr) ? arr : []
}

const safeParseDcUiLabels = (v) => {
  const raw = safeParseDcRaw(v)
  const tags = raw?.tags
  const arr = tags?.uiLabels
  return Array.isArray(arr) ? arr : []
}

const safeParseDcTagSystem = (v) => {
  const raw = safeParseDcRaw(v)
  const tags = raw?.tags
  const ts = tags?.tagSystem
  return ts && typeof ts === 'object' ? ts : null
}

const safeParseDcHotAnalysis = (v) => {
  const raw = safeParseDcRaw(v)
  const tags = raw?.tags
  const arr = tags?.hotAnalysis
  return Array.isArray(arr) ? arr : []
}

const syncCurrent = async (force) => {
  if (activeTab.value === 'dc') {
    return syncDc(force)
  }
  if (activeTab.value === 'cls') {
    return syncCls(force)
  }
  return syncThs(force)
}

const refreshCurrent = async () => {
  if (activeTab.value === 'dc') {
    return fetchDc()
  }
  if (activeTab.value === 'cls') {
    return fetchCls(true)
  }
  return fetchThs()
}

const syncCls = async (force) => {
  syncing.value = true
  try {
    const dateStr = formatDate(selectedDate.value)
    const res = await SyncClsHotListDay(dateStr, !!force, 'stock')
    if (!res || res.success === false) {
      message.error(res?.message || '同步财联社热榜失败')
      return
    }
    message.success(`同步成功：${res.count ?? 0}条`)
    await fetchCls(false)
  } finally {
    syncing.value = false
  }
}

const fetchCls = async (autoSyncIfEmpty) => {
  loading.value = true
  try {
    const dateStr = formatDate(selectedDate.value)
    const res = await GetClsHotListDayList(dateStr, 'stock')
    if (!res || res.success === false) {
      message.error(res?.message || '获取财联社热榜失败')
      clsRows.value = []
      return
    }
    const list = res?.data ?? res?.list ?? []
    clsRows.value = Array.isArray(list) ? list : []

    if (autoSyncIfEmpty && (!clsRows.value || clsRows.value.length === 0)) {
      await syncCls(false)
    }
  } finally {
    loading.value = false
  }
}

const syncDc = async (force) => {
  syncing.value = true
  try {
    const dateStr = formatDate(selectedDate.value)
    const res = await SyncDcHotListDay(dateStr, !!force, 'stock')
    if (!res || res.success === false) {
      message.error(res?.message || '同步东方财富热榜失败')
      return
    }
    message.success(`同步成功：${res.count ?? 0}条`)
    await fetchDc()
  } finally {
    syncing.value = false
  }
}

const fetchDc = async () => {
  loading.value = true
  try {
    const dateStr = formatDate(selectedDate.value)
    const res = await GetDcHotListDayList(dateStr, 'stock')
    if (!res || res.success === false) {
      message.error(res?.message || '获取东方财富热榜失败')
      dcRows.value = []
      return
    }
    const list = res?.data ?? res?.list ?? []
    dcRows.value = Array.isArray(list) ? list : []
  } finally {
    loading.value = false
  }
}

const fetchThs = async () => {
  loading.value = true
  try {
    const dateStr = formatDate(selectedDate.value)
    const res = await GetThsHotListDayList(dateStr, 'a', 'normal')
    if (!res || res.success === false) {
      message.error(res?.message || '获取同花顺热榜失败')
      thsRows.value = []
      return
    }
    const list = res?.data ?? res?.list ?? []
    thsRows.value = Array.isArray(list) ? list : []
  } finally {
    loading.value = false
  }
}

const syncThs = async (force) => {
  syncing.value = true
  try {
    const dateStr = formatDate(selectedDate.value)
    const res = await SyncThsHotListDay(dateStr, !!force, 'a', 'normal')
    if (!res || res.success === false) {
      message.error(res?.message || '同步同花顺热榜失败')
      return
    }
    message.success(`同步成功：${res.count ?? 0}条`)
    await fetchThs()
  } finally {
    syncing.value = false
  }
}

onMounted(async () => {
  await fetchThs()
  await fetchDc()
  if (activeTab.value === 'cls') {
    await fetchCls(true)
  }
})

watch(
  () => activeTab.value,
  async (v) => {
    if (v === 'cls') {
      await fetchCls(true)
      return
    }
    if (v === 'dc') {
      await fetchDc()
      return
    }
    await fetchThs()
  }
)
</script>

<template>
  <div style="padding: 12px;">
    <n-card size="small">
      <n-flex justify="space-between" align="center" :wrap="true">
        <n-space align="center">
          <n-date-picker v-model:value="selectedDate" type="date" @update:value="() => { refreshCurrent(); }" />
          <n-button size="small" type="primary" :loading="syncing" @click="syncCurrent(true)">获取数据</n-button>
          <n-button size="small" :loading="loading" @click="refreshCurrent">刷新</n-button>
        </n-space>
        <n-text depth="3">
          按日存储：{{ activeTab === 'dc' ? '东方财富热榜（day）' : (activeTab === 'cls' ? '财联社热榜（day）' : '同花顺24小时热榜（day）') }}
        </n-text>
      </n-flex>
    </n-card>

    <n-tabs v-model:value="activeTab" type="line" animated style="margin-top: 12px;">
      <n-tab-pane name="ths" tab="同花顺(24h)">
        <n-spin :show="loading || syncing">
          <div v-if="!thsRows || thsRows.length === 0" style="margin-top: 12px;">
            <n-empty description="暂无数据，点击获取数据抓取并入库" />
          </div>

          <div v-else style="margin-top: 12px;">
            <n-table :single-line="false" size="small">
              <thead>
                <tr>
                  <th style="width: 70px;">排名</th>
                  <th style="width: 90px;">代码</th>
                  <th style="width: 140px;">名称</th>
                  <th style="width: 90px;">涨跌幅</th>
                  <th style="width: 90px;">热度</th>
                  <th style="width: 80px;">变动</th>
                  <th style="width: 260px;">标题</th>
                  <th style="width: 220px;">标签</th>
                  <th>分析</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in thsRows" :key="row.stockCode">
                  <td>{{ row.orderNum }}</td>
                  <td>
                    <n-button text type="info" @click="openKLine(row)">{{ row.stockCode }}</n-button>
                  </td>
                  <td>
                    <n-button text type="info" @click="openKLine(row)">{{ row.stockName }}</n-button>
                  </td>
                  <td>
                    <n-tag :type="(row.riseAndFall || 0) >= 0 ? 'error' : 'success'" size="small">
                      {{ row.riseAndFall }}
                    </n-tag>
                  </td>
                  <td>{{ row.rate }}</td>
                  <td>{{ row.hotRankChg }}</td>
                  <td>{{ row.analyseTitle }}</td>
                  <td>
                    <n-space size="small" :wrap="true">
                      <n-tag v-for="t in safeParseConceptTags(row.conceptTags)" :key="t" size="small" type="info">
                        {{ t }}
                      </n-tag>
                      <n-tag v-if="row.popularityTag" size="small" type="warning">{{ row.popularityTag }}</n-tag>
                    </n-space>
                  </td>
                  <td style="white-space: pre-wrap;">{{ row.analyse }}</td>
                </tr>
              </tbody>
            </n-table>
          </div>
        </n-spin>
      </n-tab-pane>

      <n-tab-pane name="cls" tab="财联社">
        <n-spin :show="loading || syncing">
          <div v-if="!clsRows || clsRows.length === 0" style="margin-top: 12px;">
            <n-empty description="暂无数据，点击获取数据抓取并入库" />
          </div>

          <div v-else style="margin-top: 12px;">
            <n-table :single-line="false" size="small">
              <thead>
                <tr>
                  <th style="width: 70px;">排名</th>
                  <th style="width: 110px;">代码</th>
                  <th style="width: 160px;">名称</th>
                  <th style="width: 90px;">涨跌幅</th>
                  <th style="width: 320px;">标签</th>
                  <th style="width: 90px;">详情</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in clsRows" :key="row.stockCode">
                  <td>{{ row.orderNum }}</td>
                  <td>
                    <n-button text type="info" @click="openKLine(row)">{{ row.stockCode }}</n-button>
                  </td>
                  <td>
                    <n-button text type="info" @click="openKLine(row)">{{ row.stockName }}</n-button>
                  </td>
                  <td>
                    <n-tag :type="(row.riseAndFall || 0) >= 0 ? 'error' : 'success'" size="small">
                      {{ row.riseAndFall }}
                    </n-tag>
                  </td>
                  <td>
                    <n-space size="small" :wrap="true">
                      <n-tag v-for="t in safeParseClsTags(row.rawJson)" :key="t" size="small" type="info">
                        {{ t }}
                      </n-tag>
                    </n-space>
                  </td>
                  <td>
                    <n-popover trigger="hover" placement="left" style="max-width: 520px;">
                      <template #trigger>
                        <n-button size="tiny" quaternary>raw</n-button>
                      </template>
                      <pre style="white-space: pre-wrap; margin: 0;">{{ row.rawJson }}</pre>
                    </n-popover>
                  </td>
                </tr>
              </tbody>
            </n-table>
          </div>
        </n-spin>
      </n-tab-pane>

      <n-tab-pane name="dc" tab="东方财富">
        <n-spin :show="loading || syncing">
          <div v-if="!dcRows || dcRows.length === 0" style="margin-top: 12px;">
            <n-empty description="暂无数据，点击获取数据抓取并入库" />
          </div>

          <div v-else style="margin-top: 12px;">
            <n-table :single-line="false" size="small">
              <thead>
                <tr>
                  <th style="width: 70px;">排名</th>
                  <th style="width: 90px;">代码</th>
                  <th style="width: 160px;">名称</th>
                  <th style="width: 220px;">标签</th>
                  <th style="width: 90px;">最新价</th>
                  <th style="width: 90px;">涨跌幅</th>
                  <th style="width: 90px;">涨跌额</th>
                  <th style="width: 120px;">成交量</th>
                  <th style="width: 140px;">成交额</th>
                  <th style="width: 90px;">换手</th>
                  <th style="width: 140px;">总市值</th>
                  <th style="width: 140px;">流通市值</th>
                  <th style="width: 90px;">热度(rc)</th>
                  <th style="width: 90px;">历史热度</th>
                  <th style="width: 90px;">详情</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in dcRows" :key="row.stockCode">
                  <template v-if="true">
                    <template v-for="_ in [safeParseDcRaw(row.rawJson)]">
                      <td>{{ row.orderNum }}</td>
                      <td>
                        <n-button text type="info" @click="openKLine(row)">{{ row.stockCode }}</n-button>
                      </td>
                      <td>
                        <n-button text type="info" @click="openKLine(row)">{{ row.stockName }}</n-button>
                      </td>
                      <td>
                        <template v-if="safeParseDcThemePlates(row.rawJson).length || safeParseDcUiLabels(row.rawJson).length || safeParseDcTagSystem(row.rawJson)">
                          <n-space size="small" :wrap="true">
                            <n-tag v-for="t in safeParseDcThemePlates(row.rawJson)" :key="t" size="small" type="info">
                              {{ t }}
                            </n-tag>
                          </n-space>
                          <div v-if="safeParseDcUiLabels(row.rawJson).length" style="margin-top: 6px;">
                            <n-space size="small" :wrap="true">
                              <n-tag v-for="t in safeParseDcUiLabels(row.rawJson)" :key="t" size="small" type="warning">
                                {{ t }}
                              </n-tag>
                            </n-space>
                          </div>

                          <div v-if="safeParseDcTagSystem(row.rawJson)" style="margin-top: 6px;">
                            <n-space size="small" :wrap="true">
                              <n-tag v-for="t in (safeParseDcTagSystem(row.rawJson)?.theme || [])" :key="'ts_theme_'+t" size="small" type="info">
                                {{ t }}
                              </n-tag>
                              <n-tag v-for="t in (safeParseDcTagSystem(row.rawJson)?.signal || [])" :key="'ts_signal_'+t" size="small" type="error">
                                {{ t }}
                              </n-tag>
                              <n-tag v-for="t in (safeParseDcTagSystem(row.rawJson)?.source || [])" :key="'ts_source_'+t" size="small" type="default">
                                {{ t }}
                              </n-tag>
                              <n-tag v-for="t in (safeParseDcTagSystem(row.rawJson)?.other || [])" :key="'ts_other_'+t" size="small" type="success">
                                {{ t }}
                              </n-tag>
                            </n-space>
                          </div>

                          <div v-if="safeParseDcHotAnalysis(row.rawJson).length" style="margin-top: 6px;">
                            <n-popover trigger="click" placement="left" :width="520">
                              <template #trigger>
                                <n-button size="tiny" quaternary>
                                  热点解读({{ safeParseDcHotAnalysis(row.rawJson).length }})
                                </n-button>
                              </template>
                              <div style="max-height: 360px; overflow: auto;">
                                <div
                                  v-for="(it, idx) in safeParseDcHotAnalysis(row.rawJson)"
                                  :key="it?.id || it?.title || idx"
                                  style="margin-bottom: 12px;"
                                >
                                  <div style="font-weight: 600;">
                                    <a
                                      v-if="it?.id"
                                      :href="`https://gubatopic.eastmoney.com/topic_v3.html?htid=${it.id}&jumph5=1`"
                                      target="_blank"
                                      rel="noopener noreferrer"
                                    >
                                      {{ it?.title || it?.summary || it?.id }}
                                    </a>
                                    <span v-else>
                                      {{ it?.title || it?.summary || '-' }}
                                    </span>
                                  </div>
                                  <div v-if="it?.summary" style="margin-top: 4px; white-space: pre-wrap;">
                                    {{ it.summary }}
                                  </div>
                                </div>
                              </div>
                            </n-popover>
                          </div>
                        </template>
                        <n-text v-else depth="3">-</n-text>
                      </td>
                      <td>{{ _?.quote?.f2 ?? '-' }}</td>
                      <td>
                        <n-tag :type="(row.riseAndFall || 0) >= 0 ? 'error' : 'success'" size="small">
                          {{ row.riseAndFall }}
                        </n-tag>
                      </td>
                      <td>{{ _?.quote?.f4 ?? '-' }}</td>
                      <td>{{ _?.quote?.f5 ?? '-' }}</td>
                      <td>{{ _?.quote?.f6 ?? '-' }}</td>
                      <td>{{ _?.quote?.f8 ?? '-' }}</td>
                      <td>{{ _?.quote?.f20 ?? '-' }}</td>
                      <td>{{ _?.quote?.f21 ?? '-' }}</td>
                      <td>{{ _?.rank?.rc ?? '-' }}</td>
                      <td>{{ _?.rank?.hisRc ?? '-' }}</td>
                      <td>
                        <n-popover trigger="hover" placement="left" :width="520">
                          <template #trigger>
                            <n-button size="tiny">查看</n-button>
                          </template>
                          <pre style="margin:0; white-space: pre-wrap; word-break: break-all; max-height: 380px; overflow: auto;">{{ row.rawJson }}</pre>
                        </n-popover>
                      </td>
                    </template>
                  </template>
                </tr>
              </tbody>
            </n-table>
          </div>
        </n-spin>
      </n-tab-pane>

      <n-tab-pane name="cls" tab="财联社">
        <n-empty description="暂未接入（后续可加抓取+落库）" />
      </n-tab-pane>

      <n-tab-pane name="tdx" tab="通达信">
        <n-empty description="暂未接入（后续可加抓取+落库）" />
      </n-tab-pane>
    </n-tabs>

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
