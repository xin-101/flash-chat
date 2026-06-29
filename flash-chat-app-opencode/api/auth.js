import { get, post, put } from '../utils/request'

export function sendCode(phone) {
  return get('/auth/auth/sendSms', { phone })
}

export function login(phone, code) {
  return post('/auth/auth/login', { phone, code })
}

export function logout() {
  return post('/auth/auth/logout')
}

export function updateFlashChatNum(flashChatNum) {
  return put('/auth/auth/user/flashChatNum', { flashChatNum })
}

export function updateUserInfo(data) {
  return put('/auth/auth/user/info', data)
}
