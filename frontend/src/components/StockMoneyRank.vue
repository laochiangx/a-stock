<script setup>
import { CaretDown, CaretUp, RefreshCircleOutline } from "@vicons/ionicons5";
import { NText, useMessage, NTable, NThead, NTbody, NTr, NTh, NTd, NTag, NIcon, NButton, NPopover, NCard } from "naive-ui";
import { onBeforeUnmount, onMounted, ref, watch } from "vue";
import { GetMoneyRankSina } from "../api/api";
import KLineChart from "./KLineChart.vue";

const props = defineProps({
  sortType: {
    type: String,
    default: 'netamount'
  }
})

const message = useMessage()
const dataList = ref([])
const sort = ref(props.sortType)
const interval = ref(null)

// 排序字段映射
const sortLabels = {
  'netamount': '净流入额',
  'outamount': '流出资金',
  'ratioamount': '净流入率',
  'r0_net': '主力净流入额',
  'r0_out': '主力流出',
  'r0_ratio': '主力净流入率',
  'r3_net': '散户净流入额',
  'r3_out': '散户流出',
  'r3_ratio': '散户净流入率'
}

onMounted(() => {
  GetRankData()
  interval.value = setInterval(() => {
    GetRankData()
  }, 1000 * 60)
})

onBeforeUnmount(() => {
  clearInterval(interval.value)
})

watch(() => props.sortType, (newSort) => {
  sort.value = newSort
  GetRankData()
})

function GetRankData() {
  message.loading("正在刷新数据...")
  GetMoneyRankSina(sort.value).then(result => {
    if (result && result.length > 0) {
      dataList.value = result
    }
  }).catch(err => {
    console.error('获取个股资金流向排名失败:', err)
  })
}

function changeSort(newSort) {
  sort.value = newSort
  GetRankData()
}
</script>

<template>
  <n-card :title="'个股资金流向排名 - ' + (sortLabels[sort] || sort)" size="small">
    <template #header-extra>
      <n-button size="small" @click="GetRankData" quaternary>
        <template #icon>
          <n-icon :component="RefreshCircleOutline" />
        </template>
        刷新
      </n-button>
    </template>
    <n-table striped size="small">
      <n-thead>
        <n-tr>
          <n-th>股票代码</n-th>
          <n-th>股票名称</n-th>
          <n-th>最新价</n-th>
          <n-th>涨跌幅</n-th>
          <n-th>流入资金/万</n-th>
          <n-th>流出资金/万</n-th>
          <n-th style="cursor: pointer;" @click="changeSort('netamount')">
            净流入/万
            <n-icon v-if="sort === 'netamount'" :component="CaretDown" />
          </n-th>
          <n-th>净流入率</n-th>
          <n-th style="cursor: pointer;" @click="changeSort('r0_net')">
            主力净流入/万
            <n-icon v-if="sort === 'r0_net'" :component="CaretDown" />
          </n-th>
          <n-th>主力净流入率</n-th>
          <n-th style="cursor: pointer;" @click="changeSort('r3_net')">
            散户净流入/万
            <n-icon v-if="sort === 'r3_net'" :component="CaretDown" />
          </n-th>
        </n-tr>
      </n-thead>
      <n-tbody>
        <n-tr v-for="item in dataList" :key="item.symbol">
          <n-td>
            <n-tag :bordered="false" type="info">{{ item.symbol }}</n-tag>
          </n-td>
          <n-td>
            <n-popover trigger="hover" placement="right">
              <template #trigger>
                <n-button tag="a" text :type="item.changeratio > 0 ? 'error' : 'success'" :bordered="false">
                  {{ item.name }}
                </n-button>
              </template>
              <k-line-chart style="width: 800px" :code="item.symbol" :chart-height="500" :name="item.name"
                :k-days="20" :dark-theme="true"></k-line-chart>
            </n-popover>
          </n-td>
          <n-td>
            <n-text type="info">{{ item.trade }}</n-text>
          </n-td>
          <n-td>
            <n-text :type="item.changeratio > 0 ? 'error' : 'success'">
              {{ (item.changeratio * 100).toFixed(2) }}%
            </n-text>
          </n-td>
          <n-td>
            <n-text type="error">{{ (item.inamount / 10000).toFixed(2) }}</n-text>
          </n-td>
          <n-td>
            <n-text type="success">{{ (item.outamount / 10000).toFixed(2) }}</n-text>
          </n-td>
          <n-td>
            <n-text :type="item.netamount > 0 ? 'error' : 'success'">
              {{ (item.netamount / 10000).toFixed(2) }}
            </n-text>
          </n-td>
          <n-td>
            <n-text :type="item.ratioamount > 0 ? 'error' : 'success'">
              {{ (item.ratioamount * 100).toFixed(2) }}%
            </n-text>
          </n-td>
          <n-td>
            <n-text :type="item.r0_net > 0 ? 'error' : 'success'">
              {{ item.r0_net ? (item.r0_net / 10000).toFixed(2) : '-' }}
            </n-text>
          </n-td>
          <n-td>
            <n-text :type="item.r0_ratio > 0 ? 'error' : 'success'">
              {{ item.r0_ratio ? (item.r0_ratio * 100).toFixed(2) + '%' : '-' }}
            </n-text>
          </n-td>
          <n-td>
            <n-text :type="item.r3_net > 0 ? 'error' : 'success'">
              {{ item.r3_net ? (item.r3_net / 10000).toFixed(2) : '-' }}
            </n-text>
          </n-td>
        </n-tr>
      </n-tbody>
    </n-table>
  </n-card>
</template>

<style scoped></style>
