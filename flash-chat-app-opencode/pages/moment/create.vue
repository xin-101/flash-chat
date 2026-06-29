<template>
  <view class="page">
    <view class="nav-bar">
      <view class="nav-left" @tap="goBack">
        <text class="back-arrow">&#x2039;</text>
      </view>
      <text class="nav-title">发表动态</text>
      <view class="nav-right" @tap="submit">
        <text class="publish-btn">发布</text>
      </view>
    </view>

    <view class="editor">
      <textarea class="content-input" v-model="content" placeholder="说点什么..." maxlength="2000" />
      <view class="image-list" v-if="images.length">
        <image v-for="(img, i) in images" :key="i" class="preview-img"
               :src="img" @tap="removeImage(i)" />
      </view>
      <view class="add-image" @tap="chooseImage">
        <text class="add-icon">+</text>
      </view>
    </view>
  </view>
</template>

<script>
import { createMoment } from '../../api/chat'
import { BASE_URL } from '../../config/env'
import { getUserId, getToken } from '../../utils/storage'
import { safeBack } from '../../utils/nav'

export default {
  data() {
    return {
      content: '',
      images: []
    }
  },
  methods: {
    goBack() {
      safeBack()
    },
    chooseImage() {
      uni.chooseImage({
        count: 9 - this.images.length,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: (res) => {
          const paths = res.tempFilePaths || []
          this.images = [...this.images, ...paths]
        }
      })
    },
    removeImage(index) {
      this.images.splice(index, 1)
    },
    async submit() {
      if (!this.content.trim()) {
        uni.showToast({ title: '内容不能为空', icon: 'none' })
        return
      }
      uni.showLoading({ title: '发布中...' })
      let uploadedUrls = []
      if (this.images.length) {
        try {
          for (const path of this.images) {
            const res = await uni.uploadFile({
              url: `${BASE_URL}/file/upload`,
              filePath: path,
              name: 'file',
              header: { userId: getUserId(), Authorization: 'Bearer ' + getToken() }
            })
            const body = JSON.parse(res.data)
            if (body.data && body.data.url) uploadedUrls.push(body.data.url)
          }
        } catch (e) {
          uni.hideLoading()
          uni.showToast({ title: '图片上传失败', icon: 'none' })
          return
        }
      }

      try {
        await createMoment(this.content.trim(), uploadedUrls)
        uni.hideLoading()
        uni.showToast({ title: '发布成功', icon: 'success' })
        safeBack()
      } catch (e) {
        uni.hideLoading()
        uni.showToast({ title: e.message || '发布失败', icon: 'none' })
      }
    }
  }
}
</script>

<style>
.page { min-height: 100vh; background: #fff; }
.nav-bar { display: flex; align-items: center; padding: 60rpx 30rpx 20rpx; border-bottom: 1rpx solid #f0f0f0; }
.nav-left { width: 60rpx; }
.back-arrow { font-size: 40rpx; color: #333; }
.nav-title { flex: 1; text-align: center; font-size: 34rpx; font-weight: 600; color: #333; }
.nav-right { width: 120rpx; text-align: right; }
.publish-btn { color: #007AFF; font-size: 30rpx; font-weight: 500; }

.editor { padding: 24rpx; }
.content-input { width: 100%; min-height: 240rpx; font-size: 30rpx; color: #333; border: none; }
.image-list { display: flex; flex-wrap: wrap; gap: 12rpx; margin: 16rpx 0; }
.preview-img { width: 180rpx; height: 180rpx; border-radius: 8rpx; }
.add-image { width: 180rpx; height: 180rpx; border: 2rpx dashed #ccc; border-radius: 8rpx; display: flex; align-items: center; justify-content: center; }
.add-icon { font-size: 48rpx; color: #ccc; }
</style>
