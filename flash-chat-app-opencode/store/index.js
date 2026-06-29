import { reactive } from 'vue'
import { getUserInfo as getStoredUserInfo } from '../utils/storage'
import { getConversationList } from '../api/chat'

const userState = reactive({
  userInfo: null,
  isLoggedIn: false,
})

export function useUserStore() {
  function init() {
    const stored = getStoredUserInfo()
    if (stored) {
      userState.userInfo = stored
      userState.isLoggedIn = true
    }
  }

  function setUserInfo(info) {
    userState.userInfo = info
    userState.isLoggedIn = true
  }

  function clearUserInfo() {
    userState.userInfo = null
    userState.isLoggedIn = false
  }

  return {
    get userInfo() { return userState.userInfo },
    get isLoggedIn() { return userState.isLoggedIn },
    get userId() { return userState.userInfo?.id || '' },
    get avatar() { return userState.userInfo?.face || '' },
    get nickname() { return userState.userInfo?.nickname || '用户' },
    init,
    setUserInfo,
    clearUserInfo,
  }
}

const chatState = reactive({
  conversationList: [],
  currentConversation: null,
  unreadCount: 0,
  loading: false,
  friendRequestCount: 0,
})

export function useChatStore() {
  async function loadConversationList() {
    if (chatState.loading) return
    chatState.loading = true
    try {
      const res = await getConversationList()
      chatState.conversationList = res?.data || []
      chatState.unreadCount = chatState.conversationList.reduce(
        (sum, item) => sum + (item.unreadCount || 0), 0
      )
    } catch (e) {
      console.error('加载会话列表失败:', e)
    } finally {
      chatState.loading = false
    }
  }

  function setCurrentConversation(c) {
    chatState.currentConversation = c
  }

  function clearCurrentConversation() {
    chatState.currentConversation = null
  }

  function clearConversationUnread(conversationId) {
    const c = chatState.conversationList.find((i) => i.id === conversationId)
    if (c) {
      chatState.unreadCount -= c.unreadCount || 0
      c.unreadCount = 0
    }
  }

  return {
    get conversationList() { return chatState.conversationList },
    get currentConversation() { return chatState.currentConversation },
    get unreadCount() { return chatState.unreadCount },
    get loading() { return chatState.loading },
    loadConversationList,
    setCurrentConversation,
    clearCurrentConversation,
    clearConversationUnread,

    get friendRequestCount() { return chatState.friendRequestCount },
    incrementFriendRequestCount() { chatState.friendRequestCount++ },
    resetFriendRequestCount() { chatState.friendRequestCount = 0 },
  }
}
