let socketTask = null
let isConnected = false
let reconnectCount = 0
let heartbeatTimer = null
let pongTimeoutTimer = null
let reconnectTimer = null

const MAX_RECONNECT = 5
const RECONNECT_INTERVAL = 3000
const HEARTBEAT_INTERVAL = 30000
const PONG_TIMEOUT = 10000

const messageListeners = []
const openListeners = []
const closeListeners = []
const errorListeners = []

export function connectWebSocket(url, token, forceReconnect = false) {
  if (isConnected && socketTask) return

  if (!forceReconnect && reconnectCount >= MAX_RECONNECT) {
    console.warn('[WS] 已达最大重连次数，停止重连')
    return
  }

  if (socketTask) {
    closeWebSocket(false)
  }

  if (forceReconnect) {
    reconnectCount = 0
  }

  const wsUrl = `${url}?token=${token}`
  console.log('[WS] 连接:', wsUrl)

  socketTask = uni.connectSocket({
    url: wsUrl,
    fail: (err) => {
      console.error('[WS] 连接失败:', err)
      notifyErrorListeners(err)
    },
  })

  socketTask.onOpen(() => {
    console.log('[WS] 连接成功')
    isConnected = true
    reconnectCount = 0
    notifyOpenListeners()
    startHeartbeat()
  })

  socketTask.onMessage((res) => {
    try {
      const data = JSON.parse(res.data)
      if (data.type === 'pong') {
        clearPongTimeout()
      }
      notifyMessageListeners(data)
    } catch (e) {
      console.error('[WS] 消息解析失败:', e)
    }
  })

  socketTask.onClose((res) => {
    console.log('[WS] 连接关闭:', res)
    isConnected = false
    stopHeartbeat()
    notifyCloseListeners()

    const code = res.code || res.statusCode || 0
    if (code !== 1000 && reconnectCount < MAX_RECONNECT) {
      reconnectTimer = setTimeout(() => {
        reconnectCount++
        console.log(`[WS] 重连 (${reconnectCount}/${MAX_RECONNECT})`)
        connectWebSocket(url, token)
      }, RECONNECT_INTERVAL)
    }
  })

  socketTask.onError((err) => {
    console.error('[WS] 错误:', err)
    isConnected = false
    notifyErrorListeners(err)
  })
}

export function sendWebSocketMessage(data) {
  if (!socketTask || !isConnected) return false
  socketTask.send({
    data: JSON.stringify(data),
    fail: (err) => console.error('[WS] 发送失败:', err),
  })
  return true
}

export function closeWebSocket(resetReconnect = true) {
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
  stopHeartbeat()
  if (socketTask) {
    socketTask.close({ code: 1000 })
    socketTask = null
  }
  isConnected = false
  if (resetReconnect) reconnectCount = 0
}

function startHeartbeat() {
  stopHeartbeat()
  heartbeatTimer = setInterval(() => {
    if (isConnected) {
      sendWebSocketMessage({ type: 'ping', timestamp: Date.now() })
      clearPongTimeout()
      pongTimeoutTimer = setTimeout(() => {
        console.warn('[WS] pong超时，断开重连')
        closeWebSocket(false)
      }, PONG_TIMEOUT)
    }
  }, HEARTBEAT_INTERVAL)
}

function stopHeartbeat() {
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer)
    heartbeatTimer = null
  }
  clearPongTimeout()
}

function clearPongTimeout() {
  if (pongTimeoutTimer) {
    clearTimeout(pongTimeoutTimer)
    pongTimeoutTimer = null
  }
}

export function onWebSocketMessage(cb) {
  if (typeof cb === 'function') messageListeners.push(cb)
}

export function offWebSocketMessage(cb) {
  const i = messageListeners.indexOf(cb)
  if (i > -1) messageListeners.splice(i, 1)
}

export function onWebSocketOpen(cb) {
  if (typeof cb === 'function') openListeners.push(cb)
}

export function onWebSocketClose(cb) {
  if (typeof cb === 'function') closeListeners.push(cb)
}

export function onWebSocketError(cb) {
  if (typeof cb === 'function') errorListeners.push(cb)
}

function notifyMessageListeners(data) {
  messageListeners.forEach((cb) => {
    try { cb(data) } catch (e) { console.error('[WS] 监听器错误:', e) }
  })
}

function notifyOpenListeners() {
  openListeners.forEach((cb) => {
    try { cb() } catch (e) { console.error('[WS] 监听器错误:', e) }
  })
}

function notifyCloseListeners() {
  closeListeners.forEach((cb) => {
    try { cb() } catch (e) { console.error('[WS] 监听器错误:', e) }
  })
}

function notifyErrorListeners(err) {
  errorListeners.forEach((cb) => {
    try { cb(err) } catch (e) { console.error('[WS] 监听器错误:', e) }
  })
}

export function isWebSocketConnected() {
  return isConnected
}
