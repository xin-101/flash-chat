"use strict";
const common_vendor = require("../../common/vendor.js");
const common_assets = require("../../common/assets.js");
const api_chat = require("../../api/chat.js");
const utils_storage = require("../../utils/storage.js");
const utils_websocket = require("../../utils/websocket.js");
if (!Array) {
  const _component_u_icon = common_vendor.resolveComponent("u-icon");
  const _component_u_loading_icon = common_vendor.resolveComponent("u-loading-icon");
  const _component_u_input = common_vendor.resolveComponent("u-input");
  const _component_u_button = common_vendor.resolveComponent("u-button");
  const _component_u_popup = common_vendor.resolveComponent("u-popup");
  (_component_u_icon + _component_u_loading_icon + _component_u_input + _component_u_button + _component_u_popup)();
}
const pageSize = 20;
const _sfc_main = {
  __name: "detail",
  props: {
    conversationId: {
      type: String,
      required: true
    },
    targetUserId: {
      type: String,
      default: ""
    },
    nickname: {
      type: String,
      default: "用户"
    },
    avatar: {
      type: String,
      default: "/static/default-avatar.png"
    }
  },
  setup(__props) {
    const props = __props;
    const messageList = common_vendor.ref([]);
    const inputText = common_vendor.ref("");
    const sending = common_vendor.ref(false);
    const loading = common_vendor.ref(false);
    const loadingMore = common_vendor.ref(false);
    const scrollTop = common_vendor.ref(0);
    const page = common_vendor.ref(1);
    const hasMore = common_vendor.ref(true);
    const currentUser = common_vendor.ref(null);
    const targetAvatar = common_vendor.ref(props.avatar);
    const myAvatar = common_vendor.ref("/static/default-avatar.png");
    const showOptions = common_vendor.ref(false);
    function isOwnMessage(msg) {
      var _a, _b;
      return msg.fromUserId === ((_a = currentUser.value) == null ? void 0 : _a.id) || msg.senderId === ((_b = currentUser.value) == null ? void 0 : _b.id);
    }
    async function loadMessages(isLoadMore = false) {
      if (isLoadMore) {
        if (!hasMore.value || loadingMore.value)
          return;
        loadingMore.value = true;
        page.value++;
      } else {
        loading.value = true;
        page.value = 1;
        hasMore.value = true;
      }
      try {
        const res = await api_chat.getMessageList(props.conversationId, page.value, pageSize);
        if (res && res.data) {
          const messages = Array.isArray(res.data) ? res.data : res.data.list || [];
          if (isLoadMore) {
            messageList.value = [...messages.reverse(), ...messageList.value];
          } else {
            messageList.value = messages.reverse();
            common_vendor.nextTick$1(() => {
              scrollToBottom();
            });
          }
          if (messages.length < pageSize) {
            hasMore.value = false;
          }
        } else {
          messageList.value = [];
        }
      } catch (e) {
        common_vendor.index.__f__("error", "at pages/chat/detail.vue:200", "获取消息列表失败:", e);
        common_vendor.index.showToast({
          title: e.message || "获取消息失败",
          icon: "none"
        });
      } finally {
        loading.value = false;
        loadingMore.value = false;
      }
    }
    function loadMoreMessages() {
      loadMessages(true);
    }
    async function sendMessage() {
      var _a, _b;
      const content = inputText.value.trim();
      if (!content || sending.value)
        return;
      sending.value = true;
      try {
        const tempMessage = {
          id: "temp_" + Date.now(),
          content,
          fromUserId: (_a = currentUser.value) == null ? void 0 : _a.id,
          senderId: (_b = currentUser.value) == null ? void 0 : _b.id,
          createdTime: (/* @__PURE__ */ new Date()).toISOString(),
          sendTime: (/* @__PURE__ */ new Date()).toISOString(),
          status: "sending"
        };
        messageList.value.push(tempMessage);
        inputText.value = "";
        common_vendor.nextTick$1(() => {
          scrollToBottom();
        });
        const res = await api_chat.sendTextMessage(props.conversationId, content);
        if (res && res.data) {
          const index = messageList.value.findIndex((m) => m.id === tempMessage.id);
          if (index !== -1) {
            messageList.value[index] = {
              ...res.data,
              status: "sent"
            };
          } else {
            messageList.value.push({
              ...res.data,
              status: "sent"
            });
          }
        }
        utils_websocket.sendWebSocketMessage({
          type: "message",
          conversationId: props.conversationId,
          content
        });
        common_vendor.nextTick$1(() => {
          scrollToBottom();
        });
      } catch (e) {
        common_vendor.index.__f__("error", "at pages/chat/detail.vue:274", "发送消息失败:", e);
        common_vendor.index.showToast({
          title: e.message || "发送失败",
          icon: "none"
        });
        const index = messageList.value.findIndex((m) => m.status === "sending");
        if (index !== -1) {
          messageList.value.splice(index, 1);
        }
      } finally {
        sending.value = false;
      }
    }
    function scrollToBottom() {
      scrollTop.value = 999999;
    }
    function formatMessageTime(time) {
      if (!time)
        return "";
      const date = new Date(time);
      const now = /* @__PURE__ */ new Date();
      const diff = now - date;
      if (diff < 5 * 60 * 1e3) {
        return "";
      }
      const hours = date.getHours().toString().padStart(2, "0");
      const minutes = date.getMinutes().toString().padStart(2, "0");
      return `${hours}:${minutes}`;
    }
    function handleWebSocketMessage(data) {
      common_vendor.index.__f__("log", "at pages/chat/detail.vue:316", "收到WebSocket消息:", data);
      if (data.type === "message" || data.type === "new_message") {
        if (data.conversationId === props.conversationId) {
          messageList.value.push({
            ...data,
            createdTime: data.createdTime || data.sendTime || (/* @__PURE__ */ new Date()).toISOString()
          });
          common_vendor.nextTick$1(() => {
            scrollToBottom();
          });
        }
      }
    }
    common_vendor.onMounted(() => {
      currentUser.value = utils_storage.getUserInfo();
      if (currentUser.value && currentUser.value.face) {
        myAvatar.value = currentUser.value.face;
      }
      common_vendor.index.setNavigationBarTitle({
        title: decodeURIComponent(props.nickname || "聊天")
      });
      loadMessages();
      utils_websocket.onWebSocketMessage(handleWebSocketMessage);
    });
    function goBack() {
      common_vendor.index.navigateBack();
    }
    function showMoreActions() {
      common_vendor.index.showActionSheet({
        itemList: ["清空聊天记录", "查看用户信息"],
        success: (res) => {
          if (res.tapIndex === 0) {
            clearChatHistory();
          } else if (res.tapIndex === 1) {
            viewUserInfo();
          }
        }
      });
    }
    function showMoreOptions() {
      showOptions.value = true;
    }
    function selectImage() {
      showOptions.value = false;
      common_vendor.index.chooseImage({
        count: 1,
        sizeType: ["compressed"],
        sourceType: ["album", "camera"],
        success: (res) => {
          common_vendor.index.__f__("log", "at pages/chat/detail.vue:385", "选择图片:", res.tempFilePaths);
          common_vendor.index.showToast({
            title: "图片功能开发中",
            icon: "none"
          });
        }
      });
    }
    function selectFile() {
      showOptions.value = false;
      common_vendor.index.showToast({
        title: "文件功能开发中",
        icon: "none"
      });
    }
    function selectLocation() {
      showOptions.value = false;
      common_vendor.index.chooseLocation({
        success: (res) => {
          common_vendor.index.__f__("log", "at pages/chat/detail.vue:408", "选择位置:", res);
          common_vendor.index.showToast({
            title: "位置功能开发中",
            icon: "none"
          });
        }
      });
    }
    function clearChatHistory() {
      common_vendor.index.showModal({
        title: "提示",
        content: "确定要清空聊天记录吗？",
        success: (res) => {
          if (res.confirm) {
            messageList.value = [];
            common_vendor.index.showToast({
              title: "聊天记录已清空",
              icon: "success"
            });
          }
        }
      });
    }
    function viewUserInfo() {
      common_vendor.index.showToast({
        title: "用户信息功能开发中",
        icon: "none"
      });
    }
    common_vendor.onUnmounted(() => {
      utils_websocket.offWebSocketMessage(handleWebSocketMessage);
    });
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.o(goBack),
        b: common_vendor.p({
          name: "arrow-left",
          size: "20",
          color: "#333"
        }),
        c: targetAvatar.value,
        d: common_vendor.t(decodeURIComponent(props.nickname || "用户")),
        e: common_vendor.o(showMoreActions),
        f: common_vendor.p({
          name: "more-dot-fill",
          size: "20",
          color: "#333"
        }),
        g: common_vendor.f(messageList.value, (msg, index, i0) => {
          return common_vendor.e({
            a: !isOwnMessage(msg)
          }, !isOwnMessage(msg) ? {
            b: targetAvatar.value
          } : {}, {
            c: common_vendor.t(msg.content),
            d: msg.status === "sending"
          }, msg.status === "sending" ? {
            e: "ee06274a-2-" + i0,
            f: common_vendor.p({
              size: "12",
              color: "#999"
            })
          } : {}, {
            g: isOwnMessage(msg) ? 1 : "",
            h: common_vendor.t(formatMessageTime(msg.createdTime || msg.sendTime)),
            i: isOwnMessage(msg)
          }, isOwnMessage(msg) ? {
            j: myAvatar.value
          } : {}, {
            k: msg.id || index,
            l: isOwnMessage(msg) ? 1 : ""
          });
        }),
        h: loadingMore.value
      }, loadingMore.value ? {
        i: common_vendor.p({
          mode: "spinner",
          size: "20"
        })
      } : {}, {
        j: messageList.value.length === 0 && !loading.value
      }, messageList.value.length === 0 && !loading.value ? {
        k: common_assets._imports_0
      } : {}, {
        l: scrollTop.value,
        m: common_vendor.o(loadMoreMessages),
        n: common_vendor.o(sendMessage),
        o: common_vendor.o(($event) => inputText.value = $event),
        p: common_vendor.p({
          placeholder: "输入消息...",
          border: false,
          clearable: true,
          ["confirm-type"]: "send",
          modelValue: inputText.value
        }),
        q: common_vendor.o(showMoreOptions),
        r: common_vendor.p({
          name: "plus-circle",
          size: "24",
          color: "#007AFF"
        }),
        s: common_vendor.o(sendMessage),
        t: common_vendor.p({
          type: "primary",
          size: "mini",
          text: "发送",
          disabled: !inputText.value.trim() || sending.value,
          loading: sending.value
        }),
        v: common_vendor.p({
          name: "photo",
          size: "24",
          color: "#007AFF"
        }),
        w: common_vendor.o(selectImage),
        x: common_vendor.p({
          name: "file-text",
          size: "24",
          color: "#007AFF"
        }),
        y: common_vendor.o(selectFile),
        z: common_vendor.p({
          name: "map",
          size: "24",
          color: "#007AFF"
        }),
        A: common_vendor.o(selectLocation),
        B: common_vendor.o(($event) => showOptions.value = false),
        C: common_vendor.p({
          show: showOptions.value,
          mode: "bottom"
        })
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-ee06274a"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/chat/detail.js.map
