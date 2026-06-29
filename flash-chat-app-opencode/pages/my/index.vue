<template>
  <view class="page">
    <view class="nav-bar">
      <text class="nav-bar-title">我的</text>
    </view>

    <view class="profile-card">
      <view class="profile-accent"></view>
      <view class="profile-body">
        <image class="profile-avatar" :src="$resolveImage(userInfo.face) || '/static/default-avatar.svg'" mode="aspectFill" />
        <view class="profile-info">
          <text class="profile-name">{{ userInfo.nickname || '用户' }}</text>
          <text class="profile-id">闪聊号：{{ userInfo.flashChatNum || userInfo.id || '-' }}</text>
          <text class="profile-sig" v-if="userInfo.signature">{{ userInfo.signature }}</text>
        </view>
      </view>
    </view>

    <view class="section">
      <view class="section-item" @tap="goMoment">
        <text class="section-label">朋友圈</text>
        <text class="section-arrow">&#x203A;</text>
      </view>
      <view class="section-item" @tap="editFlashChatNum">
        <text class="section-label">修改闪聊号</text>
        <text class="section-value">{{ userInfo.flashChatNum || '' }}</text>
        <text class="section-arrow">&#x203A;</text>
      </view>
      <view class="section-item" @tap="editProfile">
        <text class="section-label">编辑资料</text>
        <text class="section-arrow">&#x203A;</text>
      </view>
      <view class="section-item" @tap="tip('账号与安全')">
        <text class="section-label">账号与安全</text>
        <text class="section-arrow">&#x203A;</text>
      </view>
      <view class="section-item" @tap="tip('消息通知')">
        <text class="section-label">消息通知</text>
        <text class="section-arrow">&#x203A;</text>
      </view>
      <view class="section-item" @tap="tip('隐私设置')">
        <text class="section-label">隐私设置</text>
        <text class="section-arrow">&#x203A;</text>
      </view>
      <view class="section-item" @tap="tip('关于我们')">
        <text class="section-label">关于我们</text>
        <text class="section-arrow">&#x203A;</text>
      </view>
    </view>

    <view class="logout-btn" @tap="handleLogout">
      <text class="logout-text">退出登录</text>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getUserInfo, clearAuth, setUserInfo } from '../../utils/storage'
import { useUserStore } from '../../store'
import { logout, updateFlashChatNum } from '../../api/auth'
import { closeWebSocket } from '../../utils/websocket'

const userStore = useUserStore()
const userInfo = ref({})

onShow(() => {
  userInfo.value = getUserInfo() || {}
})

function goMoment() {
  uni.navigateTo({ url: '/pages/moment/list' })
}

function editFlashChatNum() {
  uni.showModal({
    title: '修改闪聊号',
    content: '闪聊号一个月仅可修改一次',
    editable: true,
    placeholderText: '输入新的闪聊号',
    success: async (res) => {
      if (!res.confirm || !res.content) return
      const newNum = res.content.trim()
      if (!newNum) {
        uni.showToast({ title: '闪聊号不能为空', icon: 'none' })
        return
      }
      try {
        const result = await updateFlashChatNum(newNum)
        const data = result?.data
        if (data) {
          userInfo.value = data
          setUserInfo(data)
          uni.showToast({ title: '修改成功', icon: 'success' })
        }
      } catch (e) {
        uni.showToast({ title: e?.message || '修改失败', icon: 'none' })
      }
    }
  })
}

function editProfile() {
  uni.navigateTo({ url: '/pages/my/edit' })
}

function tip(msg) {
  uni.showToast({ title: `${msg}功能开发中`, icon: 'none' })
}

function handleLogout() {
  uni.showModal({
    title: '确认退出',
    content: '确定要退出登录吗？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await logout()
        } catch (e) {
          // 即使接口失败也退出
        }
        closeWebSocket(true)
        clearAuth()
        userStore.clearUserInfo()
        uni.reLaunch({ url: '/pages/login/index' })
      }
    },
  })
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f5f5f5;
}

.nav-bar {
  padding: 60rpx 30rpx 20rpx;
  background: #fff;
  border-bottom: 1rpx solid #f0f0f0;
  text-align: center;
}

.nav-bar-title {
  font-size: 32rpx;
  color: #333;
  font-weight: 500;
}

.profile-card {
  margin: 20rpx 20rpx 0;
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;
}

.profile-accent {
  height: 8rpx;
  background: linear-gradient(135deg, #667eea, #764ba2);
}

.profile-body {
  display: flex;
  align-items: center;
  padding: 32rpx;
}

.profile-avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  flex-shrink: 0;
}

.profile-info {
  flex: 1;
  margin-left: 28rpx;
}

.profile-name {
  font-size: 36rpx;
  font-weight: 600;
  color: #333;
  display: block;
  margin-bottom: 8rpx;
}

.profile-id {
  font-size: 26rpx;
  color: #888;
  display: block;
  margin-bottom: 4rpx;
}

.profile-sig {
  font-size: 24rpx;
  color: #bbb;
  display: block;
}

.section {
  margin: 20rpx 20rpx 0;
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;
}

.section-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 30rpx 30rpx;
  border-bottom: 1rpx solid #f5f5f5;
}

.section-item:last-child {
  border-bottom: none;
}

.section-item:active {
  background: #fafafa;
}

.section-label {
  font-size: 30rpx;
  color: #333;
}

.section-value {
  font-size: 26rpx;
  color: #999;
  flex: 1;
  text-align: right;
  margin-right: 12rpx;
}
.section-arrow {
  font-size: 32rpx;
  color: #ccc;
}

.logout-btn {
  margin: 60rpx 40rpx;
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ff4d4f;
  border-radius: 44rpx;
}

.logout-btn:active {
  background: #e84343;
}

.logout-text {
  font-size: 32rpx;
  color: #fff;
  font-weight: 500;
}
</style>
