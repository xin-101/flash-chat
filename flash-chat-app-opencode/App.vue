<script>
import { connectWebSocket, closeWebSocket, onWebSocketMessage } from './utils/websocket'
import { getToken, getUserInfo, isLoggedIn } from './utils/storage'
import { useUserStore, useChatStore } from './store'
import { getFriendRequests } from './api/chat'
import { BASE_URL } from './config/env'

export default {
  onLaunch() {
    const userStore = useUserStore()
    userStore.init()

    if (isLoggedIn()) {
      const token = getToken()
      const wsUrl = BASE_URL.replace('http://', 'ws://').replace('https://', 'wss://') + '/chat/ws'
      setTimeout(() => {
        connectWebSocket(wsUrl, token, true)
      }, 1000)

      // 监听好友申请推送
      onWebSocketMessage((data) => {
        if (data.type === 'friend_request') {
          const nickname = data.fromNickname || '某位用户'
          const chatStore = useChatStore()
          if (chatStore) {
            chatStore.incrementFriendRequestCount()
          }
          uni.showToast({
            title: `${nickname} 请求添加好友`,
            icon: 'none',
            duration: 3000,
          })
        }
      })
    } else {
      uni.reLaunch({ url: '/pages/login/index' })
    }
  },
  onShow() {},
  onHide() {},
}
</script>

<style>
page {
  background-color: #f5f5f5;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}
view, text, image, input, button {
  box-sizing: border-box;
}
button {
  border: none;
  outline: none;
  padding: 0;
  margin: 0;
  line-height: normal;
  background-color: transparent;
}
button::after {
  border: none;
}
</style>
