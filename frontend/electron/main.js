const { app, BrowserWindow, shell } = require('electron')
const path = require('path')
const { spawn } = require('child_process')

let mainWindow
let javaProcess

// 启动 Java 后端
function startJavaBackend() {
  const jarPath = path.join(__dirname, '../../java-stock-backend/target/java-stock-backend-1.0-SNAPSHOT.jar')
  
  // 检查是否在开发模式
  const isDev = process.env.NODE_ENV === 'development'
  
  if (!isDev) {
    // 生产模式：启动打包好的 jar
    javaProcess = spawn('java', ['-jar', jarPath], {
      cwd: path.join(__dirname, '../..'),
      stdio: 'inherit'
    })
    
    javaProcess.on('error', (err) => {
      console.error('Failed to start Java backend:', err)
    })
  }
}

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1400,
    height: 900,
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true
    },
    icon: path.join(__dirname, '../src/assets/images/logo-universal.png')
  })

  // 开发模式连接 Vite 开发服务器，生产模式加载打包后的文件
  const isDev = process.env.NODE_ENV === 'development'
  
  if (isDev) {
    const devServerUrl = process.env.VITE_DEV_SERVER_URL || 'http://localhost:5178'
    mainWindow.loadURL(devServerUrl)
    mainWindow.webContents.openDevTools()
  } else {
    mainWindow.loadFile(path.join(__dirname, '../dist/index.html'))
  }

  // 外部链接在默认浏览器打开
  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    shell.openExternal(url)
    return { action: 'deny' }
  })

  mainWindow.on('closed', () => {
    mainWindow = null
  })
}

app.whenReady().then(() => {
  startJavaBackend()
  
  // 等待后端启动
  setTimeout(createWindow, 2000)
  
  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow()
    }
  })
})

app.on('window-all-closed', () => {
  // 关闭 Java 后端
  if (javaProcess) {
    javaProcess.kill()
  }
  
  if (process.platform !== 'darwin') {
    app.quit()
  }
})
