<script setup>
import { onBeforeMount, ref } from 'vue'
import { GetStockResearchReport, GetStockList } from "../api/api";
import { RefreshCircleSharp, SearchOutline } from "@vicons/ionicons5";
import KLineChart from "./KLineChart.vue";
import { useMessage, NCard, NAutoComplete, NTable, NThead, NTbody, NTr, NTh, NTd, NTag, NText, NA, NPopover, NFlex, NIcon, NEllipsis, NButton } from "naive-ui";

const { stockCode } = defineProps({
  stockCode: {
    type: String,
    default: ''
  }
})

const message = useMessage()
const list = ref([])
const options = ref([])
const searchValue = ref('')

function getStockResearchReport(value) {
  GetStockResearchReport(value || '').then(result => {
    list.value = result || []
  }).catch(err => {
    console.error('获取个股研报失败:', err)
    list.value = []
  })
}

onBeforeMount(() => {
  getStockResearchReport(stockCode);
})

function ratingChangeName(ratingChange) {
  if (ratingChange === 0) return '调高'
  else if (ratingChange === 1) return '调低'
  else if (ratingChange === 2) return '首次'
  else if (ratingChange === 3) return '维持'
  else if (ratingChange === 4) return '无变化'
  else return ''
}

function getmMarketCode(market, code) {
  if (market === "SHENZHEN") return "sz" + code
  else if (market === "SHANGHAI") return "sh" + code
  else if (market === "BEIJING") return "bj" + code
  else if (market === "HONGKONG") return "hk" + code
  else return code
}

function openWin(code) {
  // 使用当前时间戳
  const timestamp = Date.now()
  window.open("https://pdf.dfcfw.com/pdf/H3_" + code + "_1.pdf?" + timestamp + ".pdf", '_blank')
}

function handleInput(value) {
  searchValue.value = value
  if (value && value.length >= 1) {
    GetStockList(value).then(result => {
      options.value = (result || []).map(item => ({
        label: item.name + " - " + item.ts_code,
        value: item.ts_code
      }))
    })
  } else {
    options.value = []
  }
}

function handleSelect(value) {
  searchValue.value = value
  const code = value ? value.replace(/^(sh|sz|bj|hk)/i, '') : ''
  getStockResearchReport(code)
}

function handleSearch() {
  const code = searchValue.value ? searchValue.value.replace(/^(sh|sz|bj|hk)/i, '') : ''
  getStockResearchReport(code)
}

function handleKeydown(e) {
  if (e.key === 'Enter') {
    handleSearch()
  }
}
</script>

<template>
  <n-card>
    <n-flex>
      <n-auto-complete :value="searchValue" :options="options" placeholder="请输入A股名称或者代码" clearable 
        style="flex: 1;" @update:value="handleInput" @select="handleSelect" @keydown="handleKeydown" />
      <n-button type="primary" @click="handleSearch">
        <template #icon><n-icon :component="SearchOutline" /></template>
        搜索
      </n-button>
    </n-flex>
  </n-card>
  <n-table striped size="small">
    <n-thead>
      <n-tr>
        <n-th>名称</n-th>
        <n-th>行业</n-th>
        <n-th>标题</n-th>
        <n-th>东财评级</n-th>
        <n-th>评级变动</n-th>
        <n-th>机构评级</n-th>
        <n-th>分析师</n-th>
        <n-th>机构</n-th>
        <n-th>
          <n-flex justify="space-between">日期<n-icon @click="() => getStockResearchReport('')" color="#409EFF" :size="20"
              :component="RefreshCircleSharp" /></n-flex>
        </n-th>
      </n-tr>
    </n-thead>
    <n-tbody>
      <n-tr v-for="item in list" :key="item.infoCode">
        <n-td :title="item.stockCode">
          <n-popover trigger="hover" placement="right">
            <template #trigger>
              <n-tag type="info" :bordered="false">{{ item.stockName }}</n-tag>
            </template>
            <k-line-chart style="width: 800px" :code="getmMarketCode(item.market, item.stockCode)" :chart-height="500"
              :name="item.stockName" :k-days="20" :dark-theme="true"></k-line-chart>
          </n-popover>
        </n-td>
        <n-td>
          <n-tag type="info" :bordered="false">{{ item.indvInduName }}</n-tag>
        </n-td>
        <n-td>
          <n-a type="info" @click="openWin(item.infoCode)">{{ item.title }}</n-a>
        </n-td>
        <n-td>
          <n-text :type="item.emRatingName === '增持' ? 'error' : 'info'">{{ item.emRatingName }}</n-text>
        </n-td>
        <n-td>
          <n-text :type="item.ratingChange === 0 ? 'error' : 'info'">{{ ratingChangeName(item.ratingChange) }}</n-text>
        </n-td>
        <n-td>{{ item.sRatingName }}</n-td>
        <n-td>{{ item.researcher }}</n-td>
        <n-td>{{ item.orgSName }}</n-td>
        <n-td>{{ item.publishDate?.substring(0, 10) }}</n-td>
      </n-tr>
    </n-tbody>
  </n-table>
</template>

<style scoped></style>
