<script setup>
import { ref, onMounted, h } from 'vue'
import { NCard, NDataTable, NSwitch, NButton, NSpace, NTag, NText, NInput, NInputNumber, NModal, NForm, NFormItem, useMessage, NSpin } from 'naive-ui'

const message = useMessage()
const loading = ref(false)
const menuList = ref([])
const menuTree = ref([])
const showEditModal = ref(false)
const editingMenu = ref({})

// 表格列定义
const columns = [
  { 
    title: '菜单名称', 
    key: 'menuName',
    width: 150,
    render: (row) => {
      const indent = row.parentKey ? '　　' : ''
      return h('span', {}, indent + row.menuName)
    }
  },
  { title: '菜单Key', key: 'menuKey', width: 120 },
  { title: '图标', key: 'menuIcon', width: 150 },
  { title: '路径', key: 'menuPath', width: 200, ellipsis: { tooltip: true } },
  { title: '排序', key: 'sortOrder', width: 80 },
  { 
    title: '显示', 
    key: 'isVisible', 
    width: 80,
    render: (row) => h(NSwitch, {
      value: row.isVisible,
      onUpdateValue: (val) => toggleVisible(row, val)
    })
  },
  { 
    title: '系统菜单', 
    key: 'isSystem', 
    width: 90,
    render: (row) => h(NTag, { 
      type: row.isSystem ? 'info' : 'default',
      size: 'small'
    }, { default: () => row.isSystem ? '是' : '否' })
  },
  {
    title: '操作',
    key: 'actions',
    width: 100,
    render: (row) => h(NSpace, {}, {
      default: () => [
        h(NButton, {
          size: 'small',
          type: 'primary',
          text: true,
          onClick: () => openEditModal(row)
        }, { default: () => '编辑' })
      ]
    })
  }
]

// 加载菜单列表
const loadMenuList = async () => {
  loading.value = true
  try {
    const response = await fetch('/api/menu/list')
    const data = await response.json()
    if (data.success) {
      menuList.value = data.list || []
      menuTree.value = data.tree || []
      // 按树形结构排序展示
      menuList.value = flattenTree(data.tree)
    } else {
      message.error('加载菜单失败: ' + data.message)
    }
  } catch (error) {
    message.error('加载菜单失败: ' + error.message)
  } finally {
    loading.value = false
  }
}

// 将树形结构扁平化（保持层级顺序）
const flattenTree = (tree, result = []) => {
  for (const node of tree) {
    result.push({
      id: node.id,
      menuKey: node.key,
      menuName: node.name,
      menuIcon: node.icon,
      menuPath: node.path,
      parentKey: null,
      sortOrder: node.sort,
      isVisible: node.visible,
      isSystem: node.system
    })
    if (node.children && node.children.length > 0) {
      for (const child of node.children) {
        result.push({
          id: child.id,
          menuKey: child.key,
          menuName: child.name,
          menuIcon: child.icon,
          menuPath: child.path,
          parentKey: node.key,
          sortOrder: child.sort,
          isVisible: child.visible,
          isSystem: child.system
        })
      }
    }
  }
  return result
}

// 应用更改（刷新页面）
const applyChanges = () => {
  window.location.reload()
}

// 切换菜单可见性
const toggleVisible = async (row, visible) => {
  try {
    const response = await fetch('/api/menu/toggle-visible', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ menuKey: row.menuKey, visible })
    })
    const data = await response.json()
    if (data.success) {
      row.isVisible = visible
      message.success('更新成功')
    } else {
      message.error('更新失败: ' + data.message)
    }
  } catch (error) {
    message.error('更新失败: ' + error.message)
  }
}

// 打开编辑弹窗
const openEditModal = (row) => {
  editingMenu.value = { ...row }
  showEditModal.value = true
}

// 保存编辑
const saveEdit = async () => {
  try {
    const response = await fetch('/api/menu/update', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(editingMenu.value)
    })
    const data = await response.json()
    if (data.success) {
      message.success('保存成功')
      showEditModal.value = false
      loadMenuList()
    } else {
      message.error('保存失败: ' + data.message)
    }
  } catch (error) {
    message.error('保存失败: ' + error.message)
  }
}

onMounted(() => {
  loadMenuList()
})
</script>

<template>
  <div class="menu-manage-container">
    <n-card title="菜单管理" size="small">
      <template #header-extra>
        <n-space>
          <n-button size="small" @click="loadMenuList">刷新列表</n-button>
          <n-button size="small" type="primary" @click="applyChanges">应用更改</n-button>
          <n-text depth="3" style="font-size: 12px;">提示：点击"应用更改"刷新页面生效</n-text>
        </n-space>
      </template>
      
      <n-spin :show="loading">
        <n-data-table
          :columns="columns"
          :data="menuList"
          :max-height="'calc(100vh - 200px)'"
          size="small"
        />
      </n-spin>
    </n-card>

    <!-- 编辑弹窗 -->
    <n-modal v-model:show="showEditModal" preset="dialog" title="编辑菜单" positive-text="保存" negative-text="取消" @positive-click="saveEdit" style="width: 500px;">
      <n-form :model="editingMenu" label-placement="left" label-width="100">
        <n-form-item label="菜单Key">
          <n-input v-model:value="editingMenu.menuKey" disabled />
        </n-form-item>
        <n-form-item label="菜单名称">
          <n-input v-model:value="editingMenu.menuName" />
        </n-form-item>
        <n-form-item label="图标">
          <n-input v-model:value="editingMenu.menuIcon" />
        </n-form-item>
        <n-form-item label="路径">
          <n-input v-model:value="editingMenu.menuPath" />
        </n-form-item>
        <n-form-item label="排序">
          <n-input-number v-model:value="editingMenu.sortOrder" :min="0" style="width: 100%;" />
        </n-form-item>
        <n-form-item label="显示">
          <n-switch v-model:value="editingMenu.isVisible" />
        </n-form-item>
      </n-form>
    </n-modal>
  </div>
</template>

<style scoped>
.menu-manage-container {
  padding: 16px;
}
</style>
