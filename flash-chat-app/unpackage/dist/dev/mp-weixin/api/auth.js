"use strict";
const utils_request = require("../utils/request.js");
require("../common/vendor.js");
function sendCode(phone) {
  return utils_request.get("/auth/auth/sendSms?phone=" + phone, {}, {}, false);
}
function login(phone, code) {
  return utils_request.post("/auth/auth/login", { phone, code }, {}, false);
}
exports.login = login;
exports.sendCode = sendCode;
//# sourceMappingURL=../../.sourcemap/mp-weixin/api/auth.js.map
