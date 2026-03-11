<template>
  <div class="popularity-container">
    <iframe 
      src="/company/人气榜.html"
      frameborder="0"
      class="full-iframe"
    ></iframe>
  </div>
</template>

<script setup>
// 直接嵌入人气榜.html
</script>

<style scoped>
.popularity-container {
  width: 100%;
  height: calc(100vh - 80px);
  background: #000;
}

.full-iframe {
  width: 100%;
  height: 100%;
  border: none;
  background: #000;
}
</style>
