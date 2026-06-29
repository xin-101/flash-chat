"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_storage = require("../../utils/storage.js");
const _sfc_main = {
  __name: "index",
  setup(__props) {
    const userInfo = common_vendor.ref({});
    const chatCount = common_vendor.ref(0);
    const friendCount = common_vendor.ref(0);
    common_vendor.onMounted(() => {
      userInfo.value = utils_storage.getUserInfo() || {};
      chatCount.value = Math.floor(Math.random() * 50) + 10;
      friendCount.value = Math.floor(Math.random() * 20) + 5;
    });
    function goToProfile() {
      common_vendor.index.showToast({
        title: "个人资料功能开发中",
        icon: "none"
      });
    }
    function goToPrivacy() {
      common_vendor.index.showToast({
        title: "隐私设置功能开发中",
        icon: "none"
      });
    }
    function goToNotification() {
      common_vendor.index.showToast({
        title: "通知设置功能开发中",
        icon: "none"
      });
    }
    function goToAbout() {
      common_vendor.index.showToast({
        title: "关于我们功能开发中",
        icon: "none"
      });
    }
    function logout() {
      common_vendor.index.showModal({
        title: "确认退出",
        content: "确定要退出登录吗？",
        success: (res) => {
          if (res.confirm) {
            utils_storage.clearAuth();
            common_vendor.index.reLaunch({
              url: "/pages/login/index"
            });
          }
        }
      });
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: userInfo.value.face || "/static/default-avatar.svg",
        b: common_vendor.t(userInfo.value.nickname || "用户0000"),
        c: userInfo.value.mobile
      }, userInfo.value.mobile ? {
        d: common_vendor.t(userInfo.value.id || userInfo.value.mobile)
      } : {}, {
        e: common_vendor.o(goToProfile),
        f: common_vendor.o(goToProfile),
        g: common_vendor.o(goToNotification),
        h: common_vendor.o(goToPrivacy),
        i: common_vendor.o(goToAbout),
        j: common_vendor.o(logout)
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-f97bc692"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/my/index.js.map
