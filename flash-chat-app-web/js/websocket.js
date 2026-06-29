const ChatWebSocket = {
  _ws: null,
  _reconnectCount: 0,
  _heartbeatTimer: null,
  _reconnectTimer: null,
  _messageListeners: [],
  _openListeners: [],
  _closeListeners: [],

  MAX_RECONNECT: 5,
  RECONNECT_INTERVAL: 3000,
  HEARTBEAT_INTERVAL: 30000,

  connect(token) {
    if (this._ws && this._ws.readyState === WebSocket.OPEN) return;
    if (this._reconnectCount >= this.MAX_RECONNECT) return;

    const url = `${AppConfig.wsUrl}/chat/ws?token=${encodeURIComponent(token)}`;
    console.log('[WS] connecting:', url);
    this._ws = new WebSocket(url);

    this._ws.onopen = () => {
      this._reconnectCount = 0;
      this._startHeartbeat();
      this._notifyListeners(this._openListeners);
    };

    this._ws.onmessage = (e) => {
      try {
        const data = JSON.parse(e.data);
        this._notifyListeners(this._messageListeners, data);
      } catch (err) {}
    };

    this._ws.onclose = (e) => {
      this._stopHeartbeat();
      this._notifyListeners(this._closeListeners);
      if (e.code !== 1000 && this._reconnectCount < this.MAX_RECONNECT) {
        this._reconnectTimer = setTimeout(() => {
          this._reconnectCount++;
          this.connect(token);
        }, this.RECONNECT_INTERVAL);
      }
    };

    this._ws.onerror = () => {};
  },

  close() {
    this._stopHeartbeat();
    if (this._reconnectTimer) { clearTimeout(this._reconnectTimer); this._reconnectTimer = null; }
    if (this._ws) { this._ws.close({ code: 1000 }); this._ws = null; }
    this._reconnectCount = 0;
  },

  send(data) {
    if (this._ws && this._ws.readyState === WebSocket.OPEN) {
      this._ws.send(JSON.stringify(data));
      return true;
    }
    return false;
  },

  _startHeartbeat() {
    this._stopHeartbeat();
    this._heartbeatTimer = setInterval(() => {
      this.send({ type: 'ping', timestamp: Date.now() });
    }, this.HEARTBEAT_INTERVAL);
  },

  _stopHeartbeat() {
    if (this._heartbeatTimer) { clearInterval(this._heartbeatTimer); this._heartbeatTimer = null; }
  },

  onMessage(cb) { if (typeof cb === 'function') this._messageListeners.push(cb); },
  offMessage(cb) { const i = this._messageListeners.indexOf(cb); if (i > -1) this._messageListeners.splice(i, 1); },
  onOpen(cb) { if (typeof cb === 'function') this._openListeners.push(cb); },
  onClose(cb) { if (typeof cb === 'function') this._closeListeners.push(cb); },

  _notifyListeners(listeners, ...args) {
    listeners.forEach(cb => { try { cb(...args); } catch (e) {} });
  },
};
