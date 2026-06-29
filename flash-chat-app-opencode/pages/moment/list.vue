<template>
  <view class="page">
    <view class="nav-bar">
      <view class="nav-left" @tap="goBack">
        <text class="back-arrow">&#x2039;</text>
      </view>
      <text class="nav-title">朋友圈</text>
      <view class="nav-right" @tap="goCreate">
        <text class="camera-icon">&#x1F4F7;</text>
      </view>
    </view>

    <scroll-view class="moment-list" scroll-y :refresher-enabled="true"
                 :refresher-triggered="refreshing" @refresherrefresh="onRefresh"
                 @scrolltolower="loadMore">
      <view v-for="item in list" :key="item.id" class="moment-item">
        <view class="moment-header">
          <image class="avatar" :src="$resolveImage(item.face) || '/static/default-avatar.svg'" />
          <text class="nickname">{{ item.nickname }}</text>
          <text class="time">{{ formatChatTime(item.createTime) }}</text>
        </view>
        <text class="content" v-if="item.content">{{ item.content }}</text>
        <view class="images" v-if="item.imageList && item.imageList.length">
          <image v-for="(img, i) in item.imageList" :key="i" class="img"
                 :src="$resolveImage(img)" @tap="previewImage(item, i)" />
        </view>
        <view class="actions">
          <view class="like-area" @tap="toggleLike(item)">
            <text class="like-icon">{{ item.liked ? '❤️' : '🤍' }}</text>
            <text class="like-count">{{ item.likeCount || '' }}</text>
          </view>
          <view class="comment-area" @tap="focusComment(item)">
            <text class="comment-icon">💬</text>
          </view>
          <view class="delete-area" v-if="item.userId === myUserId" @tap="doDelete(item.id)">
            <text class="delete-icon">🗑️</text>
          </view>
        </view>
        <view class="comments" v-if="item.comments && item.comments.length">
          <view v-for="c in item.comments" :key="c.id" class="comment-item">
            <text class="comment-name">{{ c.nickname }}</text>
            <text v-if="c.replyUserId" class="comment-reply"> 回复 </text>
            <text v-if="c.replyUserId" class="comment-name">{{ c.replyNickname }}</text>
            <text class="comment-text">：{{ c.content }}</text>
            <text class="comment-time">{{ formatBriefTime(c.createTime) }}</text>
            <text v-if="c.userId === myUserId" class="comment-del" @tap="doDeleteComment(c.id)">×</text>
          </view>
        </view>
        <view class="comment-input-area" v-if="commentTarget && commentTarget.id === item.id">
          <input class="comment-input" v-model="commentText" placeholder="说点什么..."
                 @confirm="submitComment(item)" confirm-type="send" />
          <text class="comment-send" @tap="submitComment(item)">发送</text>
        </view>
      </view>

      <view v-if="loading" class="loading"><text>加载中...</text></view>
      <view v-if="!loading && list.length === 0" class="empty">
        <text>暂无动态</text>
        <text class="empty-sub">点击右上角发表你的第一条朋友圈</text>
      </view>
    </scroll-view>
  </view>
</template>

<script>
import { getMomentList, deleteMoment, likeMoment, unlikeMoment, commentMoment, deleteMomentComment } from '../../api/chat'
import { getUserInfo } from '../../utils/storage'
import { formatChatTime, formatBriefTime } from '../../utils/time'
import { safeBack } from '../../utils/nav'

export default {
  data() {
    return {
      list: [],
      page: 1,
      loading: false,
      refreshing: false,
      hasMore: true,
      myUserId: '',
      commentTarget: null,
      commentText: ''
    }
  },
  onLoad() {
    const user = getUserInfo() || {}
    this.myUserId = user.id || ''
    this.loadData()
  },
  onShow() {
    this.page = 1
    this.hasMore = true
    this.loadData()
  },
  methods: {
    formatChatTime, formatBriefTime,
    async loadData() {
      if (this.loading) return
      this.loading = true
      try {
        const res = await getMomentList(this.page, 10)
        const data = res.data || []
        if (this.page === 1) {
          this.list = data
        } else {
          this.list = [...this.list, ...data]
        }
        this.hasMore = data.length >= 10
      } catch (e) {
        console.error('加载朋友圈失败', e)
      } finally {
        this.loading = false
        this.refreshing = false
      }
    },
    onRefresh() {
      this.page = 1
      this.hasMore = true
      this.refreshing = true
      this.loadData()
    },
    loadMore() {
      if (!this.hasMore || this.loading) return
      this.page++
      this.loadData()
    },
    goBack() {
      safeBack()
    },
    goCreate() {
      uni.navigateTo({ url: '/pages/moment/create' })
    },
    async toggleLike(item) {
      try {
        if (item.liked) {
          await unlikeMoment(item.id)
          item.liked = false
          item.likeCount = Math.max(0, (item.likeCount || 1) - 1)
        } else {
          await likeMoment(item.id)
          item.liked = true
          item.likeCount = (item.likeCount || 0) + 1
        }
      } catch (e) {
        uni.showToast({ title: e.message || '操作失败', icon: 'none' })
      }
    },
    previewImage(item, index) {
      if (!item || !item.imageList) return
      uni.previewImage({ urls: item.imageList, current: index })
    },
    focusComment(item) {
      this.commentTarget = this.commentTarget?.id === item.id ? null : item
      this.commentText = ''
    },
    async submitComment(item) {
      if (!this.commentText.trim()) return
      try {
        await commentMoment(item.id, this.commentText.trim(), '')
        this.commentText = ''
        // 重新加载评论
        uni.showToast({ title: '评论成功', icon: 'success' })
        const res = await getMomentList(this.page, 10)
        const data = res.data || []
        if (this.page === 1) {
          this.list = data
        }
      } catch (e) {
        uni.showToast({ title: e.message || '评论失败', icon: 'none' })
      }
    },
    async doDelete(id) {
      uni.showModal({
        title: '确认删除',
        content: '确定删除这条朋友圈？',
        success: async (res) => {
          if (res.confirm) {
            try {
              await deleteMoment(id)
              this.list = this.list.filter(m => m.id !== id)
              uni.showToast({ title: '已删除', icon: 'success' })
            } catch (e) {
              uni.showToast({ title: e.message || '删除失败', icon: 'none' })
            }
          }
        }
      })
    },
    async doDeleteComment(commentId) {
      try {
        await deleteMomentComment(commentId)
        uni.showToast({ title: '已删除', icon: 'success' })
        const res = await getMomentList(this.page, 10)
        const data = res.data || []
        if (this.page === 1) {
          this.list = data
        }
      } catch (e) {
        uni.showToast({ title: e.message || '删除失败', icon: 'none' })
      }
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
.nav-right { width: 60rpx; text-align: right; }
.camera-icon { font-size: 36rpx; }

.moment-list { height: calc(100vh - 120rpx); }
.moment-item { background: #fff; margin: 16rpx 20rpx; border-radius: 12rpx; padding: 24rpx; }
.moment-header { display: flex; align-items: center; margin-bottom: 16rpx; }
.avatar { width: 72rpx; height: 72rpx; border-radius: 50%; }
.nickname { font-size: 30rpx; font-weight: 500; color: #333; margin-left: 16rpx; flex: 1; }
.time { font-size: 22rpx; color: #999; }
.content { font-size: 28rpx; color: #333; line-height: 1.6; margin-bottom: 16rpx; }
.images { display: flex; flex-wrap: wrap; gap: 8rpx; margin-bottom: 16rpx; }
.img { width: 200rpx; height: 200rpx; border-radius: 8rpx; background: #f0f0f0; }

.actions { display: flex; align-items: center; gap: 32rpx; padding: 16rpx 0 0; border-top: 1rpx solid #f0f0f0; }
.like-area, .comment-area, .delete-area { display: flex; align-items: center; }
.like-icon, .comment-icon, .delete-icon { font-size: 30rpx; }
.like-count { font-size: 24rpx; color: #999; margin-left: 4rpx; }

.comments { background: #f9f9f9; border-radius: 8rpx; padding: 12rpx; margin-top: 12rpx; }
.comment-item { font-size: 26rpx; color: #333; padding: 4rpx 0; display: flex; flex-wrap: wrap; align-items: center; }
.comment-name { color: #007AFF; }
.comment-reply { color: #999; }
.comment-text { }
.comment-time { font-size: 20rpx; color: #ccc; margin-left: 8rpx; }
.comment-del { color: #ff3b30; margin-left: 8rpx; font-size: 28rpx; }

.comment-input-area { display: flex; align-items: center; margin-top: 12rpx; gap: 8rpx; }
.comment-input { flex: 1; border: 1rpx solid #e0e0e0; border-radius: 8rpx; padding: 8rpx 12rpx; font-size: 26rpx; }
.comment-send { color: #007AFF; font-size: 28rpx; }

.loading, .empty { text-align: center; padding: 60rpx 0; color: #999; font-size: 28rpx; }
.empty-sub { display: block; font-size: 24rpx; color: #ccc; margin-top: 8rpx; }
</style>
