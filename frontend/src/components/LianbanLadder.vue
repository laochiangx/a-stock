<script setup>
import { ref, onMounted, computed } from 'vue'
import { 
  NCard, NSpace, NButton, NSpin, NEmpty, NScrollbar, NTable, NTag, NFlex, NText, NGrid, NGi
} from 'naive-ui'

const loading = ref(false)
const daysData = ref([]) // 存储多天数据
const selectedStock = ref(null) // 当前选中的股票代码

// 获取最近N个交易日的日期
const getTradingDates = async (count = 10) => {
  const dates = []
  const today = new Date()
  let checkDate = new Date(today)
  
  while (dates.length < count) {
    const dateStr = formatDate(checkDate)
    // 检查是否是交易日（通过API返回数据判断）
    const hasData = await checkTradingDay(dateStr)
    if (hasData) {
      dates.push(dateStr)
    }
    checkDate.setDate(checkDate.getDate() - 1)
    // 防止无限循环，最多检查60天
    if (dates.length === 0 && (today - checkDate) / (1000 * 60 * 60 * 24) > 60) {
      break
    }
  }
  return dates
}

// 检查某天是否有交易数据
const checkTradingDay = async (dateStr) => {
  try {
    const url = `https://push2ex.eastmoney.com/getTopicZTPool?ut=7eea3edcaed734bea9cbfc24409ed989&dpt=wz.ztzt&Pageindex=0&pagesize=10&sort=lbc:desc&date=${dateStr.replace(/-/g, '')}`
    const response = await fetch(url)
    const text = await response.text()
    return !text.includes('"pool":[]') && !text.includes('"data":null')
  } catch (e) {
    return false
  }
}

// 格式化日期
const formatDate = (date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

// 格式化金额
const formatMoney = (num) => {
  if (!num) return '--'
  const n = parseFloat(num)
  if (isNaN(n)) return '--'
  if (Math.abs(n) >= 100000000) {
    return (n / 100000000).toFixed(2) + '亿'
  } else if (Math.abs(n) >= 10000) {
    return (n / 10000).toFixed(0) + '万'
  }
  return n.toFixed(0)
}

// 获取某天的涨停数据
const fetchDayData = async (dateStr) => {
  try {
    const url = `https://push2ex.eastmoney.com/getTopicZTPool?ut=7eea3edcaed734bea9cbfc24409ed989&dpt=wz.ztzt&Pageindex=0&pagesize=1000&sort=lbc:desc&date=${dateStr.replace(/-/g, '')}`
    const response = await fetch(url)
    const text = await response.text()
    
    // 解析JSONP格式数据
    const match = text.match(/"pool":(\[.*?\])/)
    if (match) {
      const pool = JSON.parse(match[1])
      return pool.map(item => ({
        code: item.c,
        name: item.n,
        lbc: item.lbc, // 连板数
        fund: item.fund, // 封单额
        hybk: item.hybk, // 行业板块
        zttj: item.zttj, // 涨停统计
        amount: item.amount, // 成交额
      }))
    }
    return []
  } catch (e) {
    console.error('获取数据失败:', dateStr, e)
    return []
  }
}

// 加载数据
const loadData = async () => {
  loading.value = true
  daysData.value = []
  
  try {
    // 获取最近10个交易日
    const dates = await getTradingDates(10)
    
    // 并行获取所有日期的数据
    const promises = dates.map(async (date) => {
      const stocks = await fetchDayData(date)
      return { date, stocks }
    })
    
    const results = await Promise.all(promises)
    daysData.value = results.filter(r => r.stocks.length > 0)
  } catch (e) {
    console.error('加载数据失败:', e)
  } finally {
    loading.value = false
  }
}

// 刷新数据
const refresh = () => {
  loadData()
}

// 点击股票行
const handleRowClick = (code) => {
  selectedStock.value = code
}

// 检查股票是否在多天出现
const getStockAppearCount = (code) => {
  let count = 0
  daysData.value.forEach(day => {
    if (day.stocks.some(s => s.code === code)) {
      count++
    }
  })
  return count
}

// 获取行样式
const getRowStyle = (code) => {
  const count = getStockAppearCount(code)
  if (selectedStock.value === code) {
    return { backgroundColor: '#483D8B', cursor: 'pointer' }
  }
  if (count >= 5) {
    return { border: '2px solid #FF0000', cursor: 'pointer' }
  }
  if (count >= 4) {
    return { border: '2px solid #FF00FF', cursor: 'pointer' }
  }
  return { cursor: 'pointer' }
}

// 获取连板数颜色
const getLbcColor = (lbc) => {
  if (lbc >= 5) return '#FF0000'
  if (lbc >= 3) return '#FF00FF'
  return '#FFA500'
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="lianban-container">
    <!-- 顶部操作栏 -->
    <n-card size="small" style="margin-bottom: 8px;">
      <n-flex justify="space-between" align="center">
        <n-space>
          <n-text strong style="color: #FF8C00; font-size: 16px;">东财连板天梯</n-text>
          <n-button size="small" @click="refresh" :loading="loading">刷新</n-button>
        </n-space>
        <n-space>
          <n-tag type="info" v-if="daysData.length > 0">
            共 {{ daysData.length }} 个交易日
          </n-tag>
          <n-tag type="warning" size="small">
            红框: 5天+出现 | 紫框: 4天出现
          </n-tag>
        </n-space>
      </n-flex>
    </n-card>

    <!-- 数据展示 -->
    <n-spin :show="loading">
      <div v-if="daysData.length > 0" class="days-container">
        <n-scrollbar x-scrollable>
          <div class="days-flex">
            <div 
              v-for="day in daysData" 
              :key="day.date" 
              class="day-column"
            >
              <div class="day-header">
                <n-tag type="warning" size="small">{{ day.date }}</n-tag>
                <n-text depth="3" style="font-size: 11px; margin-left: 4px;">
                  {{ day.stocks.length }}只
                </n-text>
              </div>
              <n-scrollbar style="max-height: calc(100vh - 220px);">
                <n-table :bordered="false" :single-line="false" size="small" class="stock-table">
                  <thead>
                    <tr>
                      <th style="width: 60px;">名称</th>
                      <th style="width: 40px; color: #FFA500;">连板</th>
                      <th style="width: 50px;">封单</th>
                      <th style="width: 70px;">行业</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr 
                      v-for="stock in day.stocks" 
                      :key="stock.code"
                      :style="getRowStyle(stock.code)"
                      @click="handleRowClick(stock.code)"
                    >
                      <td>
                        <n-text :style="{ color: selectedStock === stock.code ? '#fff' : '#E1FFFF', fontSize: '12px' }">
                          {{ stock.name }}
                        </n-text>
                      </td>
                      <td>
                        <n-text :style="{ color: getLbcColor(stock.lbc), fontWeight: 'bold' }">
                          {{ stock.lbc }}
                        </n-text>
                      </td>
                      <td>
                        <n-text style="color: #E1FFFF; font-size: 11px;">
                          {{ formatMoney(stock.fund) }}
                        </n-text>
                      </td>
                      <td>
                        <n-text style="color: #999; font-size: 11px;" :title="stock.hybk">
                          {{ stock.hybk ? (stock.hybk.length > 4 ? stock.hybk.substring(0, 4) + '..' : stock.hybk) : '--' }}
                        </n-text>
                      </td>
                    </tr>
                  </tbody>
                </n-table>
              </n-scrollbar>
            </div>
          </div>
        </n-scrollbar>
      </div>
      
      <n-empty v-else-if="!loading" description="暂无数据" />
    </n-spin>
  </div>
</template>

<style scoped>
.lianban-container {
  padding: 0;
  height: calc(100vh - 120px);
  background-color: #1a1a1a;
}

.days-container {
  width: 100%;
}

.days-flex {
  display: flex;
  gap: 4px;
  padding: 4px;
}

.day-column {
  min-width: 220px;
  max-width: 220px;
  background-color: #000;
  border: 1px solid #333;
  border-radius: 4px;
  padding: 4px;
}

.day-header {
  text-align: center;
  padding: 4px;
  margin-bottom: 4px;
  border-bottom: 1px solid #333;
}

.stock-table {
  background-color: #000 !important;
}

:deep(.n-table) {
  background-color: #000 !important;
}

:deep(.n-table th) {
  background-color: #1a1a1a !important;
  color: #E1FFFF !important;
  padding: 4px !important;
  font-size: 11px !important;
}

:deep(.n-table td) {
  background-color: #000 !important;
  padding: 3px 4px !important;
  border-color: #333 !important;
}

:deep(.n-table tr:hover td) {
  background-color: #800000 !important;
}

:deep(.n-card) {
  background-color: #1a1a1a;
}
</style>
