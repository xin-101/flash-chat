"use strict";
const common_vendor = require("../common/vendor.js");
const config_env = require("../config/env.js");
function request({ url, method = "GET", data = {}, header = {}, timeout = 1e4, addAuth = true }) {
  const userInfo = common_vendor.index.getStorageSync("userInfo") || null;
  const storedUserId = userInfo && userInfo.id ? userInfo.id : void 0;
  const finalHeader = { ...header };
  if (addAuth && storedUserId && finalHeader.id == null) {
    finalHeader.userId = storedUserId;
  }
  return new Promise((resolve, reject) => {
    common_vendor.index.request({
      url: `${config_env.BASE_URL}${url}`,
      method,
      data,
      header: finalHeader,
      timeout,
      success: (res) => {
        const { statusCode, data: data2 } = res;
        if (statusCode >= 200 && statusCode < 300) {
          if (data2 && typeof data2 === "object") {
            if (data2.success === false) {
              const errorMsg = data2.message || data2.errorCode || "请求失败";
              reject({
                message: errorMsg,
                errorCode: data2.errorCode,
                data: data2.data,
                statusCode
              });
              return;
            }
            resolve(data2);
          } else {
            resolve(data2);
          }
        } else {
          reject({ statusCode, data: data2 });
        }
      },
      fail: reject
    });
  });
}
function get(url, params = {}, header = {}, addAuth = true) {
  return request({ url, method: "GET", data: params, header, addAuth });
}
function post(url, body = {}, header = {}, addAuth = true) {
  return request({ url, method: "POST", data: body, header, addAuth });
}
exports.get = get;
exports.post = post;
//# sourceMappingURL=../../.sourcemap/mp-weixin/utils/request.js.map
