<template>
  <div class="dict-container">
    <n-card title="词典管理" size="small">
      <template #header-extra>
        <n-space>
          <n-button type="primary" size="small" @click="showAddDialog = true">
            <template #icon><n-icon><AddIcon /></n-icon></template>
            添加词汇
          </n-button>
          <n-button size="small" @click="loadDictionary">
            <template #icon><n-icon><RefreshIcon /></n-icon></template>
            刷新
          </n-button>
        </n-space>
      </template>

      <!-- 筛选条件 -->
      <n-space style="margin-bottom: 16px;">
        <n-select v-model:value="filterType" :options="typeOptions" placeholder="词汇类型" style="width: 150px;" clearable />
        <n-input v-model:value="searchWord" placeholder="搜索词汇" clearable style="width: 200px;" />
        <n-button @click="loadDictionary">搜索</n-button>
      </n-space>

      <!-- 统计信息 -->
      <n-space style="margin-bottom: 16px;">
        <n-tag type="error">正面: {{ stats.positive }}</n-tag>
        <n-tag type="success">负面: {{ stats.negative }}</n-tag>
        <n-tag type="warning">行业: {{ stats.industry }}</n-tag>
        <n-tag type="info">概念: {{ stats.concept }}</n-tag>
        <n-tag>普通: {{ stats.normal }}</n-tag>
        <n-tag>总计: {{ stats.total }}</n-tag>
      </n-space>

      <!-- 词汇列表 -->
      <n-data-table
        :columns="columns"
        :data="dictList"
        :pagination="pagination"
        :loading="loading"
        size="small"
      />
    </n-card>

    <!-- 添加/编辑对话框 -->
    <n-modal v-model:show="showAddDialog" preset="dialog" title="添加词汇" positive-text="确定" negative-text="取消" @positive-click="handleAdd">
      <n-form ref="formRef" :model="formData" label-placement="left" label-width="100">
        <n-form-item label="词汇" path="word" required>
          <n-input v-model:value="formData.word" placeholder="请输入词汇" />
        </n-form-item>
        <n-form-item label="类型" path="wordType" required>
          <n-select v-model:value="formData.wordType" :options="typeOptions" />
        </n-form-item>
        <n-form-item label="权重" path="baseWeight">
          <n-input-number v-model:value="formData.baseWeight" :min="0" :max="10" :step="0.5" style="width: 100%;" />
        </n-form-item>
        <n-form-item label="情感值" path="sentimentValue">
          <n-input-number v-model:value="formData.sentimentValue" :min="-10" :max="10" :step="0.1" style="width: 100%;" />
        </n-form-item>
        <n-form-item label="行业" path="industry">
          <n-input v-model:value="formData.industry" placeholder="如：金融、科技等" />
        </n-form-item>
      </n-form>
    </n-modal>
  </div>
</template>

<script setup>
import { ref, onMounted, h } from 'vue'
import { NButton, NTag, NSpace, useMessage } from 'naive-ui'
import { Add as AddIcon, Refresh as RefreshIcon } from '@vicons/ionicons5'

const message = useMessage()
const loading = ref(false)
const dictList = ref([])
const showAddDialog = ref(false)
const filterType = ref(null)
const searchWord = ref('')

const stats = ref({
  positive: 0,
  negative: 0,
  industry: 0,
  concept: 0,
  normal: 0,
  total: 0
})

const formData = ref({
  word: '',
  wordType: 'normal',
  baseWeight: 1.0,
  sentimentValue: 0,
  industry: ''
})

const typeOptions = [
  { label: '正面', value: 'positive' },
  { label: '负面', value: 'negative' },
  { label: '行业', value: 'industry' },
  { label: '概念', value: 'concept' },
  { label: '股票', value: 'stock' },
  { label: '普通', value: 'normal' }
]

const pagination = ref({
  page: 1,
  pageSize: 20,
  showSizePicker: true,
  pageSizes: [10, 20, 50, 100]
})

const columns = [
  { title: '词汇', key: 'word', width: 120, fixed: 'left' },
  { 
    title: '类型', 
    key: 'wordType', 
    width: 100,
    render: (row) => {
      const typeMap = {
        positive: { text: '正面', type: 'error' },
        negative: { text: '负面', type: 'success' },
        industry: { text: '行业', type: 'warning' },
        concept: { text: '概念', type: 'info' },
        stock: { text: '股票', type: 'primary' },
        normal: { text: '普通', type: 'default' }
      }
      const config = typeMap[row.wordType] || typeMap.normal
      return h(NTag, { type: config.type, size: 'small' }, { default: () => config.text })
    }
  },
  { title: '权重', key: 'baseWeight', width: 80 },
  { title: '情感值', key: 'sentimentValue', width: 90 },
  { title: '行业', key: 'industry', width: 100 },
  { 
    title: '系统词', 
    key: 'isSystem', 
    width: 80,
    render: (row) => h(NTag, { 
      type: row.isSystem ? 'info' : 'default', 
      size: 'small' 
    }, { default: () => row.isSystem ? '是' : '否' })
  },
  { title: '总频次', key: 'frequencyTotal', width: 90 },
  { title: '最后出现', key: 'lastSeenDate', width: 120 },
  {
    title: '操作',
    key: 'actions',
    width: 150,
    fixed: 'right',
    render: (row) => h(NSpace, {}, {
      default: () => [
        h(NButton, {
          size: 'small',
          type: 'primary',
          text: true,
          onClick: () => handleEdit(row)
        }, { default: () => '编辑' }),
        h(NButton, {
          size: 'small',
          type: 'error',
          text: true,
          disabled: row.isSystem,
          onClick: () => handleDelete(row)
        }, { default: () => '删除' })
      ]
    })
  }
]

const loadDictionary = async () => {
  loading.value = true
  try {
    const response = await fetch('/api/word-dictionary/list?' + new URLSearchParams({
      wordType: filterType.value || '',
      word: searchWord.value || '',
      page: pagination.value.page,
      pageSize: pagination.value.pageSize
    }))
    const data = await response.json()
    dictList.value = data.list || []
    stats.value = data.stats || stats.value
    pagination.value.itemCount = data.total || 0
  } catch (error) {
    message.error('加载词典失败: ' + error.message)
  } finally {
    loading.value = false
  }
}

const handleAdd = async () => {
  try {
    const response = await fetch('/api/word-dictionary/add', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(formData.value)
    })
    if (response.ok) {
      message.success('添加成功')
      showAddDialog.value = false
      loadDictionary()
      // 重置表单
      formData.value = {
        word: '',
        wordType: 'normal',
        baseWeight: 1.0,
        sentimentValue: 0,
        industry: ''
      }
    } else {
      message.error('添加失败')
    }
  } catch (error) {
    message.error('添加失败: ' + error.message)
  }
}

const handleEdit = (row) => {
  formData.value = { ...row }
  showAddDialog.value = true
}

const handleDelete = async (row) => {
  try {
    const response = await fetch(`/api/word-dictionary/delete/${row.id}`, {
      method: 'DELETE'
    })
    if (response.ok) {
      message.success('删除成功')
      loadDictionary()
    } else {
      message.error('删除失败')
    }
  } catch (error) {
    message.error('删除失败: ' + error.message)
  }
}

onMounted(() => {
  loadDictionary()
})
</script>

<style scoped>
.dict-container {
  padding: 16px;
}
</style>
