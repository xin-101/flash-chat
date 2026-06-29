export function promisify() {
  const uniPromisify = {}
  const methods = [
    'getStorage',
    'setStorage',
    'removeStorage',
    'getSystemInfo',
    'login',
    'getUserProfile',
    'request',
    'downloadFile',
    'uploadFile',
    'chooseImage',
    'chooseVideo',
    'getLocation',
  ]

  methods.forEach((method) => {
    uniPromisify[method] = (options = {}) => {
      return new Promise((resolve, reject) => {
        options.success = resolve
        options.fail = reject
        uni[method](options)
      })
    }
  })

  return uniPromisify
}

export const uniPromisify = promisify()
