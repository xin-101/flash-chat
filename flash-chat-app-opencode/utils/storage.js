const TOKEN_KEY = 'flash_chat_token'
const USER_KEY = 'flash_chat_user'

export function setToken(token) {
  uni.setStorageSync(TOKEN_KEY, token)
}

export function getToken() {
  return uni.getStorageSync(TOKEN_KEY) || ''
}

export function removeToken() {
  uni.removeStorageSync(TOKEN_KEY)
}

export function setUserInfo(info) {
  uni.setStorageSync(USER_KEY, info)
}

export function getUserInfo() {
  return uni.getStorageSync(USER_KEY) || null
}

export function getUserId() {
  const info = getUserInfo()
  return info ? info.id || '' : ''
}

export function removeUserInfo() {
  uni.removeStorageSync(USER_KEY)
}

export function clearAuth() {
  removeToken()
  removeUserInfo()
}

export function isLoggedIn() {
  return !!getToken() && !!getUserId()
}
