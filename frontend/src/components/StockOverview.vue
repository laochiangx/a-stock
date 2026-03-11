<script setup>
import { ref, onMounted } from 'vue'
import {
  NCard,
  NButton,
  NSpace,
  NDatePicker,
  NSpin,
  NEmpty,
  NTable,
  NTag,
  NText,
  NFlex,
  NScrollbar,
  NCollapse,
  NCollapseItem,
  NModal
} from 'naive-ui'
import { GetStockOverviewList, GetStockOverviewDetail, SyncThsHotListDay } from '../api/api'
import KLineChart from './KLineChart.vue'

const loading = ref(false)
const detailLoading = ref(false)
const syncing = ref(false)

const selectedDate = ref(Date.now())
const rows = ref([])
const selectedRow = ref(null)
const detail = ref(null)

const kModalShow = ref(false)
const kModalCode = ref('')
const kModalName = ref('')

const normalizeMarketCode = (code) => {
  const c = String(code || '').trim()
  if (!c) return ''
  if (c.startsWith('sh') || c.startsWith('sz') || c.startsWith('bj') || c.startsWith('hk') || c.startsWith('gb_')) return c
  if (/^6\d{5}$/.test(c)) return `sh${c}`
  if (/^[03]\d{5}$/.test(c)) return `sz${c}`
  if (/^[48]\d{5}$/.test(c)) return `bj${c}`
  return c
}

const openKLine = (row) => {
  kModalCode.value = normalizeMarketCode(row?.stockCode)
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

const safeParseJson = (v) => {
  if (!v) return null
  if (typeof v === 'object') return v
  try {
    return JSON.parse(String(v))
  } catch (e) {
    return null
  }
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

const refreshList = async () => {
  loading.value = true
  try {
    const dateStr = formatDate(selectedDate.value)
    const res = await GetStockOverviewList(dateStr, 'a', 'normal')
    if (res && res.success && Array.isArray(res.data)) {
      rows.value = res.data
      if (rows.value.length > 0) {
        await selectRow(rows.value[0])
      }
    } else {
      rows.value = []
      selectedRow.value = null
      detail.value = null
    }
  } finally {
    loading.value = false
  }
}

const syncThs = async (force) => {
  syncing.value = true
  try {
    const dateStr = formatDate(selectedDate.value)
    await SyncThsHotListDay(dateStr, !!force, 'a', 'normal')
    await refreshList()
  } finally {
    syncing.value = false
  }
}

const selectRow = async (row) => {
  selectedRow.value = row
  detail.value = null
  detailLoading.value = true
  try {
    const dateStr = formatDate(selectedDate.value)
    const code = row?.stockCode
    if (!code) return
    const res = await GetStockOverviewDetail(dateStr, code, 'a', 'normal')
    if (res && res.success) {
      detail.value = res
    } else {
      detail.value = null
    }
  } finally {
    detailLoading.value = false
  }
}

onMounted(async () => {
  await refreshList()
})
</script>

<template>
  <div style="padding: 12px;">
    <n-card size="small">
      <n-flex justify="space-between" align="center" :wrap="true">
        <n-space align="center">
          <n-date-picker v-model:value="selectedDate" type="date" @update:value="() => { refreshList(); }" />
          <n-button size="small" type="primary" :loading="syncing" @click="syncThs(true)">获取同花顺热榜并入库</n-button>
          <n-button size="small" :loading="loading" @click="refreshList">刷新列表</n-button>
        </n-space>
        <n-text depth="3">
          左侧按同花顺热榜顺序，右侧聚合多源信息（选股通/东财/舞阳/标签系统/热门题材明细）
        </n-text>
      </n-flex>
    </n-card>

    <n-spin :show="loading || syncing || detailLoading" style="margin-top: 12px;">
      <div style="display: flex; gap: 12px;">
        <n-card size="small" style="width: 360px;">
          <template #header>
            <n-flex justify="space-between" align="center">
              <n-text strong>同花顺热榜</n-text>
              <n-tag v-if="rows && rows.length" type="info" size="small">{{ rows.length }}条</n-tag>
            </n-flex>
          </template>

          <n-scrollbar style="max-height: calc(100vh - 220px);">
            <div v-if="!rows || rows.length === 0">
              <n-empty description="暂无数据（可点上方按钮抓取入库）" />
            </div>
            <div v-else>
              <div
                v-for="r in rows"
                :key="r.id || r.stockCode || r.orderNum"
                @click="selectRow(r)"
                style="padding: 10px; border-bottom: 1px solid rgba(255,255,255,0.06); cursor: pointer;"
                :style="{ background: selectedRow?.stockCode === r.stockCode ? 'rgba(24,144,255,0.10)' : 'transparent' }"
              >
                <n-flex justify="space-between" align="center">
                  <n-space size="small" align="center">
                    <n-tag size="small" type="default">{{ r.orderNum }}</n-tag>
                    <n-button text type="info" @click.stop="openKLine(r)">{{ r.stockCode }}</n-button>
                    <n-button text type="info" @click.stop="openKLine(r)">{{ r.stockName }}</n-button>
                  </n-space>
                  <n-tag size="small" :type="(r.riseAndFall || 0) >= 0 ? 'error' : 'success'">{{ r.riseAndFall ?? '-' }}</n-tag>
                </n-flex>
                <div style="margin-top: 6px;">
                  <n-space size="small" :wrap="true">
                    <n-tag v-for="t in safeParseConceptTags(r.conceptTags)" :key="t" size="small" type="info">{{ t }}</n-tag>
                    <n-tag v-if="r.popularityTag" size="small" type="warning">{{ r.popularityTag }}</n-tag>
                  </n-space>
                </div>
              </div>
            </div>
          </n-scrollbar>
        </n-card>

        <n-card size="small" style="flex: 1; min-width: 0;">
          <template #header>
            <n-flex justify="space-between" align="center">
              <n-text strong>综合信息</n-text>
              <n-text depth="3" v-if="detail?.date">date={{ detail.date }}</n-text>
            </n-flex>
          </template>

          <div v-if="!selectedRow">
            <n-empty description="请先从左侧选择股票" />
          </div>

          <div v-else-if="!detail || !detail.data">
            <n-empty description="暂无详情数据" />
          </div>

          <div v-else>
            <n-collapse>
              <n-collapse-item title="热榜信息（THS/DC/CLS）" name="hot">
                <n-table size="small" :single-line="false">
                  <thead>
                    <tr>
                      <th style="width: 80px;">来源</th>
                      <th style="width: 90px;">排名</th>
                      <th style="width: 120px;">热度</th>
                      <th style="width: 120px;">涨跌幅</th>
                      <th>raw</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <td>THS</td>
                      <td>{{ detail.data.thsHot?.orderNum ?? '-' }}</td>
                      <td>{{ detail.data.thsHot?.rate ?? '-' }}</td>
                      <td>{{ detail.data.thsHot?.riseAndFall ?? '-' }}</td>
                      <td><pre style="margin:0; white-space: pre-wrap;">{{ detail.data.thsHot?.rawJson || '' }}</pre></td>
                    </tr>
                    <tr>
                      <td>DC</td>
                      <td>{{ detail.data.dcHot?.orderNum ?? '-' }}</td>
                      <td>{{ detail.data.dcHot?.hotScore ?? '-' }}</td>
                      <td>{{ detail.data.dcHot?.riseAndFall ?? '-' }}</td>
                      <td><pre style="margin:0; white-space: pre-wrap;">{{ detail.data.dcHot?.rawJson || '' }}</pre></td>
                    </tr>
                    <tr>
                      <td>CLS</td>
                      <td>{{ detail.data.clsHot?.orderNum ?? '-' }}</td>
                      <td>{{ detail.data.clsHot?.hotScore ?? '-' }}</td>
                      <td>{{ detail.data.clsHot?.riseAndFall ?? '-' }}</td>
                      <td><pre style="margin:0; white-space: pre-wrap;">{{ detail.data.clsHot?.rawJson || '' }}</pre></td>
                    </tr>
                  </tbody>
                </n-table>
              </n-collapse-item>

              <n-collapse-item title="标签/打标系统（stock_daily_snapshot + tags）" name="tags">
                <div v-if="!detail.data.tagSystem">
                  <n-empty description="暂无标签系统数据" />
                </div>
                <div v-else>
                  <n-space size="small" :wrap="true">
                    <n-tag v-for="t in (detail.data.tagSystem.theme || [])" :key="'theme_'+t" size="small" type="info">{{ t }}</n-tag>
                    <n-tag v-for="t in (detail.data.tagSystem.signal || [])" :key="'signal_'+t" size="small" type="error">{{ t }}</n-tag>
                    <n-tag v-for="t in (detail.data.tagSystem.source || [])" :key="'source_'+t" size="small" type="warning">{{ t }}</n-tag>
                    <n-tag v-for="t in (detail.data.tagSystem.other || [])" :key="'other_'+t" size="small" type="success">{{ t }}</n-tag>
                  </n-space>
                  <div style="margin-top: 10px;" v-if="detail.data.tagSystem.snapshot">
                    <n-text depth="3">snapshot：</n-text>
                    <pre style="white-space: pre-wrap; margin: 0;">{{ JSON.stringify(detail.data.tagSystem.snapshot, null, 2) }}</pre>
                  </div>
                  <div style="margin-top: 10px;" v-if="detail.data.tagSystem.evidence">
                    <n-text depth="3">evidence：</n-text>
                    <pre style="white-space: pre-wrap; margin: 0;">{{ JSON.stringify(detail.data.tagSystem.evidence, null, 2) }}</pre>
                  </div>
                </div>
              </n-collapse-item>

              <n-collapse-item title="选股通题材（theme_plate_data）" name="xgt">
                <div v-if="!detail.data.xuangutongThemes || detail.data.xuangutongThemes.length === 0">
                  <n-empty description="暂无选股通题材" />
                </div>
                <div v-else>
                  <n-table size="small" :single-line="false">
                    <thead>
                      <tr>
                        <th style="width: 140px;">题材</th>
                        <th style="width: 90px;">涨跌%</th>
                        <th style="width: 90px;">价格</th>
                        <th style="width: 120px;">涨停/连板</th>
                        <th>描述</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="(it, idx) in detail.data.xuangutongThemes" :key="it.id || idx">
                        <td>{{ it.plateName }}</td>
                        <td>{{ it.changeRate }}</td>
                        <td>{{ it.price }}</td>
                        <td>{{ it.mDaysNBoards }}</td>
                        <td style="white-space: pre-wrap;">{{ it.description }}</td>
                      </tr>
                    </tbody>
                  </n-table>
                </div>
              </n-collapse-item>

              <n-collapse-item title="东财题材（dongcai_theme_plate）" name="dcTheme">
                <div v-if="!detail.data.dongcaiThemes || detail.data.dongcaiThemes.length === 0">
                  <n-empty description="暂无东财题材" />
                </div>
                <div v-else>
                  <n-table size="small" :single-line="false">
                    <thead>
                      <tr>
                        <th style="width: 140px;">板块</th>
                        <th style="width: 120px;">连板</th>
                        <th style="width: 90px;">涨跌%</th>
                        <th>描述</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="(it, idx) in detail.data.dongcaiThemes" :key="it.id || idx">
                        <td>{{ it.plateName }}</td>
                        <td>{{ it.lianban }}</td>
                        <td>{{ it.changeRate }}</td>
                        <td style="white-space: pre-wrap;">{{ it.description }}</td>
                      </tr>
                    </tbody>
                  </n-table>
                </div>
              </n-collapse-item>

              <n-collapse-item title="舞阳题材（wuyang_theme_subject）" name="wy">
                <div v-if="!detail.data.wuyangSubjects || detail.data.wuyangSubjects.length === 0">
                  <n-empty description="暂无舞阳题材" />
                </div>
                <div v-else>
                  <n-table size="small" :single-line="false">
                    <thead>
                      <tr>
                        <th style="width: 140px;">题材</th>
                        <th style="width: 80px;">是否涨停</th>
                        <th style="width: 120px;">连板</th>
                        <th style="width: 120px;">原因</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="(it, idx) in detail.data.wuyangSubjects" :key="it.id || idx">
                        <td>{{ it.subjectName }}</td>
                        <td>{{ it.isZt ? '是' : '否' }}</td>
                        <td>{{ it.lbCount }}</td>
                        <td style="white-space: pre-wrap;">{{ it.reason }}</td>
                      </tr>
                    </tbody>
                  </n-table>
                </div>
              </n-collapse-item>

              <n-collapse-item title="东财热门题材明细入库（dc_hot_theme_stock_day）" name="dcHotTheme">
                <div v-if="!detail.data.dcHotThemeStocks || !detail.data.dcHotThemeStocks.data || detail.data.dcHotThemeStocks.data.length === 0">
                  <n-empty description="暂无记录（可能该股未出现在热门题材明细里）" />
                </div>
                <div v-else>
                  <n-text depth="3">count={{ detail.data.dcHotThemeStocks.count }}</n-text>
                  <div style="margin-top: 10px;">
                    <n-collapse>
                      <n-collapse-item
                        v-for="(it, idx) in detail.data.dcHotThemeStocks.data"
                        :key="(it.themeCode || idx) + '_dcHotTheme'"
                        :title="'themeCode=' + (it.themeCode || '-')"
                      >
                        <pre style="white-space: pre-wrap; margin: 0;">{{ it.rawJson || '' }}</pre>
                      </n-collapse-item>
                    </n-collapse>
                  </div>
                </div>
              </n-collapse-item>

              <n-collapse-item title="原始聚合JSON（debug）" name="raw">
                <pre style="white-space: pre-wrap; margin: 0;">{{ JSON.stringify(detail, null, 2) }}</pre>
              </n-collapse-item>
            </n-collapse>
          </div>
        </n-card>
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
