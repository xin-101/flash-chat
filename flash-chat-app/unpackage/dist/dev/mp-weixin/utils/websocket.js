"use strict";
const common_vendor = require("../common/vendor.js");
let socketTask = null;
let isConnected = false;
let reconnectTimer = null;
let heartbeatTimer = null;
let reconnectCount = 0;
const MAX_RECONNECT_COUNT = 5;
const RECONNECT_INTERVAL = 3e3;
const HEARTBEAT_INTERVAL = 3e4;
const messageListeners = [];
const openListeners = [];
const closeListeners = [];
const errorListeners = [];
function connectWebSocket(url, token, forceReconnect = false) {
  if (socketTask && isConnected) {
    common_vendor.index.__f__("log", "at utils/websocket.js:29", "WebSocket 已连接");
    return;
  }
  if (!forceReconnect && reconnectCount >= MAX_RECONNECT_COUNT) {
    common_vendor.index.__f__("warn", "at utils/websocket.js:35", `WebSocket 重连次数已达上限（${MAX_RECONNECT_COUNT}次），停止重连。`);
    common_vendor.index.__f__("warn", "at utils/websocket.js:36", "提示：如果后端没有 WebSocket 服务，这是正常的。应用会使用 HTTP API 进行消息收发。");
    common_vendor.index.__f__("warn", "at utils/websocket.js:37", "如需重新连接，请先调用 closeWebSocket() 重置状态，然后再调用 connectWebSocket()");
    return;
  }
  if (socketTask) {
    closeWebSocket(false);
  }
  if (forceReconnect) {
    reconnectCount = 0;
  }
  const wsUrl = url + "?token=" + token;
  common_vendor.index.__f__("log", "at utils/websocket.js:52", "连接 WebSocket:", wsUrl);
  socketTask = common_vendor.index.connectSocket({
    url: wsUrl,
    success: () => {
      common_vendor.index.__f__("log", "at utils/websocket.js:57", "WebSocket 连接中...");
    },
    fail: (err) => {
      common_vendor.index.__f__("error", "at utils/websocket.js:60", "WebSocket 连接失败:", err);
      notifyErrorListeners(err);
    }
  });
  socketTask.onOpen(() => {
    common_vendor.index.__f__("log", "at utils/websocket.js:67", "WebSocket 连接成功");
    isConnected = true;
    reconnectCount = 0;
    notifyOpenListeners();
    startHeartbeat();
  });
  socketTask.onMessage((res) => {
    try {
      const data = JSON.parse(res.data);
      common_vendor.index.__f__("log", "at utils/websocket.js:78", "收到 WebSocket 消息:", data);
      notifyMessageListeners(data);
    } catch (e) {
      common_vendor.index.__f__("error", "at utils/websocket.js:81", "解析 WebSocket 消息失败:", e);
    }
  });
  socketTask.onClose((res) => {
    common_vendor.index.__f__("log", "at utils/websocket.js:87", "WebSocket 连接关闭", res);
    isConnected = false;
    stopHeartbeat();
    notifyCloseListeners();
    const closeCode = res.code || res.statusCode || 0;
    if (closeCode !== 1e3 && reconnectCount < MAX_RECONNECT_COUNT) {
      reconnectTimer = setTimeout(() => {
        reconnectCount++;
        common_vendor.index.__f__("log", "at utils/websocket.js:99", `尝试重连 WebSocket (${reconnectCount}/${MAX_RECONNECT_COUNT})`);
        connectWebSocket(url, token);
      }, RECONNECT_INTERVAL);
    }
  });
  socketTask.onError((err) => {
    common_vendor.index.__f__("error", "at utils/websocket.js:111", "WebSocket 错误:", err);
    isConnected = false;
    notifyErrorListeners(err);
  });
}
function sendWebSocketMessage(data) {
  if (!socketTask || !isConnected) {
    common_vendor.index.__f__("warn", "at utils/websocket.js:124", "WebSocket 未连接，无法发送消息");
    return false;
  }
  try {
    const message = JSON.stringify(data);
    socketTask.send({
      data: message,
      success: () => {
        common_vendor.index.__f__("log", "at utils/websocket.js:133", "发送消息成功:", data);
      },
      fail: (err) => {
        common_vendor.index.__f__("error", "at utils/websocket.js:136", "发送消息失败:", err);
      }
    });
    return true;
  } catch (e) {
    common_vendor.index.__f__("error", "at utils/websocket.js:141", "发送消息异常:", e);
    return false;
  }
}
function closeWebSocket(resetReconnectCount = true) {
  if (reconnectTimer) {
    clearTimeout(reconnectTimer);
    reconnectTimer = null;
  }
  stopHeartbeat();
  if (socketTask) {
    socketTask.close({
      code: 1e3,
      // 正常关闭
      success: () => {
        common_vendor.index.__f__("log", "at utils/websocket.js:161", "WebSocket 已关闭");
      }
    });
    socketTask = null;
  }
  isConnected = false;
  if (resetReconnectCount) {
    reconnectCount = 0;
  }
}
function startHeartbeat() {
  stopHeartbeat();
  heartbeatTimer = setInterval(() => {
    if (isConnected) {
      sendWebSocketMessage({
        type: "ping",
        timestamp: Date.now()
      });
    }
  }, HEARTBEAT_INTERVAL);
}
function stopHeartbeat() {
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer);
    heartbeatTimer = null;
  }
}
function onWebSocketMessage(callback) {
  if (typeof callback === "function") {
    messageListeners.push(callback);
  }
}
function offWebSocketMessage(callback) {
  const index = messageListeners.indexOf(callback);
  if (index > -1) {
    messageListeners.splice(index, 1);
  }
}
function notifyMessageListeners(data) {
  messageListeners.forEach((callback) => {
    try {
      callback(data);
    } catch (e) {
      common_vendor.index.__f__("error", "at utils/websocket.js:251", "消息监听器执行错误:", e);
    }
  });
}
function notifyOpenListeners() {
  openListeners.forEach((callback) => {
    try {
      callback();
    } catch (e) {
      common_vendor.index.__f__("error", "at utils/websocket.js:264", "连接打开监听器执行错误:", e);
    }
  });
}
function notifyCloseListeners() {
  closeListeners.forEach((callback) => {
    try {
      callback();
    } catch (e) {
      common_vendor.index.__f__("error", "at utils/websocket.js:277", "连接关闭监听器执行错误:", e);
    }
  });
}
function notifyErrorListeners(err) {
  errorListeners.forEach((callback) => {
    try {
      callback(err);
    } catch (e) {
      common_vendor.index.__f__("error", "at utils/websocket.js:290", "错误监听器执行错误:", e);
    }
  });
}
exports.closeWebSocket = closeWebSocket;
exports.connectWebSocket = connectWebSocket;
exports.offWebSocketMessage = offWebSocketMessage;
exports.onWebSocketMessage = onWebSocketMessage;
exports.sendWebSocketMessage = sendWebSocketMessage;
//# sourceMappingURL=../../.sourcemap/mp-weixin/utils/websocket.js.map
