"use strict";
Object.defineProperty(exports, Symbol.toStringTag, { value: "Module" });
const common_vendor = require("./common/vendor.js");
const utils_websocket = require("./utils/websocket.js");
const utils_storage = require("./utils/storage.js");
const config_env = require("./config/env.js");
if (!Math) {
  "./pages/login/index.js";
  "./pages/index/index.js";
  "./pages/chat/detail.js";
  "./pages/user/search.js";
  "./pages/my/index.js";
}
const _sfc_main = {
  onLaunch: function() {
    common_vendor.index.__f__("log", "at App.vue:8", "App Launch");
    this.checkLoginStatus();
  },
  onShow: function() {
    common_vendor.index.__f__("log", "at App.vue:14", "App Show");
  },
  onHide: function() {
    common_vendor.index.__f__("log", "at App.vue:17", "App Hide");
  },
  onError: function(err) {
    common_vendor.index.__f__("error", "at App.vue:20", "App Error:", err);
  },
  methods: {
    // 检查登录状态
    checkLoginStatus() {
      const token = utils_storage.getToken();
      const userInfo = utils_storage.getUserInfo();
      if (token && userInfo && userInfo.id) {
        setTimeout(() => {
          this.connectWebSocketIfNeeded();
        }, 1e3);
      } else {
        const pages = getCurrentPages();
        if (pages.length === 0 || pages[pages.length - 1].route !== "pages/login/index") {
          common_vendor.index.reLaunch({
            url: "/pages/login/index"
          });
        }
      }
    },
    // 连接WebSocket（如果需要）
    connectWebSocketIfNeeded() {
      const token = utils_storage.getToken();
      const userInfo = utils_storage.getUserInfo();
      if (!token || !userInfo) {
        return;
      }
      const wsUrl = config_env.BASE_URL.replace("http://", "ws://").replace("https://", "wss://") + "/chat/ws";
      utils_websocket.connectWebSocket(wsUrl, token, true);
    }
  }
};
function createApp() {
  const app = common_vendor.createSSRApp(_sfc_main);
  return {
    app
  };
}
createApp().app.mount("#app");
exports.createApp = createApp;
//# sourceMappingURL=../.sourcemap/mp-weixin/app.js.map
