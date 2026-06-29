<template>
	<view class="container">
		<!-- 顶部导航 -->
		<view class="nav-bar">
			<view class="nav-title">修改资料</view>
		</view>

		<!-- 用户信息卡片（仿截图样式） -->
		<view class="user-card">
			<view class="user-card-inner">
				<view class="orange-strip"></view>
			<view class="user-info">
					<image 
						class="avatar" 
						:src="userInfo.face || '/static/default-avatar.svg'" 
						mode="aspectFill"
					></image>
				<view class="info">
						<text class="nickname">{{ userInfo.nickname || '用户0000' }}</text>
						<text class="mobile" v-if="userInfo.mobile">闪聊号：{{ userInfo.id || userInfo.mobile }}</text>
					</view>
				</view>
			</view>
		</view>
		
		<!-- 功能列表 -->
		<view class="function-section">
				<view class="function-item" @click="goToProfile">
				<text class="function-text">头像</text>
					<text class="arrow">></text>
				</view>
			<view class="function-item" @click="goToProfile">
				<text class="function-text">账号与安全</text>
					<text class="arrow">></text>
				</view>
				<view class="function-item" @click="goToNotification">
				<text class="function-text">消息通知</text>
					<text class="arrow">></text>
			</view>
			<view class="function-item" @click="goToPrivacy">
				<text class="function-text">隐私设置</text>
					<text class="arrow">></text>
				</view>
				<view class="function-item" @click="goToAbout">
					<text class="function-text">关于我们</text>
					<text class="arrow">></text>
			</view>
		</view>
		
		<!-- 退出登录 -->
		<view class="logout-btn" @click="logout">
			<text class="logout-text">退出登录</text>
		</view>
	</view>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { getUserInfo, clearAuth } from '../../utils/storage.js';

const userInfo = ref({});
const chatCount = ref(0);
const friendCount = ref(0);

onMounted(() => {
	userInfo.value = getUserInfo() || {};
	// 模拟数据
	chatCount.value = Math.floor(Math.random() * 50) + 10;
	friendCount.value = Math.floor(Math.random() * 20) + 5;
});

// 编辑头像
function editAvatar() {
	uni.chooseImage({
		count: 1,
		sizeType: ['compressed'],
		sourceType: ['album', 'camera'],
		success: (res) => {
			uni.showToast({
				title: '头像更新功能开发中',
				icon: 'none'
			});
		}
	});
}

// 跳转到个人资料页
function goToProfile() {
	uni.showToast({
		title: '个人资料功能开发中',
		icon: 'none'
	});
}

// 跳转到隐私设置
function goToPrivacy() {
	uni.showToast({
		title: '隐私设置功能开发中',
		icon: 'none'
	});
}

// 跳转到通知设置
function goToNotification() {
	uni.showToast({
		title: '通知设置功能开发中',
		icon: 'none'
	});
}

// 跳转到帮助与反馈
function goToHelp() {
	uni.showToast({
		title: '帮助与反馈功能开发中',
		icon: 'none'
	});
}

// 跳转到关于我们
function goToAbout() {
	uni.showToast({
		title: '关于我们功能开发中',
		icon: 'none'
	});
}

// 退出登录
function logout() {
	uni.showModal({
		title: '确认退出',
		content: '确定要退出登录吗？',
		success: (res) => {
			if (res.confirm) {
				clearAuth();
				uni.reLaunch({
					url: '/pages/login/index'
				});
			}
		}
	});
}
</script>

<style scoped>
.container {
	min-height: 100vh;
	background-color: #f7f7f7;
	padding-top: 0;
}

.nav-bar {
	padding: 70rpx 30rpx 24rpx;
	background-color: #ffffff;
	border-bottom: 1rpx solid #f0f0f0;
	text-align: center;
}

.nav-title {
	font-size: 32rpx;
	color: #333;
}

.user-card {
	padding: 20rpx 20rpx 0;
}

.avatar {
	width: 120rpx;
	height: 120rpx;
	border-radius: 50%;
}

.user-card-inner {
	background-color: #ffffff;
	border-radius: 16rpx;
	overflow: hidden;
	flex-direction: row;
	display: flex;
	align-items: center;
}

.orange-strip {
	width: 8rpx;
	height: 100%;
	background-color: #ff9f43;
}

.user-info {
	display: flex;
	align-items: center;
	padding: 24rpx 30rpx;
}

.info {
	flex: 1;
	margin-left: 24rpx;
}

.nickname {
	display: block;
	font-size: 36rpx;
	font-weight: 600;
	color: #333;
	margin-bottom: 12rpx;
}

.mobile {
	font-size: 28rpx;
	color: #888;
}

.function-item {
	display: flex;
	align-items: center;
	padding: 30rpx 30rpx;
	border-bottom: 1rpx solid #f2f2f2;
	transition: background-color 0.2s;
	background-color: #ffffff;
}

.function-item:active {
	background-color: #f8f8f8;
}

.function-item:last-child {
	border-bottom: none;
}

.function-text {
	font-size: 30rpx;
	color: #333;
	flex: 1;
}

.arrow {
	font-size: 32rpx;
	color: #ccc;
}

.logout-btn {
	background-color: #ff4d4f;
	margin: 60rpx 40rpx 0;
	border-radius: 999rpx;
	padding: 28rpx;
	text-align: center;
	transition: background-color 0.2s;
}

.logout-btn:active {
	background-color: #e84343;
}

.logout-text {
	font-size: 32rpx;
	color: #ffffff;
	font-weight: 500;
}
</style>