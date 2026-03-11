<template>
  <div>
    <div v-if="loading" style="display: flex; justify-content: center; align-items: center; height: 200px;">
      正在加载资金流向数据...
    </div>
    <div v-else-if="error" style="display: flex; justify-content: center; align-items: center; height: 200px; color: red;">
      {{ error }}
    </div>
    <div v-else ref="LineChartRef" style="width: 100%; height: auto;" :style="{ height: chartHeight + 'px' }"></div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch, reactive, nextTick } from "vue";
import * as echarts from "echarts";
import { GetStockMoneyTrendByDay } from "../api/api";

const { code, name, darkTheme, days, chartHeight } = defineProps({
  code: {
    type: String,
    default: ''
  },
  name: {
    type: String,
    default: ''
  },
  days: {
    type: Number,
    default: 14
  },
  chartHeight: {
    type: Number,
    default: 500
  },
  darkTheme: {
    type: Boolean,
    default: false
  }
});

const LineChartRef = ref(null);
const loading = ref(true);
const error = ref('');

onMounted(() => {
  // 等待下一个 tick，确保 DOM 已渲染再初始化图表
  nextTick(() => handleLine(code, days));
});

// 监听code和days的变化，重新获取数据
watch([() => code, () => days], ([newCode, newDays]) => {
  handleLine(newCode, newDays);
});

const handleLine = async (code, days) => {
  loading.value = true;
  error.value = '';
  try {
    console.log('MoneyTrendHttp: 开始获取数据, code:', code, 'days:', days);
    const result = await GetStockMoneyTrendByDay(code, days);
    console.log('MoneyTrendHttp: API返回的原始数据:', result);
    console.log('MoneyTrendHttp: 数据类型:', typeof result, '是否为数组:', Array.isArray(result));
    
    // 处理可能的响应包装
    let data = result;
    if (result && result.data && Array.isArray(result.data)) {
      data = result.data;
      console.log('MoneyTrendHttp: 从data字段提取数据');
    } else if (Array.isArray(result)) {
      data = result;
      console.log('MoneyTrendHttp: 直接使用数组数据');
    } else {
      data = [];
      console.warn('MoneyTrendHttp: 数据格式不正确');
    }
    
    loading.value = false;

    if (!data || !Array.isArray(data) || data.length === 0) {
      error.value = '暂无数据';
      console.warn('MoneyTrendHttp: 资金流向数据为空, data:', data);
      return;
    }
    
    console.log('MoneyTrendHttp: 数据获取成功，数量:', data.length);
    if (data.length > 0) {
      console.log('MoneyTrendHttp: 第一条数据示例:', data[0]);
    }

    // 等待视图更新后再获取图表容器引用，避免 ref 仍为 null
    await nextTick();

    if (!LineChartRef.value) {
      error.value = '图表元素未找到';
      console.error('Chart element not found');
      return;
    }

    const chart = echarts.init(LineChartRef.value);
    const categoryData = [];
    const netamount_values = [];
    const r0_net_values = [];
    const trades_values = [];
    let volume = [];

    let min = 0;
    let max = 0;

    for (let i = 0; i < data.length; i++) {
      let resultElement = data[i];
      if (!resultElement) {
        console.warn('MoneyTrendHttp: 跳过空数据项，索引:', i);
        continue;
      }
      
      // 处理字段名，Java后端可能返回的字段名可能不同
      const opendate = resultElement.opendate || resultElement.openDate || resultElement.date || '';
      const netamount = resultElement.netamount || resultElement.netAmount || resultElement.net_amount || 0;
      const trade = resultElement.trade || resultElement.price || resultElement.close || 0;
      const r0_net = resultElement.r0_net || resultElement.r0Net || resultElement.r0_net || 0;
      
      categoryData.push(opendate);
      let netamountValue = ((Number(netamount) || 0) / 10000).toFixed(2);
      netamount_values.push(parseFloat(netamountValue));
      let price = Number(trade) || 0;
      trades_values.push(price);
      r0_net_values.push(parseFloat(((Number(r0_net) || 0) / 10000).toFixed(2)));

      if (min === 0 || min > price) {
        min = price;
      }
      if (max < price) {
        max = price;
      }

      // 计算累计净流入（累加所有之前的净流入）
      if (i > 0) {
        // 累计净流入 = 当前净流入 + 前一天的累计净流入
        let prevCumulative = volume[i - 1] * 10000; // 前一天累计净流入（转回原始单位）
        let currentNetamount = Number(netamount) || 0;
        let cumulative = (prevCumulative + currentNetamount) / 10000;
        volume.push(parseFloat(cumulative.toFixed(2)));
      } else {
        // 第一天，累计净流入 = 当日净流入
        volume.push(parseFloat((Number(netamount) / 10000).toFixed(2)));
      }
    }
    
    console.log('MoneyTrendHttp: 数据处理完成, categoryData长度:', categoryData.length);

    const upColor = '#ec0000';
    const downColor = '#00da3c';
    let option = {
      title: {
        text: name,
        left: '20px',
        textStyle: {
          color: darkTheme ? '#ccc' : '#456'
        }
      },
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'cross',
          lineStyle: {
            color: '#376df4',
            width: 1,
            opacity: 1
          }
        },
        borderWidth: 2,
        borderColor: darkTheme ? '#456' : '#ccc',
        backgroundColor: darkTheme ? '#456' : '#fff',
        padding: 10,
        textStyle: {
          color: darkTheme ? '#ccc' : '#456'
        },
      },
      axisPointer: {
        link: [
          {
            xAxisIndex: 'all'
          }
        ],
        label: {
          backgroundColor: '#888'
        }
      },
      legend: {
        show: true,
        data: ['当日净流入', '主力当日净流入', '累计净流入', '股价'],
        selected: {
          '当日净流入': true,
          '主力当日净流入': true,
          '累计净流入': true,
          '股价': true,
        },
        textStyle: {
          color: darkTheme ? 'rgb(253,252,252)' : '#456'
        },
        right: 150,
      },
      dataZoom: [
        {
          type: 'inside',
          xAxisIndex: [0, 1],
          start: 86,
          end: 100
        },
        {
          show: true,
          xAxisIndex: [0, 1],
          type: 'slider',
          top: '90%',
          start: 86,
          end: 100
        }
      ],
      grid: [
        {
          left: '8%',
          right: '8%',
          height: '50%',
        },
        {
          left: '8%',
          right: '8%',
          top: '74%',
          height: '15%'
        },
      ],
      xAxis: [
        {
          type: 'category',
          data: categoryData,
          axisPointer: {
            z: 100
          },
          boundaryGap: false,
          axisLine: { onZero: false },
          splitLine: { show: false },
          min: 'dataMin',
          max: 'dataMax',
        },
        {
          gridIndex: 1,
          type: 'category',
          data: categoryData,
          axisLabel: {
            show: false
          },
        }
      ],
      yAxis: [
        {
          name: '当日净流入/万',
          type: 'value',
          axisLine: {
            show: true
          },
          splitLine: {
            show: false
          },
        },
        {
          name: '股价',
          type: 'value',
          min: min - 1,
          max: max + 1,
          minInterval: 0.01,
          axisLine: {
            show: true
          },
          splitLine: {
            show: false
          },
        },
        {
          gridIndex: 1,
          name: '累计净流入/万',
          type: 'value',
          axisLine: {
            show: true
          },
          splitLine: {
            show: false
          },
        },
      ],
      series: [
        {
          yAxisIndex: 0,
          name: '当日净流入',
          data: netamount_values,
          smooth: false,
          showSymbol: false,
          lineStyle: {
            width: 2
          },
          markPoint: {
            symbol: 'arrow',
            symbolRotate: 90,
            symbolSize: [10, 20],
            symbolOffset: [10, 0],
            itemStyle: {
              color: '#0d7dfc'
            },
            label: {
              position: 'right',
            },
            data: [
              { type: 'max', name: 'Max' },
              { type: 'min', name: 'Min' }
            ]
          },
          markLine: {
            data: [
              {
                type: 'average',
                name: 'Average',
                lineStyle: {
                  color: '#0077ff',
                  width: 0.5
                },
              },
            ]
          },
          type: 'line'
        },
        {
          yAxisIndex: 0,
          name: '主力当日净流入',
          data: r0_net_values,
          smooth: false,
          showSymbol: false,
          lineStyle: {
            width: 2
          },
          type: 'bar'
        },
        {
          yAxisIndex: 1,
          name: '股价',
          type: 'line',
          data: trades_values,
          smooth: true,
          showSymbol: false,
          lineStyle: {
            width: 3
          },
          markPoint: {
            symbol: 'arrow',
            symbolRotate: 90,
            symbolSize: [10, 20],
            symbolOffset: [10, 0],
            itemStyle: {
              color: '#f39509'
            },
            label: {
              position: 'right',
            },
            data: [
              { type: 'max', name: 'Max' },
              { type: 'min', name: 'Min' }
            ]
          },
          markLine: {
            data: [
              {
                type: 'average',
                name: 'Average',
                lineStyle: {
                  color: '#f39509',
                  width: 0.5
                },
              },
            ]
          },
        },
        {
          type: 'bar',
          xAxisIndex: 1,
          yAxisIndex: 2,
          name: '累计净流入',
          data: volume,
          smooth: true,
          showSymbol: false,
          lineStyle: {
            width: 2
          },
          markPoint: {
            symbol: 'arrow',
            symbolRotate: 90,
            symbolSize: [10, 20],
            symbolOffset: [10, 0],
            label: {
              position: 'right',
            },
            data: [
              { type: 'max', name: 'Max' },
              { type: 'min', name: 'Min' }
            ]
          },
        },
      ]
    };

    chart.setOption(option);
    console.log('MoneyTrendHttp: 图表渲染完成');
  } catch (e) {
    loading.value = false;
    console.error('渲染图表失败:', e);
    error.value = '渲染图表失败: ' + (e?.message || '未知错误');
  }
};
</script>

<style scoped>
</style>