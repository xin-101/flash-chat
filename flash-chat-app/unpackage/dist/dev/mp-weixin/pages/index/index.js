"use strict";
const common_vendor = require("../../common/vendor.js");
const common_assets = require("../../common/assets.js");
const api_chat = require("../../api/chat.js");
const utils_storage = require("../../utils/storage.js");
const utils_websocket = require("../../utils/websocket.js");
if (!Array) {
  const _component_u_icon = common_vendor.resolveComponent("u-icon");
  const _component_u_loading_icon = common_vendor.resolveComponent("u-loading-icon");
  (_component_u_icon + _component_u_loading_icon)();
}
const _sfc_main = {
  __name: "index",
  setup(__props) {
    const chatList = common_vendor.ref([]);
    const loading = common_vendor.ref(false);
    const currentUser = common_vendor.ref(null);
    async function loadChatList() {
      loading.value = true;
      try {
        const res = await api_chat.getChatList();
        if (res && res.data) {
          chatList.value = Array.isArray(res.data) ? res.data : [];
        } else {
          chatList.value = [];
        }
      } catch (e) {
        common_vendor.index.__f__("error", "at pages/index/index.vue:85", "获取会话列表失败:", e);
        chatList.value = [];
        common_vendor.index.showToast({
          title: e.message || "获取会话列表失败",
          icon: "none"
        });
      } finally {
        loading.value = false;
      }
    }
    function getAvatar(item) {
      if (item.face) {
        return item.face;
      }
      if (item.targetUser && item.targetUser.face) {
        return item.targetUser.face;
      }
      return "/static/default-avatar.png";
    }
    function getNickname(item) {
      if (item.nickname) {
        return item.nickname;
      }
      if (item.targetUser && item.targetUser.nickname) {
        return item.targetUser.nickname;
      }
      return "未知用户";
    }
    function getLastMessage(item) {
      if (item.lastMessage) {
        return item.lastMessage;
      }
      if (item.lastMessageContent) {
        return item.lastMessageContent;
      }
      return "暂无消息";
    }
    function formatTime(time) {
      if (!time)
        return "";
      const date = new Date(time);
      const now = /* @__PURE__ */ new Date();
      const diff = now - date;
      if (diff < 24 * 60 * 60 * 1e3 && date.getDate() === now.getDate()) {
        const hours = date.getHours().toString().padStart(2, "0");
        const minutes = date.getMinutes().toString().padStart(2, "0");
        return `${hours}:${minutes}`;
      }
      if (diff < 48 * 60 * 60 * 1e3 && date.getDate() === now.getDate() - 1) {
        return "昨天";
      }
      if (diff < 7 * 24 * 60 * 60 * 1e3) {
        const weekdays = ["日", "一", "二", "三", "四", "五", "六"];
        return `周${weekdays[date.getDay()]}`;
      }
      const month = (date.getMonth() + 1).toString().padStart(2, "0");
      const day = date.getDate().toString().padStart(2, "0");
      return `${month}-${day}`;
    }
    function goToChat(item) {
      const conversationId = item.id || item.conversationId;
      if (!conversationId) {
        common_vendor.index.showToast({
          title: "会话ID不存在",
          icon: "none"
        });
        return;
      }
      common_vendor.index.navigateTo({
        url: `/pages/chat/detail?conversationId=${conversationId}&targetUserId=${item.targetUserId || ""}&nickname=${encodeURIComponent(getNickname(item))}&avatar=${encodeURIComponent(getAvatar(item))}`
      });
    }
    function goToSearch() {
      common_vendor.index.navigateTo({
        url: "/pages/user/search"
      });
    }
    function handleWebSocketMessage(data) {
      common_vendor.index.__f__("log", "at pages/index/index.vue:188", "收到WebSocket消息:", data);
      if (data.type === "message" || data.type === "new_message") {
        loadChatList();
      }
    }
    common_vendor.onMounted(() => {
      currentUser.value = utils_storage.getUserInfo();
      loadChatList();
      utils_websocket.onWebSocketMessage(handleWebSocketMessage);
    });
    common_vendor.onShow(() => {
      loadChatList();
    });
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.p({
          name: "more-dot-fill",
          size: "22",
          color: "#333"
        }),
        b: common_vendor.o(goToSearch),
        c: common_vendor.p({
          name: "scan",
          size: "22",
          color: "#333"
        }),
        d: common_vendor.f(chatList.value, (item, index, i0) => {
          return common_vendor.e({
            a: getAvatar(item),
            b: item.unreadCount > 0
          }, item.unreadCount > 0 ? {} : {}, {
            c: common_vendor.t(getNickname(item)),
            d: common_vendor.t(formatTime(item.lastMessageTime || item.updatedTime)),
            e: common_vendor.t(getLastMessage(item)),
            f: item.unreadCount > 0
          }, item.unreadCount > 0 ? {
            g: common_vendor.t(item.unreadCount > 99 ? "99+" : item.unreadCount)
          } : {}, {
            h: item.id || index,
            i: common_vendor.o(($event) => goToChat(item), item.id || index)
          });
        }),
        e: chatList.value.length === 0 && !loading.value
      }, chatList.value.length === 0 && !loading.value ? {
        f: common_assets._imports_0
      } : {}, {
        g: loading.value
      }, loading.value ? {} : {});
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-1cf27b2a"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/index/index.js.map
