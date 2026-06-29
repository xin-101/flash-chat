<!-- pages/login/login.vue -->
<template>
	<view class="login-container">
		<!-- 顶部简洁导航栏 -->
		<view class="login-nav-bar">
			<view class="login-nav-title">手机号登录</view>
		</view>

		<view class="header-section">
			<view class="welcome-title">欢迎登录 Flash Chat</view>
		</view>
		
		<view class="content">
			<view class="login-card">
				<!-- 手机号输入框 -->
				<view class="input-wrapper">
					<view class="input-icon">📱</view>
					<input
						class="input-field"
						v-model="form.phone"
						placeholder="请输入手机号"
						type="number"
						maxlength="11"
						@input="handlePhoneInput"
					/>
					<view v-if="form.phone" class="input-clear" @tap="form.phone = ''">×</view>
				</view>
				
				<!-- 验证码输入框 -->
				<view class="code-input-container">
					<view class="input-wrapper code-input">
						<view class="input-icon">🔒</view>
						<input
							class="input-field"
							v-model="form.code"
							placeholder="请输入验证码"
							type="number"
							maxlength="6"
							@input="handleCodeInput"
						/>
						<view v-if="form.code" class="input-clear" @tap="form.code = ''">×</view>
					</view>
					<button 
						class="code-button"
						@tap.stop="getCode"
						:disabled="!canGetCode || sendingCode"
					>
						{{ codeButtonText }}
					</button>
				</view>
				<button 
					class="login-button"
					@tap="submit"
					:disabled="!canLogin || loggingIn"
				>
					{{ loginButtonText }}
				</button>
			</view>
		</view>
		
		<view class="footer-section">
			<text class="footer-text">登录即表示您同意</text>
			<text class="footer-link">用户协议</text>
			<text class="footer-text">和</text>
			<text class="footer-link">隐私政策</text>
		</view>
	</view>
</template>

<script setup>
	import { ref, reactive, computed } from 'vue';
	import { sendCode, login } from '../../api/auth.js';
	import { connectWebSocket, closeWebSocket } from '../../utils/websocket.js';
	import { BASE_URL } from '../../config/env.js';

	const form = reactive({
		phone: '18000000000',
		code: '',
	});
	const countdown = ref(0);
	const sendingCode = ref(false);
	const loggingIn = ref(false);
	
	// 手机号校验：使用本地正则，避免依赖 uni.$u
	const isPhoneValid = computed(() => /^1\d{10}$/.test(form.phone))

	const canGetCode = computed(() => {
		return isPhoneValid.value && countdown.value === 0;
	});
	const codeButtonText = computed(() => (countdown.value > 0 ? `${countdown.value}s后重试` : '获取验证码'))
	const canLogin = computed(() => isPhoneValid.value && form.code && form.code.length >= 4)
	const loginButtonText = computed(() => (loggingIn.value ? '登录中...' : '登录'))

	let timer = null
	function startCountdown(seconds = 60) {
		countdown.value = seconds
		timer && clearInterval(timer)
		timer = setInterval(() => {
			countdown.value -= 1
			if (countdown.value <= 0) {
				clearInterval(timer)
				timer = null
			}
		}, 1000)
	}

	async function getCode() {
		if (!(isPhoneValid.value) || countdown.value > 0) return
		sendingCode.value = true
		try {
			await sendCode(form.phone)
			uni.showToast({ title: '验证码已发送', icon: 'none' })
			startCountdown(60)
		} catch (e) {
			uni.showToast({ title: (e && (e.message || e.msg)) || '发送失败', icon: 'none' })
		} finally {
			sendingCode.value = false
		}
	}

	// 处理手机号输入
	function handlePhoneInput(e) {
		form.phone = e.detail.value
	}

	// 处理验证码输入
	function handleCodeInput(e) {
		form.code = e.detail.value
	}

	async function submit() {
		if (!canLogin.value) {
			if (!isPhoneValid.value) {
				uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
			} else if (!form.code || form.code.length < 4) {
				uni.showToast({ title: '请输入验证码', icon: 'none' })
			}
			return
		}
		loggingIn.value = true
		try {
			if (!form.phone || !form.code) {
				uni.showToast({ title: '请填写完整信息', icon: 'none' })
				return
			}
			
			const res = await login(form.phone, form.code)
			
			// 登录成功后尝试连接 WebSocket（如果后端支持）
			// 注意：如果后端没有 WebSocket 服务，连接会失败，但不影响登录
			// 如果之前已经达到重连上限，先重置状态再连接
			if (res && res.data && res.data.token) {
				const token = res.data.token;
				// WebSocket 路径：通过网关 /chat/ws，网关会去掉 /chat 前缀变成 /ws
				// 如果后端 WebSocket 服务路径是 /ws，则使用 /chat/ws
				// 如果后端 WebSocket 服务路径是 /chat/ws，则需要使用 /chat/chat/ws
				const wsUrl = BASE_URL.replace('http://', 'ws://').replace('https://', 'wss://') + '/chat/ws';
				// 延迟连接，避免与登录请求冲突
				setTimeout(() => {
					// 先关闭旧连接并重置状态，然后重新连接
					closeWebSocket(true); // true 表示重置重连计数
					connectWebSocket(wsUrl, token, true); // true 表示强制重连
				}, 500);
			}
			
			uni.showToast({ title: '登录成功', icon: 'success' })
			setTimeout(() => {
				uni.reLaunch({ url: '/pages/index/index' })
			}, 300)
		} catch (e) {
			// 显示后端返回的错误信息
			const errorMsg = e?.message || e?.msg || '登录失败，请重试';
			uni.showToast({ 
				title: errorMsg, 
				icon: 'none',
				duration: 2000
			});
			console.error('登录失败:', e);
		} finally {
			loggingIn.value = false
		}
	}
</script>

<style scoped>
	.login-container {
		display: flex;
		flex-direction: column;
		min-height: 100vh;
		background-color: #f5f5f5;
	}

	.login-nav-bar {
		padding: 70rpx 40rpx 30rpx;
		text-align: center;
		background-color: #ffffff;
		border-bottom: 1rpx solid #f0f0f0;
	}

	.login-nav-title {
		font-size: 32rpx;
		color: #333333;
	}

	.header-section {
		padding: 80rpx 40rpx 40rpx;
	}

	.welcome-title {
		font-size: 44rpx;
		font-weight: 600;
		color: #222222;
	}

	.content {
		flex: 1;
		padding: 0 40rpx;
	}

	.login-card {
		margin-top: 40rpx;
		background-color: #ffffff;
		border-radius: 24rpx;
		padding: 40rpx 30rpx 60rpx;
		box-shadow: 0 16rpx 40rpx rgba(0, 0, 0, 0.05);
	}

	.input-wrapper {
		background-color: #f5f5f5;
		border-radius: 999rpx;
		padding: 10rpx 24rpx;
		display: flex;
		align-items: center;
		position: relative;
	}

	.input-icon {
		font-size: 32rpx;
		margin-right: 16rpx;
		flex-shrink: 0;
	}

	.input-field {
		flex: 1;
		font-size: 30rpx;
		color: #333333;
		height: 60rpx;
		line-height: 60rpx;
	}

	.input-clear {
		width: 40rpx;
		height: 40rpx;
		line-height: 40rpx;
		text-align: center;
		font-size: 36rpx;
		color: #999999;
		flex-shrink: 0;
		margin-left: 16rpx;
	}

	.code-input-container {
		display: flex;
		flex-direction: row;
		align-items: center;
		width: 100%;
		justify-content: space-between;
		margin-top: 16rpx;
	}

	.code-input {
		flex: 1;
	}

	.code-button {
		margin-left: 20rpx;
		padding: 0 32rpx;
		height: 64rpx;
		line-height: 64rpx;
		background-color: #ff9f43;
		border: none;
		border-radius: 999rpx;
		color: #ffffff;
		font-size: 26rpx;
		flex-shrink: 0;
	}

	.code-button:disabled {
		background-color: #cccccc;
		color: #999999;
	}

	.login-button {
		margin-top: 60rpx;
		width: 100%;
		background-color: #ff9f43;
		border: none;
		border-radius: 999rpx;
		color: #ffffff;
		font-size: 32rpx;
		font-weight: 500;
		height: 88rpx;
		line-height: 88rpx;
	}

	.login-button:disabled {
		background-color: #cccccc;
		color: #999999;
	}

	.footer-section {
		display: flex;
		justify-content: center;
		align-items: center;
		padding: 40rpx 0;
		background: transparent;
	}

	.footer-text {
		font-size: 24rpx;
		color: rgba(255, 255, 255, 0.7);
	}

	.footer-link {
		font-size: 24rpx;
		color: #ffffff;
		text-decoration: underline;
		margin: 0 8rpx;
	}
</style>