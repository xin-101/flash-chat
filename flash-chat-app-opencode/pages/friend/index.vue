<template>
  <view class="friend-page">
    <view class="search-bar" @click="goSearch">
      <text class="search-icon">🔍</text>
      <text class="search-text">搜索用户</text>
    </view>
    <view class="create-group-bar" @click="createGroup">
      <text class="group-icon">👥</text>
      <text class="group-text">创建群聊</text>
      <text class="group-arrow">&#x203A;</text>
    </view>

    <view v-if="requests.length > 0" class="section">
      <view class="section-title">
        <text>好友申请</text>
        <text class="badge">{{ requests.length }}</text>
      </view>
      <view class="request-list">
        <view class="request-item" v-for="r in requests" :key="r.id">
          <image class="avatar" :src="$resolveImage(r.fromFace) || '/static/default-avatar.png'" />
          <view class="request-info">
            <text class="nickname">{{ r.fromNickname }}</text>
            <text class="remark" v-if="r.remark">备注：{{ r.remark }}</text>
          </view>
          <view class="request-actions">
            <button class="btn-approve" @click="approve(r.id)">同意</button>
            <button class="btn-reject" @click="reject(r.id)">拒绝</button>
          </view>
        </view>
      </view>
    </view>

    <view class="section">
      <view class="section-title"><text>好友列表</text></view>
      <view v-if="friends.length === 0" class="empty">
        <text>暂无好友，点击上方搜索框添加好友</text>
      </view>
      <view class="friend-list">
        <view class="friend-item" v-for="f in friends" :key="f.id"
              @click="openChat(f)">
          <image class="avatar" :src="$resolveImage(f.face) || '/static/default-avatar.png'" />
          <view class="friend-info">
            <text class="nickname">{{ f.remark || f.nickname }}</text>
            <text class="extra" v-if="f.remark">昵称：{{ f.nickname }}</text>
          </view>
          <view class="friend-actions" @click.stop>
            <button class="btn-chat" @click="openChat(f)">聊天</button>
            <button class="btn-more" @click="showActions(f)">···</button>
          </view>
        </view>
      </view>
    </view>

    <view class="action-sheet" v-if="showSheet" @click="closeSheet">
      <view class="sheet-content" @click.stop>
        <text class="sheet-title">{{ actionTarget.remark || actionTarget.nickname }}</text>
        <view class="sheet-item" @click="setRemarkAction">
          <text>设置备注</text>
        </view>
        <view class="sheet-item" @click="toggleBlockAction">
          <text>{{ actionTarget.isBlock ? '取消拉黑' : '拉黑' }}</text>
        </view>
        <view class="sheet-item danger" @click="deleteFriendAction">
          <text>删除好友</text>
        </view>
        <view class="sheet-cancel" @click="closeSheet">
          <text>取消</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { getFriendList, getFriendRequests, getFriendRequestsPage, approveFriendRequest, rejectFriendRequest, deleteFriend, setFriendRemark, blockFriend, unblockFriend, createConversation } from '../../api/chat'
import { useChatStore } from '../../store'

export default {
  data() {
    return {
      friends: [],
      requests: [],
      showSheet: false,
      actionTarget: {},
      remarkInput: '',
      requestPage: 1,
      requestTotal: 0,
      loadingMore: false
    }
  },
  onShow() {
    const chatStore = useChatStore()
    chatStore.resetFriendRequestCount()
    this.requests = []
    this.requestPage = 1
    this.requestTotal = 0
    this.loadData()
  },
  onUnload() {
    uni.removeTabBarBadge({ index: 1 })
  },
  onReachBottom() {
    this.loadMore()
  },
  methods: {
    async loadData() {
      try {
        const [friendRes, requestRes] = await Promise.all([
          getFriendList(),
          getFriendRequestsPage(this.requestPage, 20)
        ])
        this.friends = (friendRes.data || []).map(f => ({ ...f, _showActions: false }))
        const pageData = requestRes.data || {}
        this.requests = pageData.records || []
        this.requestTotal = pageData.total || 0

        const count = this.requests.length
        if (count > 0) {
          uni.setTabBarBadge({ index: 1, text: String(count) })
        } else {
          uni.removeTabBarBadge({ index: 1 })
        }
      } catch (e) {
        console.error('加载好友数据失败', e)
      }
    },
    async loadMore() {
      if (this.loadingMore || this.requests.length >= this.requestTotal) return
      this.loadingMore = true
      try {
        const res = await getFriendRequestsPage(this.requestPage + 1, 20)
        const pageData = res.data || {}
        if (pageData.records && pageData.records.length > 0) {
          this.requestPage++
          this.requests = this.requests.concat(pageData.records)
        }
      } catch (e) {
        console.error('加载更多申请失败', e)
      } finally {
        this.loadingMore = false
      }
    },

    goSearch() {
      uni.navigateTo({ url: '/pages/user/search' })
    },
    createGroup() {
      uni.navigateTo({ url: '/pages/group/create' })
    },

    async approve(id) {
      try {
        await approveFriendRequest(id)
        uni.showToast({ title: '已同意', icon: 'success' })
        this.loadData()
      } catch (e) {
        uni.showToast({ title: e.message || '操作失败', icon: 'none' })
      }
    },

    async reject(id) {
      try {
        await rejectFriendRequest(id)
        uni.showToast({ title: '已拒绝', icon: 'success' })
        this.loadData()
      } catch (e) {
        uni.showToast({ title: e.message || '操作失败', icon: 'none' })
      }
    },

    async openChat(f) {
      try {
        const res = await createConversation(f.friendId)
        const nickname = encodeURIComponent(f.remark || f.nickname || '用户')
        const avatar = encodeURIComponent(f.face || '')
        uni.navigateTo({ url: `/pages/chat/detail?conversationId=${res.data.id}&targetUserId=${f.friendId}&nickname=${nickname}&avatar=${avatar}` })
      } catch (e) {
        uni.showToast({ title: e.message || '创建会话失败', icon: 'none' })
      }
    },

    showActions(f) {
      this.actionTarget = { ...f }
      this.showSheet = true
    },

    closeSheet() {
      this.showSheet = false
      this.remarkInput = ''
    },

    async setRemarkAction() {
      uni.showModal({
        title: '设置备注',
        editable: true,
        placeholderText: this.actionTarget.remark || this.actionTarget.nickname,
        success: async (res) => {
          if (res.confirm && res.content) {
            try {
              await setFriendRemark(this.actionTarget.friendId, res.content)
              uni.showToast({ title: '已设置', icon: 'success' })
              this.closeSheet()
              this.loadData()
            } catch (e) {
              uni.showToast({ title: e.message || '设置失败', icon: 'none' })
            }
          }
        }
      })
    },

    async toggleBlockAction() {
      try {
        if (this.actionTarget.isBlock) {
          await unblockFriend(this.actionTarget.friendId)
        } else {
          await blockFriend(this.actionTarget.friendId)
        }
        uni.showToast({ title: this.actionTarget.isBlock ? '已取消拉黑' : '已拉黑', icon: 'success' })
        this.closeSheet()
        this.loadData()
      } catch (e) {
        uni.showToast({ title: e.message || '操作失败', icon: 'none' })
      }
    },

    async deleteFriendAction() {
      uni.showModal({
        title: '确认删除',
        content: `确定删除好友 ${this.actionTarget.nickname}？`,
        success: async (res) => {
          if (res.confirm) {
            try {
              await deleteFriend(this.actionTarget.friendId)
              uni.showToast({ title: '已删除', icon: 'success' })
              this.closeSheet()
              this.loadData()
            } catch (e) {
              uni.showToast({ title: e.message || '删除失败', icon: 'none' })
            }
          }
        }
      })
    }
  }
}
</script>

<style>
.friend-page { padding: 0; background: #f5f5f5; min-height: 100vh; }

.search-bar {
  display: flex; align-items: center; margin: 12px 16px 0;
  padding: 10px 16px; background: #fff; border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.05);
}
.search-icon { font-size: 16px; margin-right: 8px; }
.search-text { font-size: 14px; color: #999; }

.create-group-bar {
  display: flex; align-items: center; margin: 8px 16px;
  padding: 12px 16px; background: #fff; border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.05);
}
.group-icon { font-size: 18px; margin-right: 10px; }
.group-text { flex: 1; font-size: 15px; color: #333; font-weight: 500; }
.group-arrow { font-size: 18px; color: #ccc; }

.section { background: #fff; margin: 0 0 12px; padding: 0 16px; }
.section-title { display: flex; align-items: center; padding: 14px 0 8px; font-size: 15px; font-weight: 600; color: #333; border-bottom: 1px solid #f0f0f0; }
.badge { margin-left: 8px; background: #ff3b30; color: #fff; font-size: 11px; padding: 1px 7px; border-radius: 10px; }

.request-item, .friend-item {
  display: flex; align-items: center; padding: 12px 0; border-bottom: 1px solid #f5f5f5;
}
.request-item:last-child, .friend-item:last-child { border-bottom: none; }

.avatar { width: 44px; height: 44px; border-radius: 50%; background: #e0e0e0; flex-shrink: 0; }

.request-info, .friend-info { flex: 1; margin-left: 12px; min-width: 0; }
.nickname { font-size: 15px; color: #333; display: block; }
.remark { font-size: 12px; color: #999; margin-top: 2px; display: block; }
.extra { font-size: 12px; color: #999; margin-top: 2px; display: block; }

.request-actions { display: flex; gap: 8px; flex-shrink: 0; }
.btn-approve { background: #007AFF; color: #fff; font-size: 13px; padding: 4px 14px; border-radius: 4px; border: none; }
.btn-reject { background: #f0f0f0; color: #666; font-size: 13px; padding: 4px 14px; border-radius: 4px; border: none; }

.friend-actions { display: flex; gap: 6px; flex-shrink: 0; }
.btn-chat { background: #e8f0fe; color: #007AFF; font-size: 13px; padding: 4px 12px; border-radius: 4px; border: none; }
.btn-more { background: transparent; color: #666; font-size: 16px; padding: 4px 8px; border: none; }

.empty { padding: 30px 0; text-align: center; color: #999; font-size: 13px; }

.action-sheet {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.4);
  display: flex; align-items: flex-end; z-index: 100;
}
.sheet-content {
  width: 100%; background: #fff; border-radius: 12px 12px 0 0; padding: 0 0 30px;
}
.sheet-title { text-align: center; font-size: 16px; font-weight: 600; padding: 16px; color: #333; border-bottom: 1px solid #f0f0f0; }
.sheet-item { text-align: center; padding: 14px; font-size: 15px; color: #333; border-bottom: 1px solid #f5f5f5; }
.sheet-item.danger { color: #ff3b30; }
.sheet-cancel { text-align: center; padding: 14px; font-size: 15px; color: #999; margin-top: 8px; background: #f5f5f5; }
</style>
