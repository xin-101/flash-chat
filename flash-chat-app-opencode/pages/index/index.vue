<template>
  <view class="page">
    <view class="nav-bar">
      <view class="nav-bar-left"></view>
      <text class="nav-bar-title">Flash Chat</text>
      <view class="nav-bar-right" @tap="goSearch">
        <text class="nav-icon">&#x1F50D;</text>
      </view>
    </view>

    <scroll-view
      class="chat-list"
      scroll-y
      :refresher-enabled="true"
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
    >
      <view
        v-for="item in conversationList"
        :key="item.id"
        class="chat-item"
        @tap="goChat(item)"
      >
        <view class="avatar-wrap">
          <image class="avatar" :src="$resolveImage(item.face) || '/static/default-avatar.svg'" mode="aspectFill" />
          <view v-if="item.unreadCount > 0" class="unread-dot"></view>
        </view>
        <view class="chat-info">
          <view class="chat-info-top">
            <text class="chat-name">
              <text v-if="item.type == 2" class="group-badge">群</text>
              {{ item.name || '用户' }}
            </text>
            <text class="chat-time">{{ formatBriefTime(item.lastMessageTime) }}</text>
          </view>
          <view class="chat-info-bottom">
            <text class="chat-msg">{{ item.lastMessage || '暂无消息' }}</text>
            <view v-if="item.unreadCount > 0" class="badge">
              <text class="badge-text">{{ item.unreadCount > 99 ? '99+' : item.unreadCount }}</text>
            </view>
          </view>
        </view>
      </view>

      <view v-if="conversationList.length === 0 && !loading" class="empty">
        <text class="empty-icon">&#x1F4AC;</text>
        <text class="empty-text">暂无会话</text>
        <text class="empty-sub">点击右上角搜索开始聊天</text>
      </view>

      <view v-if="loading" class="loading">
        <text class="loading-text">加载中...</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useChatStore } from '../../store'
import { formatBriefTime } from '../../utils/time'
import { onWebSocketMessage } from '../../utils/websocket'

const chatStore = useChatStore()
const refreshing = ref(false)

const conversationList = ref([])
const loading = ref(false)

async function loadData() {
  loading.value = true
  try {
    await chatStore.loadConversationList()
    conversationList.value = [...chatStore.conversationList]
  } catch (e) {
    console.error('加载会话列表失败:', e)
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

async function onRefresh() {
  refreshing.value = true
  await loadData()
  uni.stopPullDownRefresh()
}

function goSearch() {
  uni.navigateTo({ url: '/pages/user/search' })
}

function goChat(item) {
  const params = [
    `conversationId=${item.id}`,
    `nickname=${encodeURIComponent(item.name || '用户')}`,
    `avatar=${encodeURIComponent(item.face || '')}`,
    `type=${item.type || 1}`,
  ].join('&')
  uni.navigateTo({ url: `/pages/chat/detail?${params}` })
}

function onWsMessage(data) {
  if (data.type === 'message') loadData()
}

onMounted(() => loadData())
onShow(() => loadData())

onWebSocketMessage(onWsMessage)
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f5f5f5;
}

.nav-bar {
  display: flex;
  align-items: center;
  padding: 60rpx 30rpx 20rpx;
  background: #fff;
  border-bottom: 1rpx solid #f0f0f0;
}

.nav-bar-left {
  width: 60rpx;
}

.nav-bar-title {
  flex: 1;
  text-align: center;
  font-size: 34rpx;
  font-weight: 600;
  color: #333;
}

.nav-bar-right {
  width: 60rpx;
  display: flex;
  justify-content: flex-end;
}

.nav-icon {
  font-size: 36rpx;
}

.chat-list {
  height: calc(100vh - 120rpx);
}

.chat-item {
  display: flex;
  align-items: center;
  padding: 24rpx 30rpx;
  background: #fff;
  border-bottom: 1rpx solid #f0f0f0;
}

.chat-item:active {
  background: #f5f5f5;
}

.avatar-wrap {
  position: relative;
  margin-right: 24rpx;
  flex-shrink: 0;
}

.avatar {
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
}

.unread-dot {
  position: absolute;
  top: 0;
  right: 0;
  width: 20rpx;
  height: 20rpx;
  background: #ff3b30;
  border-radius: 50%;
  border: 3rpx solid #fff;
}

.chat-info {
  flex: 1;
  min-width: 0;
}

.chat-info-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10rpx;
}

.chat-name {
  font-size: 30rpx;
  font-weight: 500;
  color: #333;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.group-badge {
  display: inline-block;
  font-size: 20rpx;
  color: #fff;
  background: #07c160;
  border-radius: 4rpx;
  padding: 2rpx 8rpx;
  margin-right: 6rpx;
  vertical-align: middle;
}

.chat-time {
  font-size: 24rpx;
  color: #999;
  margin-left: 16rpx;
  flex-shrink: 0;
}

.chat-info-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chat-msg {
  font-size: 26rpx;
  color: #999;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.badge {
  background: #ff3b30;
  border-radius: 20rpx;
  min-width: 36rpx;
  height: 36rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 10rpx;
  margin-left: 16rpx;
  flex-shrink: 0;
}

.badge-text {
  color: #fff;
  font-size: 20rpx;
}

.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 200rpx 0;
}

.empty-icon {
  font-size: 80rpx;
  margin-bottom: 20rpx;
  opacity: 0.4;
}

.empty-text {
  font-size: 30rpx;
  color: #999;
  margin-bottom: 12rpx;
}

.empty-sub {
  font-size: 26rpx;
  color: #ccc;
}

.loading {
  display: flex;
  justify-content: center;
  padding: 40rpx;
}

.loading-text {
  font-size: 26rpx;
  color: #999;
}
</style>
