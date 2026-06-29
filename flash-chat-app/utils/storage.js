/**
 * 本地存储工具类
 * 统一管理 token、用户信息等本地存储
 */

const TOKEN_KEY = 'flash_chat_token'
const USER_INFO_KEY = 'flash_chat_user_info'

/**
 * 存储 token
 */
export function setToken(token) {
  uni.setStorageSync(TOKEN_KEY, token)
}

/**
 * 获取 token
 */
export function getToken() {
  return uni.getStorageSync(TOKEN_KEY) || ''
}

/**
 * 移除 token
 */
export function removeToken() {
  uni.removeStorageSync(TOKEN_KEY)
}

/**
 * 存储用户信息
 * @param {Object} userInfo 用户信息对象（UserVO）
 * 包含字段：id, token, mobile, nickname, face, flashChatNum, realName, sex, email, birthday, 
 * country, province, city, district, chatBg, friendCircleBg, signature, createdTime, updatedTime
 */
export function setUserInfo(userInfo) {
  uni.setStorageSync(USER_INFO_KEY, userInfo)
}

/**
 * 获取用户信息
 * @returns {Object|null} 用户信息对象（UserVO），包含 id, mobile, nickname, face 等字段
 */
export function getUserInfo() {
  return uni.getStorageSync(USER_INFO_KEY) || null
}

/**
 * 获取用户ID
 * @returns {string} 用户ID
 */
export function getUserId() {
  const userInfo = getUserInfo()
  return userInfo ? (userInfo.id || '') : ''
}

/**
 * 移除用户信息
 */
export function removeUserInfo() {
  uni.removeStorageSync(USER_INFO_KEY)
}

/**
 * 清除所有登录信息
 */
export function clearAuth() {
  removeToken()
  removeUserInfo()
}

/**
 * 检查是否已登录
 */
export function isLoggedIn() {
  return !!getToken()
}



