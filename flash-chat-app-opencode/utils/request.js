import { BASE_URL } from '../config/env'
import { getToken, getUserId } from './storage'

const TIMEOUT = 10000

function request({ url, method = 'GET', data = {}, header = {}, showLoading = false }) {
  if (showLoading) {
    uni.showLoading({ title: '加载中...', mask: true })
  }

  const headers = { 'Content-Type': 'application/json', ...header }

  const userId = getUserId()
  if (userId) headers['userId'] = userId

  const token = getToken()
  if (token) headers['Authorization'] = `Bearer ${token}`

  return new Promise((resolve, reject) => {
    uni.request({
      url: BASE_URL + url,
      method,
      data,
      header: headers,
      timeout: TIMEOUT,
      success: (res) => {
        if (showLoading) uni.hideLoading()
        if (res.statusCode >= 200 && res.statusCode < 300) {
          const body = res.data
          if (body && typeof body === 'object') {
            if (body.success === false) {
              reject({ message: body.message || '请求失败', errorCode: body.errorCode })
              return
            }
            resolve(body)
          } else {
            resolve(body)
          }
        } else if (res.statusCode === 401) {
          uni.showToast({ title: '登录已过期', icon: 'none', duration: 2000 })
          uni.reLaunch({ url: '/pages/login/index' })
          reject({ message: '未授权', statusCode: 401 })
        } else {
          reject({ message: `请求错误: ${res.statusCode}`, statusCode: res.statusCode })
        }
      },
      fail: (err) => {
        if (showLoading) uni.hideLoading()
        reject({ message: '网络异常，请稍后重试', error: err })
      },
    })
  })
}

export function get(url, params = {}) {
  return request({ url, method: 'GET', data: params })
}

export function post(url, data = {}) {
  return request({ url, method: 'POST', data })
}

export function put(url, data = {}) {
  return request({ url, method: 'PUT', data })
}

export function del(url, data = {}) {
  return request({ url, method: 'DELETE', data })
}

export function resolveImage(url) {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  return BASE_URL + url
}
