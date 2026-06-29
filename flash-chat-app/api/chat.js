import { get, post } from '../utils/request.js';
/**
 * 获取会话列表
 * 注意：网关配置 StripPrefix=1 会去掉 /chat 前缀
 * 如果后端路径是 /conversation/list，前端调用 /chat/conversation/list
 * 如果后端路径是 /chat/conversation/list，前端需要调用 /chat/chat/conversation/list
 */
export function getChatList() {
  return get('/chat/conversation/list');
}

/**
 * 获取聊天消息列表
 * @param {string} conversationId 会话ID
 * @param {number} page 页码
 * @param {number} size 每页数量
 * 注意：网关配置 StripPrefix=1 会去掉 /chat 前缀
 */
export function getMessageList(conversationId, page = 1, size = 20) {
  return get(`/chat/message/list`, {
    conversationId,
    page,
    size
  });
}

/**
 * 发送文本消息
 * @param {string} conversationId 会话ID
 * @param {string} content 消息内容
 * 注意：网关配置 StripPrefix=1 会去掉 /chat 前缀
 */
export function sendTextMessage(conversationId, content) {
  return post('/chat/message/send', {
    conversationId,
    content,
    type: 'text'
  });
}

/**
 * 创建会话
 * @param {string} targetUserId 目标用户ID
 * 注意：网关配置 StripPrefix=1 会去掉 /chat 前缀
 */
export function createConversation(targetUserId) {
  return post('/chat/conversation/create', {
    targetUserId
  });
}

/**
 * 搜索用户
 * @param {string} keyword 搜索关键词（手机号或昵称）
 * 注意：网关配置 StripPrefix=1 会去掉 /chat 前缀
 */
export function searchUser(keyword) {
  return get('/chat/user/search', {
    keyword
  });
}

/**
 * 获取用户信息
 * @param {string} userId 用户ID
 * 注意：网关配置 StripPrefix=1 会去掉 /chat 前缀
 */
export function getUserInfo(userId) {
  return get(`/chat/user/info/${userId}`);
}



