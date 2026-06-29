"use strict";
const common_vendor = require("../../common/vendor.js");
const api_chat = require("../../api/chat.js");
const utils_storage = require("../../utils/storage.js");
if (!Array) {
  const _component_u_search = common_vendor.resolveComponent("u-search");
  const _component_u_loading_icon = common_vendor.resolveComponent("u-loading-icon");
  (_component_u_search + _component_u_loading_icon)();
}
const _sfc_main = {
  __name: "search",
  setup(__props) {
    const keyword = common_vendor.ref("");
    const userList = common_vendor.ref([]);
    const loading = common_vendor.ref(false);
    const searched = common_vendor.ref(false);
    const currentUser = common_vendor.ref(null);
    function getAvatar(user) {
      if (user.face) {
        return user.face;
      }
      return "/static/default-avatar.png";
    }
    async function handleSearch() {
      const searchKeyword = keyword.value.trim();
      if (!searchKeyword) {
        common_vendor.index.showToast({
          title: "请输入搜索关键词",
          icon: "none"
        });
        return;
      }
      loading.value = true;
      searched.value = true;
      try {
        const res = await api_chat.searchUser(searchKeyword);
        if (res && res.data) {
          const users = Array.isArray(res.data) ? res.data : [];
          userList.value = users.filter((user) => {
            var _a;
            return user.id !== ((_a = currentUser.value) == null ? void 0 : _a.id);
          });
        } else {
          userList.value = [];
        }
      } catch (e) {
        common_vendor.index.__f__("error", "at pages/user/search.vue:104", "搜索用户失败:", e);
        common_vendor.index.showToast({
          title: e.message || "搜索失败",
          icon: "none"
        });
        userList.value = [];
      } finally {
        loading.value = false;
      }
    }
    function handleKeywordChange(value) {
      if (!value.trim()) {
        userList.value = [];
        searched.value = false;
      }
    }
    async function startChat(user) {
      var _a;
      if (!user || !user.id) {
        common_vendor.index.showToast({
          title: "用户信息不完整",
          icon: "none"
        });
        return;
      }
      if (user.id === ((_a = currentUser.value) == null ? void 0 : _a.id)) {
        common_vendor.index.showToast({
          title: "不能和自己聊天",
          icon: "none"
        });
        return;
      }
      try {
        common_vendor.index.showLoading({
          title: "加载中..."
        });
        const res = await api_chat.createConversation(user.id);
        common_vendor.index.hideLoading();
        if (res && res.data) {
          const conversationId = res.data.id || res.data.conversationId || res.data;
          const nickname = user.nickname || "用户";
          const avatar = getAvatar(user);
          common_vendor.index.navigateTo({
            url: `/pages/chat/detail?conversationId=${conversationId}&targetUserId=${user.id}&nickname=${encodeURIComponent(nickname)}&avatar=${encodeURIComponent(avatar)}`
          });
        } else {
          common_vendor.index.showToast({
            title: "创建会话失败",
            icon: "none"
          });
        }
      } catch (e) {
        common_vendor.index.hideLoading();
        common_vendor.index.__f__("error", "at pages/user/search.vue:169", "创建会话失败:", e);
        common_vendor.index.showToast({
          title: e.message || "创建会话失败",
          icon: "none"
        });
      }
    }
    currentUser.value = utils_storage.getUserInfo();
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.o(handleSearch),
        b: common_vendor.o(handleSearch),
        c: common_vendor.o(handleKeywordChange),
        d: common_vendor.o(($event) => keyword.value = $event),
        e: common_vendor.p({
          placeholder: "搜索手机号或昵称",
          ["show-action"]: true,
          ["action-text"]: "搜索",
          shape: "round",
          modelValue: keyword.value
        }),
        f: keyword.value
      }, keyword.value ? common_vendor.e({
        g: common_vendor.f(userList.value, (user, index, i0) => {
          return common_vendor.e({
            a: getAvatar(user),
            b: common_vendor.t(user.nickname || "未知用户"),
            c: user.mobile
          }, user.mobile ? {
            d: common_vendor.t(user.mobile)
          } : {}, {
            e: user.flashChatNum
          }, user.flashChatNum ? {
            f: common_vendor.t(user.flashChatNum)
          } : {}, {
            g: user.id || index,
            h: common_vendor.o(($event) => startChat(user), user.id || index)
          });
        }),
        h: userList.value.length === 0 && !loading.value && searched.value
      }, userList.value.length === 0 && !loading.value && searched.value ? {} : {}, {
        i: loading.value
      }, loading.value ? {} : {}) : {});
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-a7eee1f1"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/user/search.js.map
