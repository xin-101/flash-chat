import { BASE_URL } from '../config/env.js';
import { getToken } from './storage.js';

/**
 * 统一网络请求封装
 * 自动拼接BASE_URL
 * 统一超时和状态码处理
 * 自动添加 token
 */
export function request({url, method = 'GET', data = {},header={},timeout=10000,addAuth= true}) {
  // 安全读取 userId
  const userInfo = uni.getStorageSync('userInfo') || null;
  const storedUserId = userInfo && userInfo.id ? userInfo.id : undefined;

  //仅在需要鉴权且调用方未显式提供 userId 时追加

  const finalHeader = {...header};
  if (addAuth && storedUserId && finalHeader.id == null) {
    finalHeader.userId = storedUserId;
  }

  return new Promise((resolve, reject) => {
    uni.request({
      url: `${BASE_URL}${url}`,
      method,
      data,
      header: finalHeader,
      timeout,
      success: (res) => {
        const {statusCode, data} = res;
        if (statusCode >= 200 && statusCode < 300) {
          // 检查后端返回的 Response 格式：{ success: boolean, message: string, errorCode: string, data: T }
          if (data && typeof data === 'object') {
            // 如果 success 为 false，表示业务失败，应该 reject
            if (data.success === false) {
              const errorMsg = data.message || data.errorCode || '请求失败';
              reject({ 
                message: errorMsg, 
                errorCode: data.errorCode, 
                data: data.data,
                statusCode 
              });
              return;
            }
            // 请求成功，返回 data 字段
            resolve(data);
          } else {
            resolve(data);
          }
        } else {
          reject({statusCode, data});
        }
      },
      fail: reject
    });
  });
}
export function get(url, params = {}, header = {}, addAuth = true) {
  return request({url, method: 'GET', data: params, header, addAuth})
}
export function post(url, body = {}, header = {}, addAuth = true) {
  return request({url, method: 'POST', data: body, header, addAuth})
}




//   // 自动添加 token
//   const token = getToken();
//   const headers = {
//     'Content-Type': 'application/json',
//     ...header
//   };
//   if (token) {
//     headers['token'] = token;
//     // 从存储中获取用户ID，用于后端验证
//     const userInfo = uni.getStorageSync('flash_chat_user_info');
//     if (userInfo && userInfo.id) {
//       headers['userId'] = userInfo.id;
//     }
//   }
//
//   return new Promise((resolve, reject) => {
//     uni.request({
//       url: BASE_URL + url,
//       method,
//       data,
//       header: headers,
//       timeout,
//       success: (res) => {
//         // 处理 HTTP 状态码
//         if (res.statusCode === 200) {
//           // 处理后端 Response 格式：{ success: boolean, message: string, errorCode: string, data: T }
//           if (res.data && typeof res.data === 'object') {
//             if (res.data.success === false) {
//               // 请求失败
//               const errorMsg = res.data.message || res.data.errorCode || '请求失败';
//               // token 过期或未登录（根据实际错误码调整）
//               if (res.data.errorCode && (res.data.errorCode.includes('401') || res.data.errorCode.includes('403'))) {
//                 uni.removeStorageSync('flash_chat_token');
//                 uni.removeStorageSync('flash_chat_user_info');
//                 uni.reLaunch({ url: '/pages/login/index' });
//               }
//               reject({ message: errorMsg, errorCode: res.data.errorCode, data: res.data.data });
//             } else {
//               // 请求成功，返回 data 字段
//               resolve(res.data);
//             }
//           } else {
//             resolve(res.data);
//           }
//         } else if (res.statusCode === 503) {
//           // 503 Service Unavailable - 服务不可用
//           reject({
//             message: '服务暂时不可用，请检查后端服务是否启动',
//             errorCode: '503',
//             status: 503,
//             path: res.data?.path || url
//           });
//         } else if (res.statusCode === 404) {
//           // 404 Not Found - 接口不存在
//           reject({
//             message: '接口不存在，请检查接口路径是否正确',
//             errorCode: '404',
//             status: 404,
//             path: res.data?.path || url
//           });
//         } else if (res.statusCode === 500) {
//           // 500 Internal Server Error - 服务器内部错误
//           reject({
//             message: res.data?.message || '服务器内部错误',
//             errorCode: res.data?.errorCode || '500',
//             status: 500,
//             path: res.data?.path || url
//           });
//         } else {
//           // 其他状态码
//           reject({
//             message: res.data?.message || `请求失败 (${res.statusCode})`,
//             errorCode: res.data?.errorCode || String(res.statusCode),
//             status: res.statusCode,
//             data: res.data
//           });
//         }
//       },
//       fail: (err) => {
//         reject(err);
//       }
//     });
//   });
// }
// export function get(url,params={},header={}){
//   return request({
//     url,
//     method:'GET',
//     data:params,
//     header
//   })
// }
// export function post(url,body={},header={}){
//   return request({
//     url,
//     method:'POST',
//     data:body,
//     header
//   })
// }