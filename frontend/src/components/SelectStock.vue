<script setup>
import { h, onBeforeMount, ref } from 'vue'
import { GetHotStrategy, SearchStock, Follow } from "../api/api";
import { useMessage, NText, NTag, NButton, NGrid, NGi, NList, NListItem, NScrollbar, NFlex, NInputGroup, NInput, NDataTable, NEllipsis, NEmpty, NCard, NTabs, NTabPane } from 'naive-ui'

const message = useMessage()
const search = ref('')
const columns = ref([])
const dataList = ref([])
const hotStrategy = ref([])
const traceInfo = ref('')
const tableScrollX = ref(2800)
const activeTab = ref('search')

// 计算表格总宽度
function calculateTableWidth(cols) {
  let totalWidth = 0;
  cols.forEach(col => {
    if (col.children && col.children.length > 0) {
      let childrenWidth = 0;
      col.children.forEach(child => {
        childrenWidth += child.width || child.minWidth || 100;
      });
      totalWidth += Math.max(col.width || col.minWidth || 200, childrenWidth);
    } else {
      totalWidth += col.width || col.minWidth || 120;
    }
  });
  totalWidth += 100;
  return Math.max(totalWidth, 1200);
}

function Search() {
  if (!search.value) {
    message.warning('请输入选股指标或者要求')
    return
  }

  const loading = message.loading("正在获取选股数据...", { duration: 0 });
  SearchStock(search.value).then(res => {
    loading.destroy()
    if (res.code == 100) {
      traceInfo.value = res.data?.traceInfo?.showText || ''
      columns.value = (res.data?.result?.columns || [])
        .filter(item => !item.hiddenNeed && (item.title != "市场码" && item.title != "市场简称"))
        .map(item => {
          if (item.children) {
            return {
              title: item.title + (item.unit ? '[' + item.unit + ']' : ''),
              key: item.key,
              resizable: true,
              minWidth: 200,
              ellipsis: { tooltip: true },
              children: item.children.filter(item => !item.hiddenNeed).map(item => ({
                title: item.dateMsg,
                key: item.key,
                minWidth: 100,
                resizable: true,
                ellipsis: { tooltip: true },
                sorter: (row1, row2) => {
                  if (isNumeric(row1[item.key]) && isNumeric(row2[item.key])) {
                    return row1[item.key] - row2[item.key];
                  } else {
                    return 'default'
                  }
                },
              }))
            }
          } else {
            return {
              title: item.title + (item.unit ? '[' + item.unit + ']' : ''),
              key: item.key,
              resizable: true,
              minWidth: 120,
              ellipsis: { tooltip: true },
              sorter: (row1, row2) => {
                if (isNumeric(row1[item.key]) && isNumeric(row2[item.key])) {
                  return row1[item.key] - row2[item.key];
                } else {
                  return 'default'
                }
              },
            }
          }
        })
      columns.value.push({
        title: '操作',
        key: 'actions',
        width: 80,
        fixed: 'right',
        render: (row) => {
          return h(
            NButton,
            {
              strong: true,
              tertiary: true,
              size: 'small',
              type: 'warning',
              style: 'font-size: 14px; padding: 0 10px;',
              onClick: () => handleFollow(row)
            },
            { default: () => '关注' }
          )
        }
      });
      dataList.value = res.data?.result?.dataList || []
      tableScrollX.value = calculateTableWidth(columns.value);
    } else {
      // API 返回错误，提示用户使用外部选股工具
      const errorMsg = res.msg || res.message || '选股条件无法识别'
      message.warning(errorMsg + '，建议使用同花顺问财或东财选股')
      dataList.value = []
    }
  }).catch(err => {
    loading.destroy()
    message.warning('选股服务暂不可用，请使用同花顺问财或东财选股')
    dataList.value = []
  })
}

function handleFollow(row) {
  let code = row.MARKET_SHORT_NAME?.toLowerCase() + row.SECURITY_CODE
  Follow(code).then(result => {
    if (result === "关注成功") {
      message.success(result)
    } else {
      message.error(result)
    }
  });
}

function isNumeric(value) {
  return !isNaN(parseFloat(value)) && isFinite(value);
}

onBeforeMount(() => {
  GetHotStrategy().then(res => {
    if (res.code == 1) {
      hotStrategy.value = res.data || []
      if (hotStrategy.value.length > 0) {
        search.value = hotStrategy.value[0].question
        Search()
      }
    }
  }).catch(err => {
    console.error('获取热门策略失败:', err)
    hotStrategy.value = []
  })
})

function DoSearch(question) {
  search.value = question
  Search()
}

function openCenteredWindow(url, width, height) {
  const left = (window.screen.width - width) / 2;
  const top = (window.screen.height - height) / 2;
  window.open(url, 'centeredWindow', `width=${width},height=${height},left=${left},top=${top},location=no,menubar=no,toolbar=no,display=standalone`)
}

// 打开同花顺问财页面
function openWenCai() {
  const query = encodeURIComponent(search.value || '涨停')
  openCenteredWindow(`https://www.iwencai.com/unifiedwap/result?w=${query}&querytype=stock`, 1400, 800)
}

// 打开东方财富选股页面
function openEastMoney() {
  const query = encodeURIComponent(search.value || '涨停')
  openCenteredWindow(`https://xuangu.eastmoney.com/?st=${query}`, 1400, 800)
}
</script>

<template>
  <n-grid :cols="24" style="max-height: calc(100vh - 165px)">
    <n-gi :span="4">
      <n-list bordered style="text-align: left;" hoverable clickable>
        <n-scrollbar style="max-height: calc(100vh - 170px);">
          <n-list-item v-for="item in hotStrategy" :key="item.rank" @click="DoSearch(item.question)">
            <n-ellipsis line-clamp="1" :tooltip="true">
              <n-tag size="small" :bordered="false" type="info">#{{ item.rank }}</n-tag>
              <n-text type="warning">{{ item.question }}</n-text>
              <template #tooltip>
                <div style="text-align: center;max-width: 180px">
                  <n-text type="warning">{{ item.question }}</n-text>
                </div>
              </template>
            </n-ellipsis>
          </n-list-item>
          <n-list-item v-if="hotStrategy.length === 0">
            <n-text depth="3">暂无热门策略</n-text>
          </n-list-item>
        </n-scrollbar>
      </n-list>
    </n-gi>
    <n-gi :span="20">
      <n-flex style="--wails-draggable:no-drag; margin-bottom: 8px;">
        <n-input-group style="text-align: left; flex: 1;">
          <n-input :rows="1" clearable v-model:value="search" placeholder="请输入选股指标或者要求，如：涨停、量比大于2、主力净流入" 
            @keyup.enter="Search" />
          <n-button type="primary" @click="Search">搜索A股</n-button>
        </n-input-group>
        <n-button type="info" @click="openWenCai">同花顺问财</n-button>
        <n-button type="warning" @click="openEastMoney">东财选股</n-button>
      </n-flex>
      
      <n-flex justify="start" v-if="traceInfo" style="margin: 5px 0;--wails-draggable:no-drag">
        <n-ellipsis line-clamp="1" :tooltip="true">
          <n-text type="info" :bordered="false">选股条件：</n-text>
          <n-text type="warning" :bordered="true">{{ traceInfo }}</n-text>
          <template #tooltip>
            <div style="text-align: center;max-width: 580px">
              <n-text type="warning">{{ traceInfo }}</n-text>
            </div>
          </template>
        </n-ellipsis>
      </n-flex>
      
      <n-empty v-if="dataList.length === 0 && hotStrategy.length === 0" 
        description="请输入选股条件搜索，或点击右上角按钮使用同花顺问财/东财选股" 
        style="margin-top: 100px;" />
      
      <n-data-table v-if="dataList.length > 0" 
        :striped="true" 
        :max-height="'calc(100vh - 220px)'" 
        size="medium" 
        :columns="columns"
        :data="dataList" 
        :pagination="{ pageSize: 15 }" 
        :scroll-x="tableScrollX"
        :render-cell="(value, rowData, column) => {
          if (column.key == 'SECURITY_CODE' || column.key == 'SERIAL') {
            return h(NText, { type: 'info', border: false }, { default: () => `${value}` })
          }
          if (isNumeric(value)) {
            let type = 'info';
            if (Number(value) < 0) {
              type = 'success';
            }
            if (Number(value) >= 0 && Number(value) <= 5) {
              type = 'warning';
            }
            if (Number(value) > 5) {
              type = 'error';
            }
            return h(NText, { type: type }, { default: () => `${value}` })
          } else {
            if (column.key == 'SECURITY_SHORT_NAME') {
              return h(NButton, {
                type: 'info', bordered: false, size: 'small', onClick: () => {
                  openCenteredWindow(`https://quote.eastmoney.com/${rowData.MARKET_SHORT_NAME}${rowData.SECURITY_CODE}.html#fullScreenChart`, 1240, 700)
                }
              }, { default: () => `${value}` })
            } else {
              return h(NText, { type: 'info' }, { default: () => `${value}` })
            }
          }
        }" />
      
      <div v-if="dataList.length > 0" style="margin-top: 5px">
        共找到 <n-tag type="info" :bordered="false">{{ dataList.length }}</n-tag> 只股票
      </div>
    </n-gi>
  </n-grid>
</template>

<style scoped></style>
