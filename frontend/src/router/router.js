import {createMemoryHistory, createRouter, createWebHashHistory, createWebHistory} from 'vue-router'

import stockView from '../components/stock.vue'
import settingsView from '../components/settings.vue'
import aboutView from "../components/about.vue";
import fundView from "../components/fund.vue";
import marketView from "../components/market.vue";
import agentChat from "../components/agent-chat.vue"
import thsLongTigerView from "../components/ThsLongTiger.vue"
import wordDictionaryView from "../components/WordDictionary.vue"
import menuManageView from "../components/MenuManage.vue"
import tagManageView from "../components/TagManage.vue"
import stockTagDailyView from "../components/StockTagDaily.vue"

const routes = [
    { path: '/', component: stockView,name: 'stock'},
    { path: '/fund', component: fundView,name: 'fund' },
    { path: '/settings', component: settingsView,name: 'settings' },
    { path: '/about', component: aboutView,name: 'about' },
    { path: '/market', component: marketView,name: 'market' },
    { path: '/agent', component: agentChat,name: 'agent' },
    { path: '/ths-longtiger', component: thsLongTigerView, name: 'thsLongTiger' },
    { path: '/word-dictionary', component: wordDictionaryView, name: 'wordDictionary' },
    { path: '/menu-manage', component: menuManageView, name: 'menuManage' },
    { path: '/tag-manage', component: tagManageView, name: 'tagManage' },
    { path: '/stock-tag-daily', component: stockTagDailyView, name: 'stockTagDaily' },
]

const router = createRouter({
    //history: createWebHistory(),
    history: createWebHashHistory(),
    routes,
})

export default router