<script setup>
import { onBeforeMount, ref } from 'vue'
import { GetDragonTigerList } from "../api/api";
import { ArrowDownOutline } from "@vicons/ionicons5";
import _ from "lodash";
import KLineChart from "./KLineChart.vue";
import MoneyTrend from "./moneyTrend.vue";
import { NButton, NText, useMessage, NTag, NTable, NThead, NTbody, NTr, NTh, NTd, NForm, NGrid, NFormItemGi, NDatePicker, NSelect, NPopover, NIcon } from "naive-ui";

const message = useMessage()
const lhbList = ref([])
const EXPLANATIONs = ref([])

const today = new Date();
const year = today.getFullYear();
const month = String(today.getMonth() + 1).padStart(2, '0');
const day = String(today.getDate()).padStart(2, '0');
const formattedDate = `${year}-${month}-${day}`;

const SearchForm = ref({
  dateValue: formattedDate,
  EXPLANATION: null,
})

onBeforeMount(() => {
  longTiger(formattedDate);
})

function longTiger(date) {
  if (date) {
    SearchForm.value.dateValue = date;
  }

  let loading1 = message.loading("正在获取龙虎榜数据...", { duration: 0 });

  // 使用 Java 后端 API，后端会自动获取最近交易日数据
  GetDragonTigerList(date || '').then(res => {
    loading1.destroy();
    if (res && res.length > 0) {
      lhbList.value = res;
      EXPLANATIONs.value = _.uniqBy(_.map(lhbList.value, function (item) {
        return {
          label: item['EXPLANATION'],
          value: item['EXPLANATION'],
        };
      }), 'label');
    } else {
      lhbList.value = [];
      EXPLANATIONs.value = [];
      message.info("暂无数据，请切换日期");
    }
  }).catch(err => {
    loading1.destroy();
    message.error("获取数据失败，请重试");
    console.error(err);
  });
}

function handleEXPLANATION(value, option) {
  SearchForm.value.EXPLANATION = value
  if (value) {
    GetDragonTigerList(SearchForm.value.dateValue).then(res => {
      lhbList.value = _.filter(res, function (o) { return o['EXPLANATION'] === value; });
      if (res.length === 0) {
        message.info("暂无数据,请切换日期")
      }
    })
  } else {
    longTiger(SearchForm.value.dateValue)
  }
}
</script>

<template>
  <n-form :model="SearchForm">
    <n-grid :cols="24" :x-gap="24">
      <n-form-item-gi :span="4" label="日期" path="dateValue" label-placement="left">
        <n-date-picker v-model:formatted-value="SearchForm.dateValue" value-format="yyyy-MM-dd" type="date"
          :on-update:value="(v, v2) => longTiger(v2)" />
      </n-form-item-gi>
      <n-form-item-gi :span="8" label="上榜原因" path="EXPLANATION" label-placement="left">
        <n-select clearable placeholder="上榜原因过滤" v-model:value="SearchForm.EXPLANATION" :options="EXPLANATIONs"
          :on-update:value="handleEXPLANATION" />
      </n-form-item-gi>
      <n-form-item-gi :span="10" label="" label-placement="left">
        <n-text type="error">*当天的龙虎榜数据通常在收盘结束后一小时左右更新</n-text>
      </n-form-item-gi>
    </n-grid>
  </n-form>
  <n-table :single-line="false" striped>
    <n-thead>
      <n-tr>
        <n-th>代码</n-th>
        <n-th width="60px">名称</n-th>
        <n-th>收盘价</n-th>
        <n-th width="60px">涨跌幅</n-th>
        <n-th>龙虎榜净买额(万)</n-th>
        <n-th>龙虎榜买入额(万)</n-th>
        <n-th>龙虎榜卖出额(万)</n-th>
        <n-th>龙虎榜成交额(万)</n-th>
        <n-th width="60px" data-field="TURNOVERRATE">换手率<n-icon :component="ArrowDownOutline" /></n-th>
        <n-th>流通市值(亿)</n-th>
        <n-th>上榜原因</n-th>
      </n-tr>
    </n-thead>
    <n-tbody>
      <n-tr v-for="(item, index) in lhbList" :key="index">
        <n-td>
          <n-tag :bordered=false type="info">{{
            item.SECUCODE.split('.')[1].toLowerCase() + item.SECUCODE.split('.')[0] }}</n-tag>
        </n-td>
        <n-td>
          <n-popover trigger="hover" placement="right">
            <template #trigger>
              <n-button tag="a" text :type="item.CHANGE_RATE > 0 ? 'error' : 'success'" :bordered=false>{{
                item.SECURITY_NAME_ABBR }}</n-button>
            </template>
            <k-line-chart style="width: 800px"
              :code="item.SECUCODE.split('.')[1].toLowerCase() + item.SECUCODE.split('.')[0]" :chart-height="500"
              :name="item.SECURITY_NAME_ABBR" :k-days="20" :dark-theme="true"></k-line-chart>
          </n-popover>
        </n-td>
        <n-td>
          <n-text :type="item.CHANGE_RATE > 0 ? 'error' : 'success'">{{ item.CLOSE_PRICE }}</n-text>
        </n-td>
        <n-td>
          <n-text :type="item.CHANGE_RATE > 0 ? 'error' : 'success'">{{ (item.CHANGE_RATE).toFixed(2) }}%</n-text>
        </n-td>
        <n-td>
          <n-popover trigger="hover" placement="right">
            <template #trigger>
              <n-button tag="a" text :type="item.BILLBOARD_NET_AMT > 0 ? 'error' : 'success'" :bordered=false>{{
                (item.BILLBOARD_NET_AMT / 10000).toFixed(2) }}</n-button>
            </template>
            <money-trend :code="item.SECUCODE.split('.')[1].toLowerCase() + item.SECUCODE.split('.')[0]"
              :name="item.SECURITY_NAME_ABBR" :days="360" :dark-theme="true" :chart-height="500"
              style="width: 800px"></money-trend>
          </n-popover>
        </n-td>
        <n-td>
          <n-text :type="'error'">{{ (item.BILLBOARD_BUY_AMT / 10000).toFixed(2) }}</n-text>
        </n-td>
        <n-td>
          <n-text :type="'success'">{{ (item.BILLBOARD_SELL_AMT / 10000).toFixed(2) }}</n-text>
        </n-td>
        <n-td>
          <n-text :type="'info'">{{ (item.BILLBOARD_DEAL_AMT / 10000).toFixed(2) }}</n-text>
        </n-td>
        <n-td>
          <n-text :type="'info'">{{ (item.TURNOVERRATE).toFixed(2) }}%</n-text>
        </n-td>
        <n-td>
          <n-text :type="'info'">{{ (item.FREE_MARKET_CAP / 100000000).toFixed(2) }}</n-text>
        </n-td>
        <n-td>
          <n-text :type="'info'">{{ item.EXPLANATION }}</n-text>
        </n-td>
      </n-tr>
    </n-tbody>
  </n-table>
</template>

<style scoped></style>
