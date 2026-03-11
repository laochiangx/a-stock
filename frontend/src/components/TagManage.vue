<script setup>
import { ref, onMounted, h } from 'vue'
import { NCard, NDataTable, NButton, NSpace, NTag, NText, NInput, NInputNumber, NModal, NForm, NFormItem, NSelect, useMessage, NSpin } from 'naive-ui'

const message = useMessage()
const loading = ref(false)
const tagList = ref([])
const showEditModal = ref(false)
const editingTag = ref({})
const typeOptions = ref([
  { label: '来源', value: 'source' },
  { label: '题材', value: 'theme' },
  { label: '信号', value: 'signal' },
  { label: '其他', value: 'other' }
])

const columns = [
  { title: 'ID', key: 'id', width: 80 },
  { title: '标签名称', key: 'name', width: 150 },
  { 
    title: '类型', 
    key: 'type', 
    width: 100,
    render: (row) => {
      const opt = typeOptions.value.find(o => o.value === row.type)
      return h(NTag, { type: 'info', size: 'small' }, { default: () => opt ? opt.label : row.type })
    }
  },
  { title: '优先级', key: 'priority', width: 80 },
  { 
    title: '创建时间', 
    key: 'createdAt', 
    width: 180,
    render: (row) => row.createdAt ? new Date(row.createdAt).toLocaleString() : ''
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
          onClick: () => openEditModal(row)
        }, { default: () => '编辑' }),
        h(NButton, {
          size: 'small',
          type: 'error',
          text: true,
          onClick: () => deleteTag(row.id)
        }, { default: () => '删除' })
      ]
    })
  }
]

const loadTags = async () => {
  loading.value = true
  try {
    const response = await fetch('/api/tags/list')
    const data = await response.json()
    if (data.success) {
      tagList.value = data.list || []
    } else {
      message.error('加载标签失败: ' + data.message)
    }
  } catch (error) {
    message.error('加载标签失败: ' + error.message)
  } finally {
    loading.value = false
  }
}

const openEditModal = (row) => {
  editingTag.value = { ...row }
  showEditModal.value = true
}

const saveEdit = async () => {
  try {
    const isUpdate = !!editingTag.value.id
    const url = isUpdate ? '/api/tags/update' : '/api/tags/add'
    const response = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(editingTag.value)
    })
    const data = await response.json()
    if (data.success) {
      message.success('保存成功')
      showEditModal.value = false
      loadTags()
    } else {
      message.error('保存失败: ' + data.message)
    }
  } catch (error) {
    message.error('保存失败: ' + error.message)
  }
}

const deleteTag = async (id) => {
  try {
    const response = await fetch(`/api/tags/delete/${id}`, { method: 'DELETE' })
    const data = await response.json()
    if (data.success) {
      message.success('删除成功')
      loadTags()
    } else {
      message.error('删除失败: ' + data.message)
    }
  } catch (error) {
    message.error('删除失败: ' + error.message)
  }
}

const addNewTag = () => {
  editingTag.value = { name: '', type: 'source', priority: 0 }
  showEditModal.value = true
}

onMounted(() => {
  loadTags()
})
</script>

<template>
  <div class="tag-manage-container">
    <n-card title="标签管理" size="small">
      <template #header-extra>
        <n-space>
          <n-button size="small" @click="loadTags">刷新</n-button>
          <n-button size="small" type="primary" @click="addNewTag">新增标签</n-button>
        </n-space>
      </template>
      
      <n-spin :show="loading">
        <n-data-table
          :columns="columns"
          :data="tagList"
          :max-height="'calc(100vh - 200px)'"
          size="small"
        />
      </n-spin>
    </n-card>

    <!-- 编辑弹窗 -->
    <n-modal v-model:show="showEditModal" preset="dialog" title="编辑标签" positive-text="保存" negative-text="取消" @positive-click="saveEdit" style="width: 500px;">
      <n-form :model="editingTag" label-placement="left" label-width="100">
        <n-form-item label="标签名称">
          <n-input v-model:value="editingTag.name" placeholder="请输入标签名称" />
        </n-form-item>
        <n-form-item label="标签类型">
          <n-select v-model:value="editingTag.type" :options="typeOptions" />
        </n-form-item>
        <n-form-item label="优先级">
          <n-input-number v-model:value="editingTag.priority" :min="0" style="width: 100%;" />
        </n-form-item>
      </n-form>
    </n-modal>
  </div>
</template>

<style scoped>
.tag-manage-container {
  padding: 16px;
}
</style>
