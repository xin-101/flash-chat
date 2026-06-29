"use strict";
const common_vendor = require("../common/vendor.js");
const TOKEN_KEY = "flash_chat_token";
const USER_INFO_KEY = "flash_chat_user_info";
function getToken() {
  return common_vendor.index.getStorageSync(TOKEN_KEY) || "";
}
function removeToken() {
  common_vendor.index.removeStorageSync(TOKEN_KEY);
}
function getUserInfo() {
  return common_vendor.index.getStorageSync(USER_INFO_KEY) || null;
}
function removeUserInfo() {
  common_vendor.index.removeStorageSync(USER_INFO_KEY);
}
function clearAuth() {
  removeToken();
  removeUserInfo();
}
exports.clearAuth = clearAuth;
exports.getToken = getToken;
exports.getUserInfo = getUserInfo;
//# sourceMappingURL=../../.sourcemap/mp-weixin/utils/storage.js.map
