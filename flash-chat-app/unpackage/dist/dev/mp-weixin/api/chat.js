"use strict";
const utils_request = require("../utils/request.js");
function getChatList() {
  return utils_request.get("/chat/conversation/list");
}
function getMessageList(conversationId, page = 1, size = 20) {
  return utils_request.get(`/chat/message/list`, {
    conversationId,
    page,
    size
  });
}
function sendTextMessage(conversationId, content) {
  return utils_request.post("/chat/message/send", {
    conversationId,
    content,
    type: "text"
  });
}
function createConversation(targetUserId) {
  return utils_request.post("/chat/conversation/create", {
    targetUserId
  });
}
function searchUser(keyword) {
  return utils_request.get("/chat/user/search", {
    keyword
  });
}
exports.createConversation = createConversation;
exports.getChatList = getChatList;
exports.getMessageList = getMessageList;
exports.searchUser = searchUser;
exports.sendTextMessage = sendTextMessage;
//# sourceMappingURL=../../.sourcemap/mp-weixin/api/chat.js.map
