<script setup>
import { ref, onMounted, h, computed } from 'vue'
import { NCard, NDataTable, NButton, NSpace, NTag, NText, NInput, NSelect, NDatePicker, NModal, NForm, NFormItem, NTransfer, useMessage, NSpin } from 'naive-ui'
import KLineChart from './KLineChart.vue'

const message = useMessage()
const loading = ref(false)
const snapshotList = ref([])
const allTags = ref([])
const showTagModal = ref(false)
const editingSnapshot = ref({})
const selectedTagIds = ref([])

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
  const snapshot = row?.snapshot || {}
  kModalCode.value = normalizeMarketCode(snapshot.stockCode)
  kModalName.value = String(snapshot.stockName || '').trim()
  if (!kModalCode.value) return
  kModalShow.value = true
}

const dateValue = ref(Date.now())
const filters = ref({
  source: '',
  tagId: null,
  keyword: ''
})

const formatDate = (ts) => {
  if (!ts) return ''
  const d = new Date(ts)
  const yyyy = d.getFullYear()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd}`
}

const dateStr = computed(() => formatDate(dateValue.value))

const pagination = ref({
  page: 1,
  pageSize: 50,
  total: 0
})

const tagOptions = computed(() => {
  return allTags.value.map(tag => ({
    label: `${tag.name} (${tag.type})`,
    value: tag.id
  }))
})

const sourceOptions = [
  { label: '全部', value: '' },
  { label: '选股通题材', value: 'theme_xgt' },
  { label: '东财题材', value: 'theme_dc' },
  { label: '龙虎榜', value: 'dragon_tiger' },
  { label: '人气聚合', value: 'popularity_rank' }
]

const columns = [
  { 
    title: '股票代码', 
    key: 'stockCode', 
    width: 100,
    render: (row) => {
      const snapshot = row.snapshot
      return h(NButton, { text: true, type: 'info', onClick: () => openKLine(row) }, { default: () => snapshot.stockCode })
    }
  },
  { 
    title: '股票名称', 
    key: 'stockName', 
    width: 120,
    render: (row) => h(NButton, { text: true, type: 'info', onClick: () => openKLine(row) }, { default: () => row.snapshot.stockName || '-' })
  },
  { 
    title: '来源', 
    key: 'sources', 
    width: 180,
    render: (row) => {
      const sources = (row.snapshot.sources || '').split(',').filter(s => s)
      return h(NSpace, { size: 'small' }, {
        default: () => sources.map(source => 
          h(NTag, { size: 'small', type: 'info' }, { default: () => source })
        )
      })
    }
  },
  { 
    title: '标签', 
    key: 'tags', 
    width: 300,
    render: (row) => {
      const tags = row.tags || []
      if (tags.length === 0) {
        return h(NText, { depth: 3 }, { default: () => '-' })
      }
      return h(NSpace, { size: 'small' }, {
        default: () => tags.map(tag => 
          h(NTag, { 
            size: 'small', 
            type: tag.type === 'source' ? 'success' : tag.type === 'signal' ? 'warning' : 'info' 
          }, { 
            default: () => `${tag.name}(${tag.priority || 0})` 
          })
        )
      })
    }
  },
  { 
    title: '人气排名', 
    key: 'popularity', 
    width: 100,
    render: (row) => {
      const snapshot = row.snapshot
      const dcpm = snapshot.dcpm
      const thspm = snapshot.thspm
      return dcpm && thspm ? `${dcpm}/${thspm}` : '-'
    }
  },
  { 
    title: '操作', 
    key: 'actions', 
    width: 120,
    render: (row) => h(NSpace, {}, {
      default: () => [
        h(NButton, {
          size: 'small',
          type: 'primary',
          text: true,
          onClick: () => openTagModal(row)
        }, { default: () => '编辑标签' })
      ]
    })
  }
]

const loadSnapshots = async () => {
  loading.value = true
  try {
    const params = new URLSearchParams({
      date: dateStr.value,
      source: filters.value.source,
      tagId: filters.value.tagId || '',
      keyword: filters.value.keyword,
      page: pagination.value.page,
      pageSize: pagination.value.pageSize
    })
    
    const response = await fetch(`/api/stock-tags/daily/list?${params}`)
    const data = await response.json()
    
    if (data.success) {
      snapshotList.value = data.list || []
      pagination.value.total = data.total || 0
    } else {
      message.error('加载快照失败: ' + data.message)
    }
  } catch (error) {
    message.error('加载快照失败: ' + error.message)
  } finally {
    loading.value = false
  }
}

const loadTags = async () => {
  try {
    const response = await fetch('/api/tags/list')
    const data = await response.json()
    if (data.success) {
      allTags.value = data.list || []
    }
  } catch (error) {
    console.error('加载标签失败:', error)
  }
}

const openTagModal = (row) => {
  editingSnapshot.value = row
  selectedTagIds.value = (row.tags || []).map(tag => tag.id)
  showTagModal.value = true
}

const saveTags = async () => {
  try {
    const payload = {
      date: dateStr.value,
      stockCode: editingSnapshot.value.snapshot.stockCode,
      stockName: editingSnapshot.value.snapshot.stockName,
      tagIds: selectedTagIds.value
    }
    
    const response = await fetch('/api/stock-tags/daily/set-tags', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    })
    
    const data = await response.json()
    if (data.success) {
      message.success('标签保存成功')
      showTagModal.value = false
      loadSnapshots()
    } else {
      message.error('标签保存失败: ' + data.message)
    }
  } catch (error) {
    message.error('标签保存失败: ' + error.message)
  }
}

const runTagging = async () => {
  try {
    const response = await fetch(`/api/stock-tags/run?date=${dateStr.value}`, { method: 'POST' })
    const data = await response.json()
    if (data.success) {
      message.success(`打标完成，快照数: ${data.snapshotCount}，标签关联数: ${data.tagLinkCount}`)
      loadSnapshots()
    } else {
      message.error('打标失败: ' + data.message)
    }
  } catch (error) {
    message.error('打标失败: ' + error.message)
  }
}

const handlePageChange = (page) => {
  pagination.value.page = page
  loadSnapshots()
}

const handlePageSizeChange = (pageSize) => {
  pagination.value.pageSize = pageSize
  pagination.value.page = 1
  loadSnapshots()
}

onMounted(() => {
  loadTags()
  loadSnapshots()
})
</script>

<template>
  <div class="stock-tag-daily-container">
    <n-card title="每日热门股票 & 标签管理" size="small">
      <template #header-extra>
        <n-space>
          <n-date-picker v-model:value="dateValue" type="date" @update:value="() => { pagination.page = 1; loadSnapshots() }" />
          <n-select v-model:value="filters.source" :options="sourceOptions" @update:value="() => { pagination.page = 1; loadSnapshots() }" />
          <n-select v-model:value="filters.tagId" clearable :options="tagOptions" style="min-width: 220px;" placeholder="按标签筛选" @update:value="() => { pagination.page = 1; loadSnapshots() }" />
          <n-input v-model:value="filters.keyword" placeholder="搜索股票代码/名称" @keyup.enter="() => { pagination.page = 1; loadSnapshots() }" />
          <n-button size="small" @click="runTagging" type="warning">执行打标</n-button>
          <n-button size="small" @click="loadSnapshots">刷新</n-button>
        </n-space>
      </template>
      
      <n-spin :show="loading">
        <n-data-table
          :columns="columns"
          :data="snapshotList"
          :pagination="{
            page: pagination.page,
            pageSize: pagination.pageSize,
            itemCount: pagination.total,
            showSizePicker: true,
            pageSizes: [20, 50, 100],
            onChange: handlePageChange,
            onUpdatePageSize: handlePageSizeChange
          }"
          :max-height="'calc(100vh - 250px)'"
          size="small"
        />
      </n-spin>
    </n-card>

    <!-- 标签编辑弹窗 -->
    <n-modal v-model:show="showTagModal" preset="dialog" title="编辑股票标签" positive-text="保存" negative-text="取消" @positive-click="saveTags" style="width: 600px;">
      <n-form label-placement="left" label-width="100">
        <n-form-item label="股票代码">
          <n-input :value="editingSnapshot.snapshot?.stockCode" readonly />
        </n-form-item>
        <n-form-item label="股票名称">
          <n-input :value="editingSnapshot.snapshot?.stockName" readonly />
        </n-form-item>
        <n-form-item label="选择标签">
          <n-transfer
            v-model:value="selectedTagIds"
            :options="tagOptions"
            source-title="可选标签"
            target-title="已选标签"
          />
        </n-form-item>
      </n-form>
    </n-modal>

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
.stock-tag-daily-container {
  padding: 16px;
}
</style>
