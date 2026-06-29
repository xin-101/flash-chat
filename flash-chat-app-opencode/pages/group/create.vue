<template>
  <view class="page">
    <view class="nav-bar">
      <view class="nav-left" @tap="goBack">
        <text class="back-arrow">&#x2039;</text>
      </view>
      <text class="nav-title">创建群聊</text>
      <view class="nav-right" @tap="submit">
        <text class="create-btn" :class="{ disabled: !groupName.trim() || selectedIds.length === 0 }">创建</text>
      </view>
    </view>

    <view class="group-name-section">
      <input class="name-input" v-model="groupName" placeholder="输入群聊名称" maxlength="50" />
    </view>

    <view class="section-title">
      <text>选择成员（{{ selectedIds.length }}）</text>
    </view>

    <scroll-view class="member-list" scroll-y>
      <view v-for="f in friends" :key="f.id" class="member-item" @tap="toggle(f)">
        <view class="checkbox" :class="{ checked: selectedIds.includes(f.friendId) }">
          <text v-if="selectedIds.includes(f.friendId)">✓</text>
        </view>
        <image class="avatar" :src="$resolveImage(f.face) || '/static/default-avatar.svg'" />
        <text class="nickname">{{ f.remark || f.nickname }}</text>
      </view>

      <view v-if="friends.length === 0" class="empty">
        <text>暂无好友，请先添加好友</text>
      </view>
    </scroll-view>
  </view>
</template>

<script>
import { getFriendList, createGroup } from '../../api/chat'
import { safeBack } from '../../utils/nav'

export default {
  data() {
    return {
      friends: [],
      selectedIds: [],
      groupName: '',
    }
  },
  onShow() {
    this.loadFriends()
  },
  methods: {
    async loadFriends() {
      try {
        const res = await getFriendList()
        this.friends = res.data || []
      } catch (e) {
        console.error('加载好友失败', e)
      }
    },
    toggle(f) {
      const idx = this.selectedIds.indexOf(f.friendId)
      if (idx > -1) {
        this.selectedIds.splice(idx, 1)
      } else {
        this.selectedIds.push(f.friendId)
      }
    },
    goBack() {
      safeBack()
    },
    async submit() {
      if (!this.groupName.trim()) {
        uni.showToast({ title: '请输入群名称', icon: 'none' })
        return
      }
      if (this.selectedIds.length === 0) {
        uni.showToast({ title: '请选择至少一个成员', icon: 'none' })
        return
      }
      try {
        const res = await createGroup(this.groupName.trim(), this.selectedIds)
        if (res.data) {
          uni.showToast({ title: '创建成功', icon: 'success' })
          safeBack()
        }
      } catch (e) {
        uni.showToast({ title: e.message || '创建失败', icon: 'none' })
      }
    },
  },
}
</script>

<style>
.page { min-height: 100vh; background: #f5f5f5; }
.nav-bar { display: flex; align-items: center; padding: 60rpx 30rpx 20rpx; background: #fff; border-bottom: 1rpx solid #f0f0f0; }
.nav-left { width: 60rpx; }
.back-arrow { font-size: 40rpx; color: #333; }
.nav-title { flex: 1; text-align: center; font-size: 34rpx; font-weight: 600; color: #333; }
.nav-right { width: 120rpx; text-align: right; }
.create-btn { color: #007AFF; font-size: 30rpx; font-weight: 500; }
.create-btn.disabled { opacity: 0.4; }

.group-name-section { background: #fff; padding: 20rpx 30rpx; margin-bottom: 16rpx; }
.name-input { height: 72rpx; font-size: 30rpx; border-bottom: 1rpx solid #f0f0f0; }

.section-title { padding: 16rpx 30rpx; font-size: 28rpx; color: #666; background: #f5f5f5; }
.member-list { height: calc(100vh - 340rpx); }
.member-item { display: flex; align-items: center; padding: 20rpx 30rpx; background: #fff; border-bottom: 1rpx solid #f5f5f5; }
.member-item:active { background: #f9f9f9; }
.checkbox { width: 40rpx; height: 40rpx; border-radius: 50%; border: 2rpx solid #ccc; display: flex; align-items: center; justify-content: center; margin-right: 20rpx; flex-shrink: 0; }
.checkbox.checked { background: #007AFF; border-color: #007AFF; color: #fff; font-size: 24rpx; }
.avatar { width: 72rpx; height: 72rpx; border-radius: 50%; margin-right: 20rpx; }
.nickname { font-size: 30rpx; color: #333; flex: 1; }
.empty { text-align: center; padding: 100rpx 0; color: #999; font-size: 28rpx; }
</style>
