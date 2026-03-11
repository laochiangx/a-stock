// 简单的代理服务器，用于连接前端和后端服务
const express = require('express');
const { createProxyMiddleware } = require('http-proxy-middleware');
const path = require('path');

const app = express();
const PORT = 5173;
const BACKEND_URL = 'http://localhost:8089';

// 设置代理中间件用于 API 请求
app.use('/api', createProxyMiddleware({
  target: BACKEND_URL,
  changeOrigin: true,
  pathRewrite: {
    '^/api': '', // 移除 /api 前缀
  },
}));

// 提供静态文件
app.use(express.static(path.join(__dirname, 'dist')));

// 处理前端路由 - 对于非 API 请求，返回 index.html
app.get(/^(?!\/api).*$/, (req, res) => {
  res.sendFile(path.join(__dirname, 'dist', 'index.html'));
});

app.listen(PORT, () => {
  console.log(`前端代理服务器运行在 http://localhost:${PORT}`);
  console.log(`后端API代理到: ${BACKEND_URL}`);
  console.log('按 Ctrl+C 停止服务器');
});