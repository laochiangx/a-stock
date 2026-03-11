<script setup>
// 龙头挖掘 - 直接嵌入通达信页面
</script>

<template>
  <div class="leader-container">
    <iframe
      src="http://hot.icfqs.com:7615/site/tdx-pc-find/page_yzfp.html"
      frameborder="0"
      allowfullscreen
    ></iframe>
  </div>
</template>

<style scoped>
.leader-container {
  width: 100%;
  height: calc(100vh - 80px);
  overflow: hidden;
}

.leader-container iframe {
  width: 100%;
  height: 100%;
  border: none;
}
</style>
