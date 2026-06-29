/**
 * WebSocket 连接管理工具
 * 用于实时聊天消息推送
 */

let socketTask = null
let isConnected = false
let reconnectTimer = null
let heartbeatTimer = null
let reconnectCount = 0
const MAX_RECONNECT_COUNT = 5
const RECONNECT_INTERVAL = 3000
const HEARTBEAT_INTERVAL = 30000

// 消息监听器
const messageListeners = []
const openListeners = []
const closeListeners = []
const errorListeners = []

/**
 * 连接 WebSocket
 * @param {string} url WebSocket 地址
 * @param {string} token 用户 token
 * @param {boolean} forceReconnect 是否强制重连（忽略重连次数限制），默认 false
 */
export function connectWebSocket(url, token, forceReconnect = false) {
  if (socketTask && isConnected) {
    console.log('WebSocket 已连接')
    return
  }

  // 如果达到最大重连次数且不是强制重连，不再尝试连接
  if (!forceReconnect && reconnectCount >= MAX_RECONNECT_COUNT) {
    console.warn(`WebSocket 重连次数已达上限（${MAX_RECONNECT_COUNT}次），停止重连。`)
    console.warn('提示：如果后端没有 WebSocket 服务，这是正常的。应用会使用 HTTP API 进行消息收发。')
    console.warn('如需重新连接，请先调用 closeWebSocket() 重置状态，然后再调用 connectWebSocket()')
    return
  }

  // 关闭旧连接（不重置计数，保持重连状态）
  if (socketTask) {
    closeWebSocket(false)
  }
  
  // 如果是强制重连，重置计数
  if (forceReconnect) {
    reconnectCount = 0
  }

  const wsUrl = url + '?token=' + token
  console.log('连接 WebSocket:', wsUrl)

  socketTask = uni.connectSocket({
    url: wsUrl,
    success: () => {
      console.log('WebSocket 连接中...')
    },
    fail: (err) => {
      console.error('WebSocket 连接失败:', err)
      notifyErrorListeners(err)
    }
  })

  // 监听连接打开
  socketTask.onOpen(() => {
    console.log('WebSocket 连接成功')
    isConnected = true
    reconnectCount = 0
    notifyOpenListeners()
    startHeartbeat()
  })

  // 监听消息
  socketTask.onMessage((res) => {
    try {
      const data = JSON.parse(res.data)
      console.log('收到 WebSocket 消息:', data)
      notifyMessageListeners(data)
    } catch (e) {
      console.error('解析 WebSocket 消息失败:', e)
    }
  })

  // 监听连接关闭
  socketTask.onClose((res) => {
    console.log('WebSocket 连接关闭', res)
    isConnected = false
    stopHeartbeat()
    notifyCloseListeners()
    
    // 检查关闭码：1000 表示正常关闭，其他表示异常关闭
    const closeCode = res.code || res.statusCode || 0
    
    // 只有在非正常关闭且未达到最大重连次数时才重连
    if (closeCode !== 1000 && reconnectCount < MAX_RECONNECT_COUNT) {
      reconnectTimer = setTimeout(() => {
        reconnectCount++
        console.log(`尝试重连 WebSocket (${reconnectCount}/${MAX_RECONNECT_COUNT})`)
        connectWebSocket(url, token)
      }, RECONNECT_INTERVAL)
    } else {
      // 达到最大重连次数或正常关闭，停止重连
      // 注意：警告信息在 connectWebSocket 函数开始处已经显示，这里不需要重复
      // 不重置计数，保持已达到上限的状态，避免无限重连
    }
  })

  // 监听错误
  socketTask.onError((err) => {
    console.error('WebSocket 错误:', err)
    isConnected = false
    notifyErrorListeners(err)
    // 错误时不要立即重连，等待 onClose 事件处理
  })
}

/**
 * 发送 WebSocket 消息
 * @param {Object} data 消息数据
 */
export function sendWebSocketMessage(data) {
  if (!socketTask || !isConnected) {
    console.warn('WebSocket 未连接，无法发送消息')
    return false
  }

  try {
    const message = JSON.stringify(data)
    socketTask.send({
      data: message,
      success: () => {
        console.log('发送消息成功:', data)
      },
      fail: (err) => {
        console.error('发送消息失败:', err)
      }
    })
    return true
  } catch (e) {
    console.error('发送消息异常:', e)
    return false
  }
}

/**
 * 关闭 WebSocket 连接
 * @param {boolean} resetReconnectCount 是否重置重连计数，默认 true
 */
export function closeWebSocket(resetReconnectCount = true) {
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
  stopHeartbeat()
  
  if (socketTask) {
    socketTask.close({
      code: 1000, // 正常关闭
      success: () => {
        console.log('WebSocket 已关闭')
      }
    })
    socketTask = null
  }
  isConnected = false
  if (resetReconnectCount) {
    reconnectCount = 0
  }
}

/**
 * 启动心跳
 */
function startHeartbeat() {
  stopHeartbeat()
  heartbeatTimer = setInterval(() => {
    if (isConnected) {
      sendWebSocketMessage({
        type: 'ping',
        timestamp: Date.now()
      })
    }
  }, HEARTBEAT_INTERVAL)
}

/**
 * 停止心跳
 */
function stopHeartbeat() {
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer)
    heartbeatTimer = null
  }
}

/**
 * 添加消息监听器
 */
export function onWebSocketMessage(callback) {
  if (typeof callback === 'function') {
    messageListeners.push(callback)
  }
}

/**
 * 移除消息监听器
 */
export function offWebSocketMessage(callback) {
  const index = messageListeners.indexOf(callback)
  if (index > -1) {
    messageListeners.splice(index, 1)
  }
}

/**
 * 添加连接打开监听器
 */
export function onWebSocketOpen(callback) {
  if (typeof callback === 'function') {
    openListeners.push(callback)
  }
}

/**
 * 添加连接关闭监听器
 */
export function onWebSocketClose(callback) {
  if (typeof callback === 'function') {
    closeListeners.push(callback)
  }
}

/**
 * 添加错误监听器
 */
export function onWebSocketError(callback) {
  if (typeof callback === 'function') {
    errorListeners.push(callback)
  }
}

/**
 * 通知消息监听器
 */
function notifyMessageListeners(data) {
  messageListeners.forEach(callback => {
    try {
      callback(data)
    } catch (e) {
      console.error('消息监听器执行错误:', e)
    }
  })
}

/**
 * 通知连接打开监听器
 */
function notifyOpenListeners() {
  openListeners.forEach(callback => {
    try {
      callback()
    } catch (e) {
      console.error('连接打开监听器执行错误:', e)
    }
  })
}

/**
 * 通知连接关闭监听器
 */
function notifyCloseListeners() {
  closeListeners.forEach(callback => {
    try {
      callback()
    } catch (e) {
      console.error('连接关闭监听器执行错误:', e)
    }
  })
}

/**
 * 通知错误监听器
 */
function notifyErrorListeners(err) {
  errorListeners.forEach(callback => {
    try {
      callback(err)
    } catch (e) {
      console.error('错误监听器执行错误:', e)
    }
  })
}

/**
 * 获取连接状态
 */
export function isWebSocketConnected() {
  return isConnected
}

