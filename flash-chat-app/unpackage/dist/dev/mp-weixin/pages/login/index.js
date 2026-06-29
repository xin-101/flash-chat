"use strict";
const common_vendor = require("../../common/vendor.js");
const api_auth = require("../../api/auth.js");
const utils_websocket = require("../../utils/websocket.js");
const config_env = require("../../config/env.js");
const _sfc_main = {
  __name: "index",
  setup(__props) {
    const form = common_vendor.reactive({
      phone: "18000000000",
      code: ""
    });
    const countdown = common_vendor.ref(0);
    const sendingCode = common_vendor.ref(false);
    const loggingIn = common_vendor.ref(false);
    const isPhoneValid = common_vendor.computed(() => /^1\d{10}$/.test(form.phone));
    const canGetCode = common_vendor.computed(() => {
      return isPhoneValid.value && countdown.value === 0;
    });
    const codeButtonText = common_vendor.computed(() => countdown.value > 0 ? `${countdown.value}s后重试` : "获取验证码");
    const canLogin = common_vendor.computed(() => isPhoneValid.value && form.code && form.code.length >= 4);
    const loginButtonText = common_vendor.computed(() => loggingIn.value ? "登录中..." : "登录");
    let timer = null;
    function startCountdown(seconds = 60) {
      countdown.value = seconds;
      timer && clearInterval(timer);
      timer = setInterval(() => {
        countdown.value -= 1;
        if (countdown.value <= 0) {
          clearInterval(timer);
          timer = null;
        }
      }, 1e3);
    }
    async function getCode() {
      if (!isPhoneValid.value || countdown.value > 0)
        return;
      sendingCode.value = true;
      try {
        await api_auth.sendCode(form.phone);
        common_vendor.index.showToast({ title: "验证码已发送", icon: "none" });
        startCountdown(60);
      } catch (e) {
        common_vendor.index.showToast({ title: e && (e.message || e.msg) || "发送失败", icon: "none" });
      } finally {
        sendingCode.value = false;
      }
    }
    function handlePhoneInput(e) {
      form.phone = e.detail.value;
    }
    function handleCodeInput(e) {
      form.code = e.detail.value;
    }
    async function submit() {
      if (!canLogin.value) {
        if (!isPhoneValid.value) {
          common_vendor.index.showToast({ title: "请输入正确的手机号", icon: "none" });
        } else if (!form.code || form.code.length < 4) {
          common_vendor.index.showToast({ title: "请输入验证码", icon: "none" });
        }
        return;
      }
      loggingIn.value = true;
      try {
        if (!form.phone || !form.code) {
          common_vendor.index.showToast({ title: "请填写完整信息", icon: "none" });
          return;
        }
        const res = await api_auth.login(form.phone, form.code);
        if (res && res.data && res.data.token) {
          const token = res.data.token;
          const wsUrl = config_env.BASE_URL.replace("http://", "ws://").replace("https://", "wss://") + "/chat/ws";
          setTimeout(() => {
            utils_websocket.closeWebSocket(true);
            utils_websocket.connectWebSocket(wsUrl, token, true);
          }, 500);
        }
        common_vendor.index.showToast({ title: "登录成功", icon: "success" });
        setTimeout(() => {
          common_vendor.index.reLaunch({ url: "/pages/index/index" });
        }, 300);
      } catch (e) {
        const errorMsg = (e == null ? void 0 : e.message) || (e == null ? void 0 : e.msg) || "登录失败，请重试";
        common_vendor.index.showToast({
          title: errorMsg,
          icon: "none",
          duration: 2e3
        });
        common_vendor.index.__f__("error", "at pages/login/index.vue:178", "登录失败:", e);
      } finally {
        loggingIn.value = false;
      }
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.o([($event) => form.phone = $event.detail.value, handlePhoneInput]),
        b: form.phone,
        c: form.phone
      }, form.phone ? {
        d: common_vendor.o(($event) => form.phone = "")
      } : {}, {
        e: common_vendor.o([($event) => form.code = $event.detail.value, handleCodeInput]),
        f: form.code,
        g: form.code
      }, form.code ? {
        h: common_vendor.o(($event) => form.code = "")
      } : {}, {
        i: common_vendor.t(codeButtonText.value),
        j: common_vendor.o(getCode),
        k: !canGetCode.value || sendingCode.value,
        l: common_vendor.t(loginButtonText.value),
        m: common_vendor.o(submit),
        n: !canLogin.value || loggingIn.value
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-d08ef7d4"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/login/index.js.map
