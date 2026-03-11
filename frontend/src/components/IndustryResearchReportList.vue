<script setup>
import { onBeforeMount, ref } from 'vue'
import { GetIndustryResearchReport } from "../api/api";
import { RefreshCircleSharp, SearchOutline } from "@vicons/ionicons5";
import { useMessage, NCard, NInput, NTable, NThead, NTbody, NTr, NTh, NTd, NTag, NText, NA, NFlex, NIcon, NEllipsis, NButton } from "naive-ui";

const message = useMessage()
const list = ref([])
const searchKeyword = ref('')

function getIndustryResearchReport(value) {
  message.loading("正在刷新数据...")
  GetIndustryResearchReport(value || '').then(result => {
    list.value = result || []
  }).catch(err => {
    console.error('获取行业研究报告失败:', err)
    list.value = []
  })
}

onBeforeMount(() => {
  getIndustryResearchReport('');
})

function ratingChangeName(ratingChange) {
  if (ratingChange === 0) return '调高'
  else if (ratingChange === 1) return '调低'
  else if (ratingChange === 2) return '首次'
  else if (ratingChange === 3) return '维持'
  else if (ratingChange === 4) return '无变化'
  else return ''
}

function openWin(code) {
  const timestamp = Date.now()
  window.open("https://pdf.dfcfw.com/pdf/H3_" + code + "_1.pdf?" + timestamp + ".pdf", '_blank')
}

function handleSearch() {
  getIndustryResearchReport(searchKeyword.value)
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
      <n-input v-model:value="searchKeyword" placeholder="请输入行业名称关键词搜索" clearable 
        style="flex: 1;" @keydown="handleKeydown" />
      <n-button type="primary" @click="handleSearch">
        <template #icon><n-icon :component="SearchOutline" /></template>
        搜索
      </n-button>
    </n-flex>
  </n-card>
  <n-table striped size="small">
    <n-thead>
      <n-tr>
        <n-th>行业</n-th>
        <n-th>标题</n-th>
        <n-th>东财评级</n-th>
        <n-th>评级变动</n-th>
        <n-th>机构评级</n-th>
        <n-th>分析师</n-th>
        <n-th>机构</n-th>
        <n-th>
          <n-flex justify="space-between">日期<n-icon @click="() => getIndustryResearchReport('')" color="#409EFF"
              :size="20" :component="RefreshCircleSharp" /></n-flex>
        </n-th>
      </n-tr>
    </n-thead>
    <n-tbody>
      <n-tr v-for="item in list" :key="item.infoCode">
        <n-td>
          <n-tag type="info" :bordered="false">{{ item.industryName }}</n-tag>
        </n-td>
        <n-td>
          <n-a type="info" @click="openWin(item.infoCode)">
            <n-text type="success">{{ item.title }}</n-text>
          </n-a>
        </n-td>
        <n-td>
          <n-text :type="item.emRatingName === '增持' ? 'error' : 'info'">{{ item.emRatingName }}</n-text>
        </n-td>
        <n-td>
          <n-text :type="item.ratingChange === 0 ? 'error' : 'info'">{{ ratingChangeName(item.ratingChange) }}</n-text>
        </n-td>
        <n-td>{{ item.sRatingName }}</n-td>
        <n-td>
          <n-ellipsis style="max-width: 120px">{{ item.researcher }}</n-ellipsis>
        </n-td>
        <n-td>{{ item.orgSName }}</n-td>
        <n-td>{{ item.publishDate?.substring(0, 10) }}</n-td>
      </n-tr>
    </n-tbody>
  </n-table>
</template>

<style scoped></style>
