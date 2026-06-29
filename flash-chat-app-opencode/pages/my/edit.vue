<template>
  <view class="page">
    <view class="nav-bar">
      <text class="nav-back" @tap="goBack">&#x2039; 返回</text>
      <text class="nav-title">编辑资料</text>
      <text class="nav-save" @tap="save">保存</text>
    </view>

    <view class="form">
      <view class="form-item avatar-item" @tap="chooseAvatar">
        <text class="form-label">头像</text>
        <image class="avatar" :src="$resolveImage(form.face) || '/static/default-avatar.svg'" mode="aspectFill" />
      </view>

      <view class="form-item" @tap="editNickname">
        <text class="form-label">昵称</text>
        <view class="form-right">
          <text class="form-value">{{ form.nickname || '未设置' }}</text>
          <text class="form-arrow">&#x203A;</text>
        </view>
      </view>

      <view class="form-item" @tap="editSignature">
        <text class="form-label">个性签名</text>
        <view class="form-right">
          <text class="form-value form-value-sig">{{ form.signature || '未设置' }}</text>
          <text class="form-arrow">&#x203A;</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { updateUserInfo } from '../../api/auth'
import { getUserId, getUserInfo, setUserInfo, getToken } from '../../utils/storage'
import { useUserStore } from '../../store'
import { BASE_URL } from '../../config/env'
import { safeBack } from '../../utils/nav'

export default {
  data() {
    return {
      form: { nickname: '', face: '', signature: '' }
    }
  },
  onShow() {
    const info = getUserInfo() || {}
    this.form = { nickname: info.nickname || '', face: info.face || '', signature: info.signature || '' }
  },
  methods: {
    goBack() {
      safeBack()
    },
    chooseAvatar() {
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: async (res) => {
          const tempFile = res.tempFilePaths[0]
          try {
            const uploadRes = await uni.uploadFile({
              url: `${BASE_URL}/file/upload`,
              filePath: tempFile,
              name: 'file',
              header: { userId: getUserId(), Authorization: 'Bearer ' + getToken() }
            })
            const body = JSON.parse(uploadRes.data)
            if (body.data && body.data.url) {
              this.form.face = body.data.url
              uni.showToast({ title: '头像已更新', icon: 'success' })
            } else {
              uni.showToast({ title: '上传失败', icon: 'none' })
            }
          } catch (e) {
            uni.showToast({ title: '上传失败', icon: 'none' })
          }
        }
      })
    },
    editNickname() {
      uni.showModal({
        title: '修改昵称',
        editable: true,
        placeholderText: '输入新昵称',
        content: this.form.nickname,
        success: (res) => {
          if (res.confirm && res.content) {
            this.form.nickname = res.content.trim()
          }
        }
      })
    },
    editSignature() {
      uni.showModal({
        title: '修改个性签名',
        editable: true,
        placeholderText: '输入个性签名',
        content: this.form.signature,
        success: (res) => {
          if (res.confirm) {
            this.form.signature = (res.content || '').trim()
          }
        }
      })
    },
    async save() {
      if (!this.form.nickname) {
        uni.showToast({ title: '昵称不能为空', icon: 'none' })
        return
      }
      uni.showLoading({ title: '保存中...' })
      try {
        const res = await updateUserInfo({
          nickname: this.form.nickname,
          face: this.form.face,
          signature: this.form.signature,
        })
        const data = res?.data
        if (data) {
          setUserInfo(data)
          useUserStore().setUserInfo(data)
          uni.showToast({ title: '保存成功', icon: 'success' })
          setTimeout(() => safeBack(), 300)
        }
      } catch (e) {
        uni.showToast({ title: e?.message || '保存失败', icon: 'none' })
      } finally {
        uni.hideLoading()
      }
    }
  }
}
</script>

<style scoped>
.page { min-height: 100vh; background: #f5f5f5; }
.nav-bar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 60rpx 30rpx 20rpx; background: #fff;
  border-bottom: 1rpx solid #f0f0f0;
}
.nav-back { font-size: 28rpx; color: #007AFF; }
.nav-title { font-size: 32rpx; color: #333; font-weight: 500; }
.nav-save { font-size: 28rpx; color: #007AFF; font-weight: 500; }

.form { margin: 20rpx 20rpx 0; background: #fff; border-radius: 16rpx; overflow: hidden; }
.form-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 28rpx 30rpx; border-bottom: 1rpx solid #f5f5f5;
}
.form-item:last-child { border-bottom: none; }
.form-label { font-size: 30rpx; color: #333; flex-shrink: 0; margin-right: 20rpx; }
.form-right { display: flex; align-items: center; flex: 1; justify-content: flex-end; }
.form-value { font-size: 28rpx; color: #999; max-width: 400rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.form-value-sig { max-width: 350rpx; }
.form-arrow { font-size: 32rpx; color: #ccc; margin-left: 10rpx; }

.avatar-item { padding: 20rpx 30rpx; }
.avatar { width: 120rpx; height: 120rpx; border-radius: 50%; background: #e0e0e0; flex-shrink: 0; }
</style>
