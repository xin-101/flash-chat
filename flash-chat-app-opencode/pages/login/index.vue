<template>
  <view class="login-page">
    <view class="login-header">
      <view class="logo-wrap">
        <text class="logo-emoji">&#x1F4AC;</text>
      </view>
      <text class="app-name">Flash Chat</text>
      <text class="app-desc">快速、安全的即时通讯</text>
    </view>

    <view class="login-card">
      <view class="form-item">
        <text class="form-label">手机号</text>
        <view class="input-row">
          <text class="prefix">+86</text>
          <input
            class="form-input"
            type="number"
            maxlength="11"
            placeholder="请输入手机号"
            :value="phone"
            @input="onPhoneInput"
          />
          <text v-if="phone" class="clear-btn" @tap="phone = ''">&#x2715;</text>
        </view>
      </view>

      <view class="form-item">
        <text class="form-label">验证码</text>
        <view class="input-row">
          <input
            class="form-input code-input"
            type="number"
            maxlength="6"
            placeholder="请输入验证码"
            :value="code"
            @input="onCodeInput"
          />
          <text v-if="code" class="clear-btn" @tap="code = ''">&#x2715;</text>
          <view
            class="code-btn"
            :class="{ 'code-btn-disabled': !canGetCode }"
            @tap="handleSendCode"
          >
            <text class="code-btn-text">{{ codeBtnText }}</text>
          </view>
        </view>
      </view>

      <view
        class="submit-btn"
        :class="{ 'submit-btn-disabled': !canLogin || loggingIn }"
        @tap="handleLogin"
      >
        <text class="submit-btn-text">{{ loggingIn ? '登录中...' : '登录' }}</text>
      </view>
    </view>

    <view class="agreement">
      <text class="agreement-text">登录即表示同意</text>
      <text class="agreement-link">《用户协议》</text>
      <text class="agreement-text">和</text>
      <text class="agreement-link">《隐私政策》</text>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { sendCode, login } from '../../api/auth'
import { setToken, setUserInfo } from '../../utils/storage'
import { useUserStore } from '../../store'
import { connectWebSocket, closeWebSocket } from '../../utils/websocket'
import { BASE_URL } from '../../config/env'

const userStore = useUserStore()
const phone = ref('')
const code = ref('')
const countdown = ref(0)
const sendingCode = ref(false)
const loggingIn = ref(false)
let timer = null

const isPhoneValid = computed(() => /^1\d{10}$/.test(phone.value))
const canGetCode = computed(() => isPhoneValid.value && countdown.value === 0 && !sendingCode.value)
const canLogin = computed(() => isPhoneValid.value && code.value.length >= 4 && !loggingIn.value)
const codeBtnText = computed(() => {
  if (countdown.value > 0) return `${countdown.value}s`
  if (sendingCode.value) return '发送中'
  return '获取验证码'
})

function onPhoneInput(e) {
  phone.value = e.detail.value
}

function onCodeInput(e) {
  code.value = e.detail.value
}

function startCountdown() {
  countdown.value = 60
  timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) clearInterval(timer)
  }, 1000)
}

async function handleSendCode() {
  if (!canGetCode.value) return
  sendingCode.value = true
  try {
    await sendCode(phone.value)
    uni.showToast({ title: '验证码已发送', icon: 'none' })
    startCountdown()
  } catch (e) {
    uni.showToast({ title: e?.message || '发送失败', icon: 'none' })
  } finally {
    sendingCode.value = false
  }
}

async function handleLogin() {
  if (!canLogin.value) return
  loggingIn.value = true
  try {
    const res = await login(phone.value, code.value)
    if (res?.data) {
      const userData = res.data
      if (userData.token) setToken(userData.token)
      setUserInfo(userData)
      userStore.setUserInfo(userData)

      const token = userData.token
      const wsUrl = BASE_URL.replace('http://', 'ws://').replace('https://', 'wss://') + '/chat/ws'
      closeWebSocket(true)
      setTimeout(() => {
        connectWebSocket(wsUrl, token, true)
      }, 500)
    }
    uni.showToast({ title: '登录成功', icon: 'success' })
    setTimeout(() => {
      uni.reLaunch({ url: '/pages/index/index' })
    }, 500)
  } catch (e) {
    uni.showToast({ title: e?.message || '登录失败', icon: 'none', duration: 2000 })
  } finally {
    loggingIn.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
}

.login-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 180rpx;
  margin-bottom: 80rpx;
}

.logo-wrap {
  width: 160rpx;
  height: 160rpx;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 40rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24rpx;
}

.logo-emoji {
  font-size: 80rpx;
}

.app-name {
  font-size: 52rpx;
  font-weight: 700;
  color: #fff;
  margin-bottom: 12rpx;
}

.app-desc {
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.8);
}

.login-card {
  width: 85%;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 24rpx;
  padding: 48rpx 36rpx;
  box-shadow: 0 8rpx 40rpx rgba(0, 0, 0, 0.1);
}

.form-item {
  margin-bottom: 36rpx;
}

.form-label {
  font-size: 26rpx;
  color: #666;
  margin-bottom: 12rpx;
  display: block;
}

.input-row {
  display: flex;
  align-items: center;
  border-bottom: 2rpx solid #eee;
  padding-bottom: 12rpx;
}

.prefix {
  font-size: 30rpx;
  color: #333;
  margin-right: 16rpx;
  padding-right: 16rpx;
  border-right: 2rpx solid #eee;
}

.form-input {
  flex: 1;
  font-size: 30rpx;
  color: #333;
  height: 56rpx;
}

.code-input {
  flex: 1;
}

.clear-btn {
  font-size: 32rpx;
  color: #ccc;
  padding: 0 8rpx;
  margin-left: 8rpx;
}

.code-btn {
  padding: 12rpx 28rpx;
  background: #007aff;
  border-radius: 8rpx;
  margin-left: 16rpx;
  flex-shrink: 0;
}

.code-btn-disabled {
  background: #ccc;
}

.code-btn-text {
  font-size: 24rpx;
  color: #fff;
  white-space: nowrap;
}

.submit-btn {
  margin-top: 48rpx;
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 44rpx;
}

.submit-btn-disabled {
  opacity: 0.5;
}

.submit-btn-text {
  font-size: 32rpx;
  font-weight: 600;
  color: #fff;
}

.agreement {
  margin-top: 40rpx;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  justify-content: center;
}

.agreement-text {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.7);
}

.agreement-link {
  font-size: 24rpx;
  color: #fff;
  text-decoration: underline;
}
</style>
