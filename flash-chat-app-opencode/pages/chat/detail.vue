<template>
  <view class="page">
    <view class="header">
      <view class="header-left" @tap="goBack">
        <text class="back-icon">&#x2190;</text>
      </view>
      <view class="header-center" @tap="conversationType == 2 ? goManage() : void 0">
        <template v-if="conversationType == 2">
          <image class="header-avatar" src="/static/default-avatar.svg" mode="aspectFill" />
          <view class="header-info">
            <text class="header-name">{{ targetName }}</text>
            <text class="header-sub">群聊</text>
          </view>
        </template>
        <template v-else>
          <image class="header-avatar" :src="$resolveImage(targetAvatar) || '/static/default-avatar.svg'" mode="aspectFill" />
          <view class="header-info">
            <text class="header-name">{{ targetName }}</text>
          </view>
        </template>
      </view>
      <view class="header-right" @tap="goManage" v-if="conversationType == 2">
        <text class="manage-icon">...</text>
      </view>
    </view>

    <view class="msg-list-wrap">
      <scroll-view
        class="msg-list"
        scroll-y
        :scroll-into-view="scrollIntoView"
        :scroll-with-animation="true"
        @scrolltoupper="loadMore"
        @scroll="onScroll"
      >
        <view v-if="loadingMore" class="load-more">
          <text class="load-more-text">加载中...</text>
        </view>

        <view
          v-for="(msg, index) in messageList"
          :key="msg.id"
          :id="index === messageList.length - 1 ? 'last-msg' : ''"
          class="msg-row"
          :class="{ 'msg-row-me': msg.me }"
        >
          <image v-if="!msg.me" class="msg-avatar"
                 :src="$resolveImage(targetAvatar) || '/static/default-avatar.svg'"
                 mode="aspectFill" />
          <image v-else class="msg-avatar"
                 :src="$resolveImage(myAvatar) || '/static/default-avatar.svg'"
                 mode="aspectFill" />
          <view class="msg-body" :class="{ 'msg-body-me': msg.me }">
            <view class="msg-bubble" :class="{ 'msg-bubble-me': msg.me }">
              <image v-if="msg.type === 2 || msg.msgType === 2" class="msg-image"
                     :src="$resolveImage(msg.content)" mode="widthFix" @tap="previewImage(msg.content)" />
              <text v-else class="msg-text" :class="{ 'msg-text-me': msg.me }">
                {{ msg.content }}
              </text>
            </view>
            <text v-if="msg._sending" class="msg-status">发送中...</text>
          </view>
        </view>

        <view v-if="messageList.length === 0 && !loading" class="empty">
          <text class="empty-text">暂无消息，发送第一条开始聊天</text>
        </view>
      </scroll-view>
    </view>

    <view class="input-bar">
      <view class="img-btn" @tap="chooseImage">
        <text class="img-btn-icon">&#x1F5BC;</text>
      </view>
      <input
        class="msg-input"
        :value="inputText"
        placeholder="输入消息..."
        confirm-type="send"
        @input="onInput"
        @confirm="send"
      />
      <view
        class="send-btn"
        :class="{ 'send-btn-disabled': !inputText.trim() || sending }"
        @tap="send"
      >
        <text class="send-btn-text">发送</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { getMessageList, sendTextMessage, sendImageMessage, markConversationRead } from '../../api/chat'
import { getUserId, getUserInfo, getToken } from '../../utils/storage'
import { formatMessageTime } from '../../utils/time'
import { onWebSocketMessage, offWebSocketMessage } from '../../utils/websocket'
import { BASE_URL } from '../../config/env'
import { safeBack } from '../../utils/nav'

const props = ref({
  conversationId: '',
  targetUserId: '',
  nickname: '用户',
  avatar: '',
})

const messageList = ref([])
const inputText = ref('')
const sending = ref(false)
const loading = ref(false)
const loadingMore = ref(false)
const scrollIntoView = ref('')
const page = ref(1)
const pageSize = 20
const hasMore = ref(true)
const myAvatar = ref('')
const targetName = ref('')
const targetAvatar = ref('')
const conversationType = ref(1)

onMounted(() => {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  const options = currentPage.$page?.options || currentPage.options || {}

  props.value.conversationId = options.conversationId || ''
  props.value.targetUserId = options.targetUserId || ''
  props.value.nickname = decodeURIComponent(options.nickname || '用户')
  props.value.avatar = decodeURIComponent(options.avatar || '')
  conversationType.value = parseInt(options.type || '1')

  targetName.value = props.value.nickname
  targetAvatar.value = props.value.avatar

  const user = getUserInfo()
  if (user?.face) myAvatar.value = user.face

  uni.setNavigationBarTitle({ title: targetName.value })
  loadMessages()

  if (props.value.conversationId) {
    markConversationRead(props.value.conversationId).catch(() => {})
  }
})

function onInput(e) {
  inputText.value = e.detail.value
}

async function loadMessages(isLoadMore = false) {
  if (isLoadMore) {
    if (!hasMore.value || loadingMore.value) return
    loadingMore.value = true
    page.value++
  } else {
    loading.value = true
    page.value = 1
    hasMore.value = true
  }

  try {
    const res = await getMessageList(props.value.conversationId, page.value, pageSize)
    const list = res?.data || []

    if (isLoadMore) {
      messageList.value = [...list.reverse(), ...messageList.value]
    } else {
      messageList.value = list.reverse()
      nextTick(() => scrollToBottom())
    }

    if (list.length < pageSize) hasMore.value = false
  } catch (e) {
    console.error('获取消息失败:', e)
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

function loadMore() {
  loadMessages(true)
}

async function chooseImage() {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: async (res) => {
      const path = res.tempFilePaths[0]
      if (!path) return
      sending.value = true
      try {
        const uploadRes = await uni.uploadFile({
          url: `${BASE_URL}/file/upload`,
          filePath: path,
          name: 'file',
          header: { userId: getUserId(), Authorization: 'Bearer ' + getToken() }
        })
        const body = JSON.parse(uploadRes.data)
        if (body.data && body.data.url) {
          await sendImageMessage(props.value.conversationId, body.data.url)
        }
      } catch (e) {
        console.error('发送图片失败', e)
        uni.showToast({ title: '图片发送失败', icon: 'none' })
      } finally {
        sending.value = false
      }
    }
  })
}

async function send() {
  const content = inputText.value.trim()
  if (!content || sending.value) return

  sending.value = true
  const tempId = 'temp_' + Date.now()

  const tempMsg = {
    id: tempId,
    content,
    senderId: getUserId(),
    me: true,
    createTime: new Date().toISOString(),
    _sending: true,
  }
  messageList.value.push(tempMsg)
  inputText.value = ''
  nextTick(() => scrollToBottom())

  try {
    const res = await sendTextMessage(props.value.conversationId, content)
    const idx = messageList.value.findIndex((m) => m.id === tempId)
    if (idx !== -1 && res?.data) {
      messageList.value[idx] = { ...res.data, me: true }
    }
    nextTick(() => scrollToBottom())
  } catch (e) {
    console.error('发送失败:', e)
    messageList.value = messageList.value.filter((m) => m.id !== tempId)
    uni.showToast({ title: e?.message || '发送失败', icon: 'none' })
  } finally {
    sending.value = false
  }
}

function scrollToBottom() {
  scrollIntoView.value = ''
  nextTick(() => {
    scrollIntoView.value = 'last-msg'
  })
}

function previewImage(src) {
  if (src) uni.previewImage({ urls: [src] })
}

function goBack() {
  safeBack()
}

function goManage() {
  if (conversationType.value !== 2) return
  const params = [
    `conversationId=${props.value.conversationId}`,
    `name=${encodeURIComponent(targetName.value)}`,
  ].join('&')
  uni.navigateTo({ url: `/pages/group/manage?${params}` })
}

function onWsMessage(data) {
  if (data.type === 'message' && data.conversationId === props.value.conversationId) {
    if (messageList.value.some((m) => m.id === data.id)) return
    messageList.value.push({
      ...data,
      me: data.senderId === getUserId(),
    })
    nextTick(() => {
      if (isNearBottom()) scrollToBottom()
    })
  }
}

let isUserNearBottom = true
let totalScrollHeight = 0

function isNearBottom() {
  return isUserNearBottom
}

function onScroll(e) {
  const scrollTop = e.detail.scrollTop
  const scrollHeight = e.detail.scrollHeight || 0
  const clientHeight = e.detail.clientHeight || 600
  if (scrollHeight > 0) {
    totalScrollHeight = scrollHeight
  }
  // 距离底部 100px 以内视为在底部
  isUserNearBottom = (totalScrollHeight - scrollTop - clientHeight) < 100
}

onWebSocketMessage(onWsMessage)

onBeforeUnmount(() => {
  offWebSocketMessage(onWsMessage)
})
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #ededed;
}

.header {
  display: flex;
  align-items: center;
  padding: 60rpx 20rpx 16rpx;
  background: #fff;
  border-bottom: 1rpx solid #e5e5e5;
}

.header-left {
  width: 60rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.back-icon {
  font-size: 40rpx;
  color: #333;
}

.header-center {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-avatar {
  width: 56rpx;
  height: 56rpx;
  border-radius: 50%;
  margin-right: 16rpx;
}

.header-name {
  font-size: 32rpx;
  font-weight: 500;
  color: #333;
}

.header-right {
  width: 60rpx;
  text-align: center;
}
.manage-icon {
  font-size: 36rpx;
  color: #333;
  font-weight: bold;
}
.header-sub {
  font-size: 22rpx;
  color: #999;
  margin-top: 2rpx;
}

.msg-list-wrap {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}
.msg-list {
  height: 100%;
  padding: 20rpx;
}

.msg-row {
  display: flex;
  align-items: flex-start;
  margin-bottom: 24rpx;
}

.msg-row-me {
  flex-direction: row-reverse;
}

.msg-avatar {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  flex-shrink: 0;
}

.msg-body {
  max-width: 65%;
  margin: 0 16rpx;
}

.msg-body-me {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.msg-bubble {
  background: #fff;
  border-radius: 8rpx;
  padding: 18rpx 22rpx;
  word-break: break-all;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.06);
}

.msg-bubble-me {
  background: #95ec69;
}

.msg-text {
  font-size: 30rpx;
  color: #333;
  line-height: 1.5;
}

.msg-text-me {
  color: #333;
}

.msg-image {
  max-width: 200rpx;
  max-height: 300rpx;
  border-radius: 4rpx;
  display: block;
}

.msg-status {
  font-size: 20rpx;
  color: #999;
  margin-top: 4rpx;
}

.msg-time {
  font-size: 20rpx;
  color: #999;
  margin-top: 6rpx;
}

.load-more {
  text-align: center;
  padding: 16rpx;
}

.load-more-text {
  font-size: 24rpx;
  color: #999;
}

.empty {
  text-align: center;
  padding: 200rpx 0;
}

.empty-text {
  font-size: 28rpx;
  color: #999;
}

.input-bar {
  display: flex;
  align-items: center;
  padding: 16rpx 20rpx;
  background: #f7f7f7;
  border-top: 1rpx solid #e5e5e5;
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
}

.img-btn {
  width: 60rpx;
  height: 60rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12rpx;
  flex-shrink: 0;
}
.img-btn-icon {
  font-size: 36rpx;
}

.msg-input {
  flex: 1;
  height: 72rpx;
  background: #fff;
  border-radius: 8rpx;
  padding: 0 20rpx;
  font-size: 30rpx;
}

.send-btn {
  margin-left: 16rpx;
  padding: 0 28rpx;
  height: 72rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #07c160;
  border-radius: 8rpx;
}

.send-btn-disabled {
  opacity: 0.5;
}

.send-btn-text {
  font-size: 28rpx;
  color: #fff;
  white-space: nowrap;
}
</style>
