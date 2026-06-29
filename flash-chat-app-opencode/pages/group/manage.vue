<template>
  <view class="page">
    <view class="nav-bar">
      <view class="nav-left" @tap="goBack">
        <text class="back-arrow">&#x2039;</text>
      </view>
      <text class="nav-title">群管理</text>
      <view class="nav-right"></view>
    </view>

    <view class="group-info">
      <text class="group-name">{{ groupName }}</text>
      <text class="group-count">{{ members.length }} 人</text>
    </view>

    <view class="section">
      <view class="section-title"><text>群成员</text></view>
      <view class="member-list">
        <view v-for="m in members" :key="m.userId" class="member-item">
          <image class="avatar" :src="$resolveImage(m.face) || '/static/default-avatar.svg'" />
          <view class="member-info">
            <text class="member-name">{{ m.nickname }}</text>
            <text v-if="m.userId === ownerId" class="owner-tag">群主</text>
          </view>
          <view v-if="isOwner && m.userId !== myUserId" class="member-actions" @click.stop>
            <button class="btn-remove" @click="removeMember(m.userId)">移除</button>
            <button class="btn-transfer" @click="transferOwner(m.userId)">转让</button>
          </view>
        </view>
      </view>
    </view>

    <view class="section" v-if="isOwner">
      <view class="section-title"><text>群设置</text></view>
      <view class="setting-item" @click="editGroupName">
        <text class="setting-label">修改群名称</text>
        <text class="setting-arrow">&#x203A;</text>
      </view>
      <view class="setting-item" @click="inviteMembers">
        <text class="setting-label">邀请成员</text>
        <text class="setting-arrow">&#x203A;</text>
      </view>
    </view>

    <view v-if="!isOwner" class="leave-btn" @click="leaveGroup">
      <text>退出群聊</text>
    </view>
  </view>
</template>

<script>
import {
  getGroupMemberDetail, getGroupInfo, updateGroupName,
  removeGroupMember, transferGroup, getFriendList, inviteGroupMembers
} from '../../api/chat'
import { getUserId, getUserInfo } from '../../utils/storage'
import { safeBack } from '../../utils/nav'

export default {
  data() {
    return {
      conversationId: '',
      groupName: '',
      members: [],
      ownerId: '',
      myUserId: '',
    }
  },
  computed: {
    isOwner() {
      return this.myUserId === this.ownerId
    }
  },
  onLoad(options) {
    this.conversationId = options.conversationId || ''
    this.groupName = decodeURIComponent(options.name || '群聊')
    this.myUserId = getUserInfo()?.id || ''
    this.loadData()
  },
  methods: {
    async loadData() {
      try {
        const [memberRes, infoRes] = await Promise.all([
          getGroupMemberDetail(this.conversationId),
          getGroupInfo(this.conversationId),
        ])
        this.members = memberRes.data || []
        this.ownerId = infoRes.data?.ownerId || ''
      } catch (e) {
        console.error('加载群数据失败', e)
      }
    },
    goBack() {
      safeBack()
    },
    removeMember(userId) {
      uni.showModal({
        title: '确认移除',
        content: '确定移除该成员？',
        success: async (res) => {
          if (res.confirm) {
            try {
              await removeGroupMember(this.conversationId, userId)
              uni.showToast({ title: '已移除', icon: 'success' })
              this.loadData()
            } catch (e) {
              uni.showToast({ title: e.message || '操作失败', icon: 'none' })
            }
          }
        }
      })
    },
    transferOwner(userId) {
      uni.showModal({
        title: '确认转让',
        content: '确定将群主转让给该成员？',
        success: async (res) => {
          if (res.confirm) {
            try {
              await transferGroup(this.conversationId, userId)
              this.ownerId = userId
              uni.showToast({ title: '已转让', icon: 'success' })
            } catch (e) {
              uni.showToast({ title: e.message || '操作失败', icon: 'none' })
            }
          }
        }
      })
    },
    editGroupName() {
      uni.showModal({
        title: '修改群名称',
        editable: true,
        placeholderText: this.groupName,
        success: async (res) => {
          if (res.confirm && res.content.trim()) {
            try {
              await updateGroupName(this.conversationId, res.content.trim())
              this.groupName = res.content.trim()
              uni.showToast({ title: '已修改', icon: 'success' })
            } catch (e) {
              uni.showToast({ title: e.message || '修改失败', icon: 'none' })
            }
          }
        }
      })
    },
    inviteMembers() {
      getFriendList().then(friendRes => {
        const friends = friendRes.data || []
        const currentIds = this.members.map(m => m.userId)
        const available = friends.filter(f => !currentIds.includes(f.friendId))
        if (available.length === 0) {
          uni.showToast({ title: '没有可邀请的好友', icon: 'none' })
          return
        }
        const items = available.map(f => f.remark || f.nickname)
        uni.showActionSheet({
          itemList: items,
          success: async (res2) => {
            const selected = available[res2.tapIndex]
            if (selected) {
              try {
                await inviteGroupMembers(this.conversationId, [selected.friendId])
                uni.showToast({ title: '已邀请', icon: 'success' })
                this.loadData()
              } catch (e) {
                uni.showToast({ title: e.message || '邀请失败', icon: 'none' })
              }
            }
          }
        })
      }).catch(e => {
        uni.showToast({ title: e.message || '获取好友列表失败', icon: 'none' })
      })
    },
    leaveGroup() {
      uni.showModal({
        title: '退出群聊',
        content: '确定退出该群聊？',
        success: async (res) => {
          if (res.confirm) {
            try {
              await removeGroupMember(this.conversationId, this.myUserId)
              uni.showToast({ title: '已退出', icon: 'success' })
              safeBack()
            } catch (e) {
              uni.showToast({ title: e.message || '操作失败', icon: 'none' })
            }
          }
        }
      })
    }
  }
}
</script>

<style>
.page { min-height: 100vh; background: #f5f5f5; }
.nav-bar { display: flex; align-items: center; padding: 60rpx 30rpx 20rpx; background: #fff; border-bottom: 1rpx solid #f0f0f0; }
.nav-left { width: 60rpx; }
.back-arrow { font-size: 40rpx; color: #333; }
.nav-title { flex: 1; text-align: center; font-size: 34rpx; font-weight: 600; color: #333; }
.nav-right { width: 60rpx; }

.group-info { background: #fff; padding: 30rpx; text-align: center; border-bottom: 1rpx solid #f0f0f0; }
.group-name { font-size: 36rpx; font-weight: 600; color: #333; display: block; }
.group-count { font-size: 26rpx; color: #999; margin-top: 8rpx; display: block; }

.section { background: #fff; margin: 16rpx 0; }
.section-title { padding: 20rpx 30rpx 12rpx; font-size: 28rpx; color: #666; border-bottom: 1rpx solid #f5f5f5; }
.member-item { display: flex; align-items: center; padding: 20rpx 30rpx; border-bottom: 1rpx solid #f5f5f5; }
.avatar { width: 72rpx; height: 72rpx; border-radius: 50%; margin-right: 20rpx; }
.member-info { flex: 1; display: flex; align-items: center; }
.member-name { font-size: 30rpx; color: #333; }
.owner-tag { font-size: 20rpx; color: #ff9500; background: #fff6e5; padding: 2rpx 12rpx; border-radius: 6rpx; margin-left: 12rpx; }
.member-actions { display: flex; gap: 8rpx; }
.btn-remove { background: #ff4d4f; color: #fff; font-size: 24rpx; padding: 4rpx 16rpx; border-radius: 6rpx; border: none; }
.btn-transfer { background: #f0f0f0; color: #333; font-size: 24rpx; padding: 4rpx 16rpx; border-radius: 6rpx; border: none; }

.setting-item { display: flex; align-items: center; justify-content: space-between; padding: 24rpx 30rpx; border-bottom: 1rpx solid #f5f5f5; }
.setting-label { font-size: 30rpx; color: #333; }
.setting-arrow { font-size: 32rpx; color: #ccc; }

.leave-btn { margin: 60rpx 40rpx; height: 88rpx; display: flex; align-items: center; justify-content: center; background: #ff4d4f; border-radius: 44rpx; color: #fff; font-size: 32rpx; }
</style>
