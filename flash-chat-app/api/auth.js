import {get,post} from '../utils/request.js';
import { setToken, setUserInfo } from '../utils/storage.js';

/**
 * 发送短信验证码
 */
export function sendCode(phone) {
  return get('/auth/auth/sendSms?phone=' + phone,{},{}, false);
}

/**
 * 手机号验证码登录
 */
export function login(phone, code) {
  return post('/auth/auth/login',{phone, code},{}, false);
}

/**
 * 登出
 * 注意：网关配置 StripPrefix=1 会去掉 /auth 前缀，所以需要 /auth/auth/logout
 */
export function logout() {
  return post('/auth/auth/logout',{phone, code},{}, false);
}
