<script setup>
import {h, onBeforeMount, onBeforeUnmount, onMounted, ref} from "vue";
import {RouterLink, useRouter} from 'vue-router'
import {createDiscreteApi,darkTheme,lightTheme , NIcon, NText,NButton,dateZhCN,zhCN, NCard, NConfigProvider, NDialogProvider, NFlex, NGi, NGrid, NMenu, NMessageProvider, NModalProvider, NNotificationProvider, NScrollbar, NSkeleton, NSpin, NTag, NWatermark} from 'naive-ui'
import {
  AlarmOutline,
  AnalyticsOutline,
  BarChartSharp, Bonfire, BonfireOutline, EaselSharp,
  ExpandOutline, Flag,
  Flame, FlameSharp, InformationOutline,
  LogoGithub,
  NewspaperOutline,
  NewspaperSharp, Notifications,
  PowerOutline, Pulse,
  ReorderTwoOutline,
  SettingsOutline, Skull, SkullOutline, SkullSharp,
  SparklesOutline,
  StarOutline,
  Wallet, WarningOutline,
  LayersOutline,
  CalendarOutline,
  SwapHorizontalOutline,
  GitNetworkOutline,
  FlashOutline,
  TrendingUpOutline,
  PeopleOutline,
  StatsChartOutline,
} from '@vicons/ionicons5'
import {
  Dragon, Fire, FirefoxBrowser, Gripfire, Robot
} from "@vicons/fa";
import {ReportSearch} from "@vicons/tabler";
import {LocalFireDepartmentRound} from "@vicons/material";
import {BoxSearch20Regular, CommentNote20Filled} from "@vicons/fluent";
import {FireFilled, FireOutlined, NotificationFilled, StockOutlined} from "@vicons/antd";
import {
  GetConfig, GetGroupList,GetVersionInfo
} from "./api/api";

// 模拟 Wails 事件系统
const EventsEmit = (event, data) => {
  // 模拟事件发送
  console.log(`Event emitted: ${event}`, data);
};

const EventsOff = (event) => {
  // 模拟事件取消监听
  console.log(`Event listener removed: ${event}`);
};

const EventsOn = (event, callback) => {
  // 模拟事件监听 - 在实际应用中，你可能需要实现一个真正的事件系统
  console.log(`Event listener added: ${event}`);
  // 这里暂时不实现具体的事件监听逻辑，因为这取决于实际的后端API实现
};

// 模拟窗口操作函数
const Quit = () => {
  console.log('Quit function called');
};

const WindowFullscreen = () => {
  console.log('WindowFullscreen function called');
};

const WindowUnfullscreen = () => {
  console.log('WindowUnfullscreen function called');
};

const WindowSetTitle = (title) => {
  document.title = title;
};

const WindowHide = () => {
  console.log('WindowHide function called');
};

const router = useRouter()
const loading = ref(true)
const loadingMsg = ref("加载数据中...")
const enableNews = ref(false)
const contentStyle = ref("")
const enableFund = ref(false)
const enableAgent = ref(false)
const enableDarkTheme = ref(null)
const content = ref('未经授权,禁止商业目的!\n\n数据来源于网络,仅供参考;投资有风险,入市需谨慎')
const isFullscreen = ref(false)
const activeKey = ref('stock')
const containerRef = ref({})
const realtimeProfit = ref(0)
const telegraph = ref([])
const groupList = ref([])
const officialStatement= ref("")
const menuVisibility = ref({}) // 菜单可见性配置

// 获取菜单可见性配置
const fetchMenuConfig = async () => {
  try {
    const response = await fetch('/api/menu/list')
    const data = await response.json()
    if (data.success && data.list) {
      const visibility = {}
      // 从list中获取所有菜单的可见性配置
      for (const menu of data.list) {
        visibility[menu.menuKey] = menu.isVisible
      }
      menuVisibility.value = visibility
      console.log('菜单可见性配置:', visibility)
      updateMenuVisibility()
    }
  } catch (error) {
    console.error('获取菜单配置失败:', error)
  }
}

// 更新菜单可见性
const updateMenuVisibility = () => {
  const updateShow = (items) => {
    for (let i = 0; i < items.length; i++) {
      const item = items[i]
      if (item.key && menuVisibility.value.hasOwnProperty(item.key)) {
        items[i] = { ...item, show: menuVisibility.value[item.key] }
        console.log('设置菜单', item.key, '可见性为', menuVisibility.value[item.key])
      }
      if (item.children) {
        updateShow(item.children)
      }
    }
  }
  updateShow(menuOptions.value)
  // 强制触发响应式更新
  menuOptions.value = [...menuOptions.value]
}

const menuOptions = ref([
  {
    label: () =>
        h(
            RouterLink,
            {
              to: {
                name: 'stock',
                query: {
                  groupName: '全部',
                  groupId: 0,
                },
                params: {},
              },
              onClick: () => {
                activeKey.value = 'stock'
              },
            },
            {default: () => '股票自选',}
        ),
    key: 'stock',
    icon: renderIcon(StarOutline),
    show: true,
    children: [
      {
        label: () =>
            h(
                'a',
                {
                  href: '#',
                  type: 'info',
                  onClick: () => {
                    activeKey.value = 'stock'
                    //console.log("push",item)
                    router.push({
                      name: 'stock',
                      query: {
                        groupName: '全部',
                        groupId: 0,
                      },
                    })
                    EventsEmit("changeTab", {ID: 0, name: '全部'})
                  },
                  to: {
                    name: 'stock',
                    query: {
                      groupName: '全部',
                      groupId: 0,
                    },
                  }
                },
                {default: () => '全部',}
            ),
        key: 0,
      }
    ],
  },
  {
    label: () =>
        h(
            RouterLink,
            {
              href: '#',
              to: {
                name: 'market',
                params: {}
              },
              onClick: () => {
                activeKey.value = 'market'
                EventsEmit("changeMarketTab", {ID: 0, name: '市场快讯'})
              },
            },
            {default: () => '市场行情'}
        ),
    key: 'market',
    icon: renderIcon(NewspaperOutline),
    show: true,
    children: [
      {
        label: () =>
            h(
                RouterLink,
                {
                  href: '#',
                  to: {
                    name: 'market',
                    query: {
                      name: "市场快讯",
                    }
                  },
                  onClick: () => {
                    activeKey.value = 'market'
                    EventsEmit("changeMarketTab", {ID: 0, name: '市场快讯'})
                  },
                },
                {default: () => '市场快讯',}
            ),
        key: 'market1',
        icon: renderIcon(NewspaperSharp),
      },
      {
        label: () =>
            h(
                RouterLink,
                {
                  href: '#',
                  to: {
                    name: 'market',
                    query: {
                      name: "全球股指",
                    }
                  },
                  onClick: () => {
                    activeKey.value = 'market'
                    EventsEmit("changeMarketTab", {ID: 0, name: '全球股指'})
                  },
                },
                {default: () => '全球股指',}
            ),
        key: 'market2',
        icon: renderIcon(BarChartSharp),
      },
      {
        label: () =>
            h(
                RouterLink,
                {
                  href: '#',
                  to: {
                    name: 'market',
                    query: {
                      name: "重大指数",
                    }
                  },
                  onClick: () => {
                    activeKey.value = 'market'
                    EventsEmit("changeMarketTab", {ID: 0, name: '重大指数'})
                  },
                },
                {default: () => '重大指数',}
            ),
        key: 'market3',
        icon: renderIcon(AnalyticsOutline),
      },
      {
        label: () =>
            h(
                RouterLink,
                {
                  href: '#',
                  to: {
                    name: 'market',
                    query: {
                      name: "行业排名",
                    }
                  },
                  onClick: () => {
                    activeKey.value = 'market'
                    EventsEmit("changeMarketTab", {ID: 0, name: '行业排名'})
                  },
                },
                {default: () => '行业排名',}
            ),
        key: 'market4',
        icon: renderIcon(Flag),
      },
      {
        label: () =>
            h(
                RouterLink,
                {
                  href: '#',
                  to: {
                    name: 'market',
                    query: {
                      name: "个股资金流向",
                    }
                  },
                  onClick: () => {
                    activeKey.value = 'market'
                    EventsEmit("changeMarketTab", {ID: 0, name: '个股资金流向'})
                  },
                },
                {default: () => '个股资金流向',}
            ),
        key: 'market5',
        icon: renderIcon(Pulse),
        children: [
          {
            label: () => h(RouterLink, {
              href: '#',
              to: { name: 'market', query: { name: "个股资金流向", sort: "netamount" } },
              onClick: () => { activeKey.value = 'market'; EventsEmit("changeMarketTab", {ID: 0, name: '个股资金流向', sort: 'netamount'}) },
            }, {default: () => '净流入额排名'}),
            key: 'market5-1',
          },
          {
            label: () => h(RouterLink, {
              href: '#',
              to: { name: 'market', query: { name: "个股资金流向", sort: "outamount" } },
              onClick: () => { activeKey.value = 'market'; EventsEmit("changeMarketTab", {ID: 0, name: '个股资金流向', sort: 'outamount'}) },
            }, {default: () => '流出资金排名'}),
            key: 'market5-2',
          },
          {
            label: () => h(RouterLink, {
              href: '#',
              to: { name: 'market', query: { name: "个股资金流向", sort: "ratioamount" } },
              onClick: () => { activeKey.value = 'market'; EventsEmit("changeMarketTab", {ID: 0, name: '个股资金流向', sort: 'ratioamount'}) },
            }, {default: () => '净流入率排名'}),
            key: 'market5-3',
          },
          {
            label: () => h(RouterLink, {
              href: '#',
              to: { name: 'market', query: { name: "个股资金流向", sort: "r0_net" } },
              onClick: () => { activeKey.value = 'market'; EventsEmit("changeMarketTab", {ID: 0, name: '个股资金流向', sort: 'r0_net'}) },
            }, {default: () => '主力净流入额排名'}),
            key: 'market5-4',
          },
          {
            label: () => h(RouterLink, {
              href: '#',
              to: { name: 'market', query: { name: "个股资金流向", sort: "r0_out" } },
              onClick: () => { activeKey.value = 'market'; EventsEmit("changeMarketTab", {ID: 0, name: '个股资金流向', sort: 'r0_out'}) },
            }, {default: () => '主力流出排名'}),
            key: 'market5-5',
          },
          {
            label: () => h(RouterLink, {
              href: '#',
              to: { name: 'market', query: { name: "个股资金流向", sort: "r0_ratio" } },
              onClick: () => { activeKey.value = 'market'; EventsEmit("changeMarketTab", {ID: 0, name: '个股资金流向', sort: 'r0_ratio'}) },
            }, {default: () => '主力净流入率排名'}),
            key: 'market5-6',
          },
          {
            label: () => h(RouterLink, {
              href: '#',
              to: { name: 'market', query: { name: "个股资金流向", sort: "r3_net" } },
              onClick: () => { activeKey.value = 'market'; EventsEmit("changeMarketTab", {ID: 0, name: '个股资金流向', sort: 'r3_net'}) },
            }, {default: () => '散户净流入额排名'}),
            key: 'market5-7',
          },
          {
            label: () => h(RouterLink, {
              href: '#',
              to: { name: 'market', query: { name: "个股资金流向", sort: "r3_out" } },
              onClick: () => { activeKey.value = 'market'; EventsEmit("changeMarketTab", {ID: 0, name: '个股资金流向', sort: 'r3_out'}) },
            }, {default: () => '散户流出排名'}),
            key: 'market5-8',
          },
          {
            label: () => h(RouterLink, {
              href: '#',
              to: { name: 'market', query: { name: "个股资金流向", sort: "r3_ratio" } },
              onClick: () => { activeKey.value = 'market'; EventsEmit("changeMarketTab", {ID: 0, name: '个股资金流向', sort: 'r3_ratio'}) },
            }, {default: () => '散户净流入率排名'}),
            key: 'market5-9',
          },
        ]
      },
      {
        label: () =>
            h(
                RouterLink,
                {
                  href: '#',
                  to: {
                    name: 'market',
                    query: {
                      name: "龙虎榜",
                    }
                  },
                  onClick: () => {
                    activeKey.value = 'market'
                    EventsEmit("changeMarketTab", {ID: 0, name: '龙虎榜'})
                  },
                },
                {default: () => '龙虎榜',}
            ),
        key: 'market6',
        icon: renderIcon(Dragon),
      },
      {
        label: () =>
            h(
                RouterLink,
                {
                  href: '#',
                  to: {
                    name: 'market',
                    query: {
                      name: "个股研报",
                    }
                  },
                  onClick: () => {
                    activeKey.value = 'market'
                    EventsEmit("changeMarketTab", {ID: 0, name: '个股研报'})
                  },
                },
                {default: () => '个股研报',}
            ),
        key: 'market7',
        icon: renderIcon(StockOutlined),
      },
      {
        label: () =>
            h(
                RouterLink,
                {
                  href: '#',
                  to: {
                    name: 'market',
                    query: {
                      name: "公司公告",
                    }
                  },
                  onClick: () => {
                    activeKey.value = 'market'
                    EventsEmit("changeMarketTab", {ID: 0, name: '公司公告'})
                  },
                },
                {default: () => '公司公告',}
            ),
        key: 'market8',
        icon: renderIcon(NotificationFilled),
      },
      {
        label: () =>
            h(
                RouterLink,
                {
                  href: '#',
                  to: {
                    name: 'market',
                    query: {
                      name: "行业研究",
                    }
                  },
                  onClick: () => {
                    activeKey.value = 'market'
                    EventsEmit("changeMarketTab", {ID: 0, name: '行业研究'})
                  },
                },
                {default: () => '行业研究',}
            ),
        key: 'market9',
        icon: renderIcon(ReportSearch),
      },
      {
        label: () =>
            h(
                RouterLink,
                {
                  href: '#',
                  to: {
                    name: 'market',
                    query: {
                      name: "当前热门",
                    }
                  },
                  onClick: () => {
                    activeKey.value = 'market'
                    EventsEmit("changeMarketTab", {ID: 0, name: '当前热门'})
                  },
                },
                {default: () => '当前热门',}
            ),
        key: 'market10',
        icon: renderIcon(Gripfire),
      },
      {
        label: () =>
            h(
                RouterLink,
                {
                  href: '#',
                  to: {
                    name: 'market',
                    query: {
                      name: "指标选股",
                    }
                  },
                  onClick: () => {
                    activeKey.value = 'market'
                    EventsEmit("changeMarketTab", {ID: 0, name: '指标选股'})
                  },
                },
                {default: () => '指标选股',}
            ),
        key: 'market11',
        icon: renderIcon(BoxSearch20Regular),
      },
      {
        label: () =>
            h(
                RouterLink,
                {
                  href: '#',
                  to: {
                    name: 'market',
                    query: {
                      name: "名站优选",
                    }
                  },
                  onClick: () => {
                    activeKey.value = 'market'
                    EventsEmit("changeMarketTab", {ID: 0, name: '名站优选'})
                  },
                },
                {default: () => '名站优选',}
            ),
        key: 'market12',
        icon: renderIcon(FirefoxBrowser),
      },
    ]
  },
  
  {
    label: () =>
        h(
            RouterLink,
            {
              to: {
                name: 'thsLongTiger',
              },
              onClick: () => {
                activeKey.value = 'thsLongTiger'
              },
            },
            {default: () => '龙虎榜',}
        ),
    key: 'thsLongTiger',
    icon: renderIcon(FlashOutline),
    show: true,
  },
  {
    label: () =>
        h(
            RouterLink,
            {
              to: {
                name: 'fund',
                query: {
                  name: '基金自选',
                },
              },
              onClick: () => {
                activeKey.value = 'fund'
              },
            },
            {default: () => '基金自选',}
        ),
    show: enableFund.value,
    key: 'fund',
    icon: renderIcon(SparklesOutline),
    children: [
      {
        label: () => h(NText, {type: realtimeProfit.value > 0 ? 'error' : 'success'}, {default: () => '功能完善中！'}),
        key: 'realtimeProfit',
        show: realtimeProfit.value,
        icon: renderIcon(AlarmOutline),
      },
    ]
  },
  {
    label: () =>
        h(
            RouterLink,
            {
              to: {
                name: 'agent',
                query: {
                  name:"Ai智能体",
                },
              },
              onClick: () => {
                activeKey.value = 'agent'
              },
            },
            {default: () => 'Ai智能体'}
        ),
    key: 'agent',
    show:enableAgent.value,
    icon: renderIcon(Robot),
  },
  {
    label: () =>
        h(
            'span',
            {},
            {default: () => '系统管理'}
        ),
    key: 'system',
    icon: renderIcon(SettingsOutline),
    show: true,
    children: [
      {
        label: () =>
            h(
                RouterLink,
                {
                  to: {
                    name: 'menuManage',
                  },
                  onClick: () => {
                    activeKey.value = 'menuManage'
                  },
                },
                {default: () => '菜单管理',}
            ),
        key: 'menuManage',
        icon: renderIcon(ReorderTwoOutline),
      },
      {
        label: () =>
            h(
                RouterLink,
                {
                  to: {
                    name: 'tagManage',
                  },
                  onClick: () => {
                    activeKey.value = 'tagManage'
                  },
                },
                {default: () => '标签管理',}
            ),
        key: 'tagManage',
        icon: renderIcon(BonfireOutline),
      },
      {
        label: () =>
            h(
                RouterLink,
                {
                  to: {
                    name: 'stockTagDaily',
                  },
                  onClick: () => {
                    activeKey.value = 'stockTagDaily'
                  },
                },
                {default: () => '每日热门&打标',}
            ),
        key: 'stockTagDaily',
        icon: renderIcon(FlameSharp),
      },
      {
        label: () =>
            h(
                RouterLink,
                {
                  to: {
                    name: 'wordDictionary',
                  },
                  onClick: () => {
                    activeKey.value = 'wordDictionary'
                  },
                },
                {default: () => '词典管理',}
            ),
        key: 'wordDictionary',
        icon: renderIcon(CommentNote20Filled),
      },
      {
        label: () =>
            h(
                RouterLink,
                {
                  to: {
                    name: 'settings',
                    query: {
                      name:"设置",
                    },
                  },
                  onClick: () => {
                    activeKey.value = 'settings'
                  },
                },
                {default: () => '设置'}
            ),
        key: 'settings',
        icon: renderIcon(SettingsOutline),
      },
      {
        label: () =>
            h(
                RouterLink,
                {
                  to: {
                    name: 'about',
                    query: {
                      name:"关于",
                    }
                  },
                  onClick: () => {
                    activeKey.value = 'about'
                  },
                },
                {default: () => '关于'}
            ),
        key: 'about',
        icon: renderIcon(LogoGithub),
      },
    ]
  },
  {
    show:false,
    label: () => h("a", {
      href: '#',
      onClick: toggleFullscreen,
      title: '全屏 Ctrl+F 退出全屏 Esc',
    }, {default: () => isFullscreen.value ? '取消全屏' : '全屏'}),
    key: 'full',
    icon: renderIcon(ExpandOutline),
  },
  {
    label: () => h("a", {
      href: '#',
      onClick: WindowHide,
      title: '隐藏到托盘区 Ctrl+Z',
    }, {default: () => '隐藏到托盘区'}),
    key: 'hide',
    icon: renderIcon(ReorderTwoOutline),
  },
  // {
  //   label: ()=> h("a", {
  //     href: 'javascript:void(0)',
  //     style: 'cursor: move;',
  //     onClick: toggleStartMoveWindow,
  //   }, { default: () => '移动' }),
  //   key: 'move',
  //   icon: renderIcon(MoveOutline),
  // },
  {
    label: () => h("a", {
      href: '#',
      onClick: Quit,
    }, {default: () => '退出程序'}),
    key: 'exit',
    icon: renderIcon(PowerOutline),
  },
])

function renderIcon(icon) {
  return () => h(NIcon, null, {default: () => h(icon)})
}

function toggleFullscreen(e) {
  activeKey.value = 'full'
  //console.log(e)
  if (isFullscreen.value) {
    // WindowUnfullscreen()
    //e.target.innerHTML = '全屏'
  } else {
    // WindowFullscreen()
    // e.target.innerHTML = '取消全屏'
  }
  isFullscreen.value = !isFullscreen.value
}

// 简化事件处理，避免在浏览器环境中使用 Wails 特定功能
const eventListeners = {};

// EventsOn("realtime_profit", (data) => {
//   realtimeProfit.value = data
// })
// EventsOn("telegraph", (data) => {
//   telegraph.value = data
// })

// EventsOn("loadingMsg", (data) => {
//   if(data==="done"){
//     loadingMsg.value = "加载完成..."
//     EventsEmit("loadingDone", "app")
//     loading.value  = false
//   }else{
//     loading.value  = true
//     loadingMsg.value = data
//   }
// })

onBeforeUnmount(() => {
  // EventsOff("realtime_profit")
  // EventsOff("loadingMsg")
  // EventsOff("telegraph")
  // EventsOff("newsPush")
})

window.onerror = function (msg, source, lineno, colno, error) {
  // 将错误信息发送给后端
  // EventsEmit("frontendError", {
  //   page: "App.vue",
  //   message: msg,
  //   source: source,
  //   lineno: lineno,
  //   colno: colno,
  //   error: error ? error.stack : null,
  // });
  console.error('Frontend Error:', msg, source, lineno, colno, error);
  return true;
};

onBeforeMount(() => {
  // 确保在组件挂载前获取必要数据
  Promise.all([
    GetVersionInfo().catch(err => {
      console.error('Error getting version info:', err);
      return { officialStatement: null };
    }),
    GetGroupList().catch(err => {
      console.error('Error getting group list:', err);
      return [];
    }),
    GetConfig().catch(err => {
      console.error('Error getting config:', err);
      return { enableFund: false, enableAgent: false, darkTheme: false };
    }),
    fetchMenuConfig().catch(err => {
      console.error('Error getting menu config:', err);
    })
  ])
  .then(([versionInfo, groupListData, configData]) => {
    // 处理版本信息
    if(versionInfo && versionInfo.officialStatement){
      content.value = versionInfo.officialStatement+"\n\n"+content.value
      officialStatement.value = versionInfo.officialStatement
    }
    
    // 处理分组列表
    groupList.value = groupListData || [];
    menuOptions.value.forEach((item) => {
      if (item.key === 'stock') {
        if (groupList.value && groupList.value.length > 0) {
          item.children.push(...groupList.value.map(item => {
            return {
              label: () =>
                  h(
                      'a',
                      {
                        href: '#',
                        type: 'info',
                        onClick: () => {
                          //console.log("push",item)
                          router.push({
                            name: 'stock',
                            query: {
                              groupName: item.name,
                              groupId: item.ID,
                            },
                          })
                          setTimeout(() => {
                            EventsEmit("changeTab", item)
                          }, 100)
                        },
                        to: {
                          name: 'stock',
                          query: {
                            groupName: item.name,
                            groupId: item.ID,
                          },
                        }
                      },
                      {default: () => item.name,}
                  ),
              key: item.ID,
            }
          }))
        }
      }
    })

    // 处理配置
    if (configData) {
      enableFund.value = configData.enableFund || false
      enableAgent.value = configData.enableAgent || false

      // 注意：fund和agent的显示由菜单配置控制，这里不再覆盖

      if (configData.darkTheme) {
        enableDarkTheme.value = darkTheme
      } else {
        enableDarkTheme.value = null
      }
    }
  })
  .catch(err => {
    console.error('Error in onBeforeMount:', err);
    // 即使出现错误，也要确保加载状态更新
    loading.value = false;
  })
  .finally(() => {
    // 确保即使API调用失败，加载状态也会更新
    if (loading.value) {
      loading.value = false;
    }
  });
})

onMounted(() => {
  WindowSetTitle("go-stock：AI赋能股票分析✨ "+officialStatement.value+"  未经授权,禁止商业目的！ [数据来源于网络,仅供参考;投资有风险,入市需谨慎]")
  contentStyle.value = "max-height: calc(92vh);overflow: hidden"
  
  GetConfig().then((res) => {
    if (res && res.enableNews) {
      enableNews.value = true
    }
    if (res) {
      enableFund.value = res.enableFund
      enableAgent.value = res.enableAgent
    }
    
    // 更新菜单选项以反映配置更改
    menuOptions.value.forEach((item) => {
      if (item.key === 'fund') {
        item.show = enableFund.value
      }
      if (item.key === 'agent') {
        item.show = enableAgent.value
      }
    })
    
    const {notification } =createDiscreteApi(["notification"], {
      configProviderProps: {
        theme: enableDarkTheme.value ? darkTheme : lightTheme ,
        max: 3,
      },
    })
    // EventsOn("newsPush", (data) => {
    //   //console.log(data)
    //   if(data.isRed){
    //     notification.create({
    //       //type:"error",
    //      // avatar: () => h(NIcon,{component:Notifications,color:"red"}),
    //       title: data.time,
    //       content: () => h('div',{type:"error",style:{
    //           "text-align":"left",
    //           "font-size":"14px",
    //           "color":"#f67979"
    //         }}, { default: () => data.content }),
    //       meta: () => h(NText,{type:"warning"}, { default: () => data.source}),
    //       duration:1000*40,
    //     })
    //   }else{
    //      notification.create({
    //       //type:"info",
    //       //avatar: () => h(NIcon,{component:Notifications}),
    //       title: data.time,
    //       content: () => h('div',{type:"info",style:{
    //         "text-align":"left",
    //           "font-size":"14px",
    //           "color": data.source==="go-stock"?"#F98C24":"#549EC8"
    //         }}, { default: () => data.content }),
    //       meta: () => h(NText,{type:"warning"}, { default: () => data.source}),
    //       duration:1000*30 ,
    //     })
    //   }
    // })
  }).catch(err => {
    console.error('Error getting config in onMounted:', err);
  })
})
</script>
<template>
  <n-config-provider ref="containerRef" :theme="enableDarkTheme" :locale="zhCN" :date-locale="dateZhCN">
    <n-message-provider>
      <n-notification-provider>
        <n-modal-provider>
          <n-dialog-provider>
            <n-watermark
                :content="''"
                cross
                selectable
                :font-size="16"
                :line-height="16"
                :width="500"
                :height="400"
                :x-offset="50"
                :y-offset="150"
                :rotate="-15"
            >
              <n-flex>
                <n-grid x-gap="12" :cols="1">
                  <n-gi>
                    <n-spin :show="loading">
                      <template #description>
                        {{ loadingMsg }}
                      </template>
                      <n-marquee :speed="100" style="position: relative;top:0;z-index: 19;width: 100%"
                                 v-if="(telegraph.length>0)&&(enableNews)">
                        <n-tag type="warning" v-for="item in telegraph" style="margin-right: 10px">
                          {{ item }}
                        </n-tag>
                      </n-marquee>
                      <n-scrollbar :style="contentStyle">
                        <n-skeleton v-if="loading" height="calc(100vh)" />
                        <RouterView v-else/>
                      </n-scrollbar>
                    </n-spin>
                  </n-gi>
                  <n-gi style="position: fixed;bottom:0;z-index: 9;width: 100%;">
                    <n-card size="small" style="--wails-draggable:no-drag">
                      <n-menu style="font-size: 18px;"
                              v-model:value="activeKey"
                              mode="horizontal"
                              :options="menuOptions"
                              responsive
                      />
                    </n-card>
                  </n-gi>
                </n-grid>
              </n-flex>
            </n-watermark>
          </n-dialog-provider>
        </n-modal-provider>
      </n-notification-provider>
    </n-message-provider>
  </n-config-provider>
</template>
<style>

</style>