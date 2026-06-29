<template>
  <view class="page">
    <view class="search-bar">
      <input
        class="search-input"
        :value="keyword"
        placeholder="搜索手机号或昵称"
        confirm-type="search"
        @input="onInput"
        @confirm="doSearch"
      />
      <view class="search-btn" @tap="doSearch">
        <text class="search-btn-text">搜索</text>
      </view>
    </view>

    <view v-if="searched && userList.length === 0 && !loading" class="empty">
      <text class="empty-text">未找到用户</text>
    </view>

    <view
      v-for="user in userList"
      :key="user.id"
      class="user-item"
      @tap="startChat(user)"
    >
      <image class="user-avatar" :src="$resolveImage(user.face) || '/static/default-avatar.svg'" mode="aspectFill" />
      <view class="user-info">
        <text class="user-name">{{ user.nickname || '用户' }}</text>
        <text class="user-mobile" v-if="user.mobile">{{ user.mobile }}</text>
      </view>
      <view class="user-actions">
        <button class="btn-add" @tap.stop="addFriend(user)">加好友</button>
        <button class="btn-chat" @tap.stop="startChat(user)">聊天</button>
      </view>
    </view>

    <view v-if="loading" class="loading">
      <text class="loading-text">搜索中...</text>
    </view>

    <view v-if="!keyword && !loading" class="tip">
      <text class="tip-text">输入手机号或昵称搜索用户</text>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { searchUser, createConversation, sendFriendRequest } from '../../api/chat'
import { getUserId } from '../../utils/storage'

const keyword = ref('')
const userList = ref([])
const loading = ref(false)
const searched = ref(false)

function onInput(e) {
  keyword.value = e.detail.value
  if (!keyword.value.trim()) {
    userList.value = []
    searched.value = false
  }
}

async function doSearch() {
  const kw = keyword.value.trim()
  if (!kw) {
    uni.showToast({ title: '请输入搜索关键词', icon: 'none' })
    return
  }
  loading.value = true
  searched.value = true
  try {
    const res = await searchUser(kw)
    const list = res?.data || []
    userList.value = list.filter((u) => u.id !== getUserId())
  } catch (e) {
    uni.showToast({ title: e?.message || '搜索失败', icon: 'none' })
    userList.value = []
  } finally {
    loading.value = false
  }
}

async function addFriend(user) {
  if (!user?.id) return
  uni.showModal({
    title: '添加好友',
    placeholderText: '输入验证消息（可选）',
    content: `向 ${user.nickname || '用户'} 发送好友申请`,
    editable: true,
    success: async (res) => {
      if (res.confirm) {
        try {
          await sendFriendRequest(user.id, res.content || '')
          uni.showToast({ title: '申请已发送', icon: 'success' })
        } catch (e) {
          uni.showToast({ title: e?.message || '发送失败', icon: 'none' })
        }
      }
    }
  })
}

async function startChat(user) {
  if (!user?.id) return
  if (user.id === getUserId()) {
    uni.showToast({ title: '不能和自己聊天', icon: 'none' })
    return
  }
  uni.showLoading({ title: '创建会话...' })
  try {
    const res = await createConversation(user.id)
    uni.hideLoading()
    if (res?.data) {
      const cId = res.data.id || res.data
      uni.navigateTo({
        url: `/pages/chat/detail?conversationId=${cId}&targetUserId=${user.id}&nickname=${encodeURIComponent(user.nickname || '用户')}&avatar=${encodeURIComponent(user.face || '')}`,
      })
    }
  } catch (e) {
    uni.hideLoading()
    uni.showToast({ title: e?.message || '创建会话失败', icon: 'none' })
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f5f5f5;
}

.search-bar {
  display: flex;
  align-items: center;
  padding: 16rpx 24rpx;
  background: #fff;
  border-bottom: 1rpx solid #f0f0f0;
}

.search-input {
  flex: 1;
  height: 64rpx;
  background: #f5f5f5;
  border-radius: 32rpx;
  padding: 0 24rpx;
  font-size: 28rpx;
}

.search-btn {
  margin-left: 16rpx;
  padding: 0 24rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.search-btn-text {
  font-size: 28rpx;
  color: #007aff;
}

.user-item {
  display: flex;
  align-items: center;
  padding: 24rpx 30rpx;
  background: #fff;
  border-bottom: 1rpx solid #f0f0f0;
}

.user-item:active {
  background: #f5f5f5;
}

.user-avatar {
  width: 88rpx;
  height: 88rpx;
  border-radius: 12rpx;
  margin-right: 24rpx;
  flex-shrink: 0;
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 30rpx;
  font-weight: 500;
  color: #333;
  display: block;
  margin-bottom: 6rpx;
}

.user-mobile {
  font-size: 26rpx;
  color: #999;
  display: block;
}

.user-actions { display: flex; gap: 8px; flex-shrink: 0; }
.btn-add { background: #007AFF; color: #fff; font-size: 12px; padding: 4px 10px; border-radius: 4px; border: none; line-height: 1.8; }
.btn-chat { background: #e8f0fe; color: #007AFF; font-size: 12px; padding: 4px 10px; border-radius: 4px; border: none; line-height: 1.8; }

.arrow {
  font-size: 36rpx;
  color: #ccc;
  margin-left: 16rpx;
}

.empty {
  text-align: center;
  padding: 200rpx 0;
}

.empty-text {
  font-size: 28rpx;
  color: #999;
}

.loading {
  text-align: center;
  padding: 40rpx;
}

.loading-text {
  font-size: 26rpx;
  color: #999;
}

.tip {
  text-align: center;
  padding: 200rpx 0;
}

.tip-text {
  font-size: 28rpx;
  color: #ccc;
}
</style>
