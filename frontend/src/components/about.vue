<script setup>
// import { MdPreview } from 'md-editor-v3';
// preview.css相比style.css少了编辑器那部分样式
import 'md-editor-v3/lib/preview.css';
import {h, onBeforeUnmount, onMounted, ref} from 'vue';
import {CheckUpdate, GetVersionInfo,GetSponsorInfo,OpenURL} from "../api/api";

// 替换 Wails 运行为浏览器兼容的实现
import { EventsOff, EventsOn } from "../api/api";

import { getVipPackages } from "../api/vip";
import { useUserStore } from "../store/user";

// 模拟 Environment 函数
const Environment = () => {
  return Promise.resolve({ platform: 'browser' });
};
import {NAvatar, NButton, useNotification} from "naive-ui";
import { format } from 'date-fns';
const updateLog = ref('');
const versionInfo = ref('');
const icon = ref('/appicon.png');
const wxContact = ref('/wx_contact_qr.png');
const notify = useNotification();
const userStore = useUserStore();
const vipLevel = ref("");
const vipStartTime = ref("");
const vipEndTime = ref("");
const expired = ref(false);
const vipPackages = ref([]);

onMounted(() => {
  document.title = '关于软件';
  GetVersionInfo().then((res) => {
    updateLog.value = res.content;
    versionInfo.value = res.version;
    if (res.icon) icon.value = res.icon;

    if (userStore.isLoggedIn.value) {
      GetSponsorInfo().then((res) => {
        vipLevel.value = res.vipLevel;
        vipStartTime.value = res.vipStartTime;
        vipEndTime.value = res.vipEndTime;
        if (res.vipLevel && res.vipEndTime < format(new Date(), 'yyyy-MM-dd HH:mm:ss')) {
          notify.warning({content: 'VIP已到期'});
          expired.value = true;
        }
      }).catch(() => {});
    }

  });

  getVipPackages().then((res) => {
    vipPackages.value = Array.isArray(res) ? res : [];
  }).catch(() => {
    vipPackages.value = [];
  });



})
onBeforeUnmount(() => {
  notify.destroyAll()
  EventsOff("updateVersion")
})

EventsOn("updateVersion",async (msg) => {
  const githubTimeStr = msg.published_at;
  // 创建一个 Date 对象
  const utcDate = new Date(githubTimeStr);
// 获取本地时间
  const date = new Date(utcDate.getTime());
  const year = date.getFullYear();
// getMonth 返回值是 0 - 11，所以要加 1
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  const seconds = String(date.getSeconds()).padStart(2, '0');

  const formattedDate = `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;

  //console.log("GitHub UTC 时间:", utcDate);
  //console.log("转换后的本地时间:", formattedDate);
  notify.info({
    avatar: () =>
        h(NAvatar, {
          size: 'small',
          round: false,
          src: icon.value
        }),
    title: '发现新版本: ' + msg.tag_name,
    content: () => {
      //return h(MdPreview, {theme:'dark',modelValue:msg.commit?.message}, null)
      return h('div', {
        style: {
          'text-align': 'left',
          'font-size': '14px',
        }
      }, { default: () => msg.commit?.message })
    },
    duration: 5000,
    meta: "发布时间:"+formattedDate,
    action: () => {
      return h(NButton, {
        type: 'primary',
        size: 'small',
        onClick: () => {
          Environment().then(env => {
            switch (env.platform) {
              case 'windows':
                window.open(msg.html_url)
                break
              default :
                OpenURL(msg.html_url)
                break
            }
          })
        }
      }, { default: () => '查看' })
    }
  })
})

</script>

<template>
      <n-space vertical size="large"  style="--wails-draggable:no-drag">
        <!-- 软件描述 -->
        <n-card size="large">
          <n-divider title-placement="center">关于软件</n-divider>
          <n-space vertical >
            <n-image width="100" :src="icon" />
            <h1>
              <n-badge v-if="!vipLevel"  :value="versionInfo" :offset="[50,10]"  type="success">
                <n-gradient-text type="info" :size="50" >天梯</n-gradient-text>
              </n-badge>
              <n-badge v-if="vipLevel"  :value="versionInfo" :offset="[50,10]"  type="success">
                <n-gradient-text :type="expired?'error':'warning'" :size="50" >天梯</n-gradient-text><n-tag :bordered="false" size="small" type="warning">VIP{{vipLevel}}</n-tag>
              </n-badge>
            </h1>
            <n-gradient-text  :type="expired?'error':'warning'" v-if="vipLevel" >vip到期时间：{{vipEndTime}}</n-gradient-text>
            <n-button size="tiny" @click="CheckUpdate(1)"  type="info" tertiary >检查更新</n-button>
            <div style="justify-self: center;text-align: left" >
              <p>自选股行情实时监控，基于Electron和NaiveUI构建的AI赋能股票分析工具</p>
              <p>
                本软件基于
                <a href="https://github.com/ArvinLovegood/go-stock" target="_blank">https://github.com/ArvinLovegood/go-stock</a>
                改写而来。
              </p>
              <p>目前已支持A股，港股，美股，未来计划加入基金，ETF等支持</p>
              <p>支持DeepSeek，OpenAI， Ollama，LMStudio，AnythingLLM，<a href="https://cloud.siliconflow.cn/i/foufCerk" target="_blank">硅基流动</a>，<a href="https://www.volcengine.com/experience/ark?utm_term=202502dsinvite&ac=DSASUQY5&rc=IJSE43PZ" target="_blank">火山方舟</a>，阿里云百炼等平台或模型</p>
              <p>
                <i style="color: crimson">本软件仅供学习研究目的，AI分析结果仅供参考，本软件不提供任何投资建议或决策，风险自担！</i>
              </p>
              <p>
                欢迎Star Gitee：<a href="https://gitee.com/gengyunzhongyue/a-stocks_all" target="_blank">a-stocks_all</a><n-divider vertical />
                <a href="https://gitee.com/gengyunzhongyue/a-stocks_all/issues" target="_blank">Issues</a><n-divider vertical />
                <a href="https://gitee.com/gengyunzhongyue/a-stocks_all/releases" target="_blank">Releases</a><n-divider vertical />
              </p>
              <p v-if="updateLog">更新说明：{{updateLog}}</p>
              <p>QQ交流群：<a href="https://qm.qq.com/q/1079549426" target="_blank">1079549426（天梯技术交流群）</a></p>
            </div>
          </n-space>
          <n-divider title-placement="center">支持💕开源</n-divider>
          <n-flex justify="center">
            <n-table  size="small" style="width: 820px">
              <n-thead>
                <n-tr>
                  <n-th>赞助计划</n-th>
                  <n-th>赞助等级</n-th>
                  <n-th>权益说明</n-th>
                </n-tr>
              </n-thead>
              <n-tbody>
                <n-tr v-if="!vipPackages.length">
                  <n-td>请前往 VIP 页面查看</n-td>
                  <n-td>VIP 会员体系</n-td>
                  <n-td>当前套餐以系统配置为准，包含 VIP 专属功能与持续更新权益。</n-td>
                </n-tr>
                <n-tr v-for="pkg in vipPackages" :key="pkg.id">
                  <n-td>¥{{ pkg.price }} / {{ pkg.durationDays }} 天</n-td>
                  <n-td>{{ pkg.packageName }}</n-td>
                  <n-td>{{ pkg.description || '以系统套餐说明为准' }}</n-td>
                </n-tr>
              </n-tbody>
            </n-table>
          </n-flex>
          <n-divider title-placement="center">关于作者</n-divider>
          <n-space vertical>
            <h2><a href="https://gitee.com/gengyunzhongyue" target="_blank">天梯（spark）</a></h2>
            <p>一个热爱编程的小白，欢迎加群交流 :)</p>
          </n-space>
          <n-divider title-placement="center">鸣谢</n-divider>
          <div style="justify-self: center;text-align: left" >
            <p>
              感谢以下捐赠者（持续更新）：
              <n-gradient-text size="small" type="warning">虚位以待...</n-gradient-text>
            </p>
            <p>
              感谢以下开源项目：
              <a href="https://github.com/ArvinLovegood/go-stock" target="_blank">go-stock</a><n-divider vertical />
              <a href="https://github.com/vuejs" target="_blank">Vue</a><n-divider vertical />
              <a href="https://github.com/tusen-ai/naive-ui" target="_blank">NaiveUI</a><n-divider vertical />
              <a href="https://www.electronjs.org" target="_blank">Electron</a><n-divider vertical />
            </p>
          </div>
          <n-divider title-placement="center">关于版权和技术支持申明</n-divider>
          <div style="justify-self: center;text-align: left" >
            <p style="color: #FAA04A">如有问题，请先查看项目文档，如果问题依然存在，请优先加群（<a href="https://qm.qq.com/q/1079549426" target="_blank">1079549426</a>）咨询。</p>
            <p>
              如需软件商业授权或定制开发，请联系作者微信（备注：商业咨询），或QQ：1750632126
            </p>
            <n-divider/>
            <p>
              本软件基于开源技术构建，使用Electron、NaiveUI、Vue等开源项目。技术上如有问题，可以先向对应的开源社区请求帮助。
            </p>
            <p>
              开源不易，本人精力和时间有限，如确实需要一对一技术支持，联系微信或QQ：1750632126（备注：技术支持）
            </p>
            <p style="color: #FAA04A">*加微信或者QQ时，请先备注或留言需求(如：<a href="#support">技术支持</a>，功能建议，商业咨询等，否则会被忽略)</p>
            <n-table id="support">
              <n-thead>
                <n-tr>
                  <n-th>技术支持方式</n-th><n-th>赞助(元)</n-th>
                </n-tr>
              </n-thead>
              <n-tbody>
                <n-tr>
                  <n-td>
                    QQ：1750632126 / 微信扫码添加好友
                  </n-td>
                  <n-td>
                    100/次
                  </n-td>
                </n-tr>
                <n-tr>
                  <n-td>
                    长期技术支持（不限次数，新功能优先体验等）
                  </n-td>
                  <n-td>
                    5000
                  </n-td>
                </n-tr>
              </n-tbody>
            </n-table>
          </div>

          <n-divider title-placement="center">联系作者</n-divider>
          <n-flex justify="center">
            <div style="text-align:center">
              <n-image width="260" :src="wxContact" />
              <p style="margin-top:8px;color:#666">微信扫码加好友</p>
              <p style="color:#999;font-size:12px">QQ：1750632126</p>
            </div>
          </n-flex>

        </n-card>
      </n-space>
</template>

<style scoped>
/* 可以在这里添加一些样式 */
h1, h2 {
  margin: 0;
  padding: 6px 0;
}

p {
  margin: 2px 0;
}

ul {
  list-style-type: disc;
  padding-left: 20px;
}

a {
  color: #18a058;
  text-decoration: none;
}

a:hover {
  text-decoration: underline;
}
</style>
