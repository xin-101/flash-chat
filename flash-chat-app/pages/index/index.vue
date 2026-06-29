<template>
	<view class="container">
		<!-- 顶部导航栏（仿微信样式） -->
		<view class="nav-bar">
			<view class="nav-left">
				<!-- 预留占位，保持标题居中 -->
			</view>
			<view class="nav-title">首页</view>
			<view class="nav-actions">
				<u-icon name="more-dot-fill" size="22" color="#333"></u-icon>
				<u-icon name="scan" size="22" color="#333" @click="goToSearch" style="margin-left: 24rpx;"></u-icon>
			</view>
		</view>
		
		<!-- 会话列表 -->
		<view class="chat-list">
			<view 
				v-for="(item, index) in chatList" 
				:key="item.id || index"
				class="chat-item"
				@click="goToChat(item)"
			>
				<!-- 头像 -->
				<view class="avatar-container">
					<image 
						class="avatar" 
						:src="getAvatar(item)" 
						mode="aspectFill"
					></image>
					<view v-if="item.unreadCount > 0" class="online-indicator"></view>
				</view>
				
				<!-- 内容区 -->
				<view class="content">
					<view class="header">
						<text class="nickname">{{ getNickname(item) }}</text>
						<text class="time">{{ formatTime(item.lastMessageTime || item.updatedTime) }}</text>
					</view>
					<view class="footer">
						<text class="last-message">{{ getLastMessage(item) }}</text>
						<view v-if="item.unreadCount > 0" class="badge">
							<text class="badge-text">{{ item.unreadCount > 99 ? '99+' : item.unreadCount }}</text>
						</view>
					</view>
				</view>
			</view>
			
			<!-- 空状态 -->
			<view v-if="chatList.length === 0 && !loading" class="empty">
				<image class="empty-icon" src="/static/logo.png" mode="aspectFit"></image>
				<text class="empty-text">暂无会话</text>
				<text class="empty-subtext">点击右上角 + 开始新的聊天</text>
			</view>
			
			<!-- 加载中 -->
			<view v-if="loading" class="loading">
				<u-loading-icon></u-loading-icon>
			</view>
		</view>
	</view>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { getChatList } from '../../api/chat.js';
import { getUserInfo } from '../../utils/storage.js';
import { onWebSocketMessage } from '../../utils/websocket.js';

const chatList = ref([]);
const loading = ref(false);
const currentUser = ref(null);

// 获取会话列表
async function loadChatList() {
	loading.value = true;
	try {
		const res = await getChatList();
		if (res && res.data) {
			chatList.value = Array.isArray(res.data) ? res.data : [];
		} else {
			chatList.value = [];
		}
	} catch (e) {
		console.error('获取会话列表失败:', e);
		chatList.value = [];
		uni.showToast({
			title: e.message || '获取会话列表失败',
			icon: 'none'
		});
	} finally {
		loading.value = false;
	}
}

// 获取头像
function getAvatar(item) {
	if (item.face) {
		return item.face;
	}
	// 如果是会话对象，取对方的头像
	if (item.targetUser && item.targetUser.face) {
		return item.targetUser.face;
	}
	return '/static/default-avatar.png';
}

// 获取昵称
function getNickname(item) {
	if (item.nickname) {
		return item.nickname;
	}
	// 如果是会话对象，取对方的昵称
	if (item.targetUser && item.targetUser.nickname) {
		return item.targetUser.nickname;
	}
	return '未知用户';
}

// 获取最后一条消息
function getLastMessage(item) {
	if (item.lastMessage) {
		return item.lastMessage;
	}
	if (item.lastMessageContent) {
		return item.lastMessageContent;
	}
	return '暂无消息';
}

// 格式化时间
function formatTime(time) {
	if (!time) return '';
	
	const date = new Date(time);
	const now = new Date();
	const diff = now - date;
	
	// 今天
	if (diff < 24 * 60 * 60 * 1000 && date.getDate() === now.getDate()) {
		const hours = date.getHours().toString().padStart(2, '0');
		const minutes = date.getMinutes().toString().padStart(2, '0');
		return `${hours}:${minutes}`;
	}
	
	// 昨天
	if (diff < 48 * 60 * 60 * 1000 && date.getDate() === now.getDate() - 1) {
		return '昨天';
	}
	
	// 一周内
	if (diff < 7 * 24 * 60 * 60 * 1000) {
		const weekdays = ['日', '一', '二', '三', '四', '五', '六'];
		return `周${weekdays[date.getDay()]}`;
	}
	
	// 更早
	const month = (date.getMonth() + 1).toString().padStart(2, '0');
	const day = date.getDate().toString().padStart(2, '0');
	return `${month}-${day}`;
}

// 跳转到聊天详情页
function goToChat(item) {
	const conversationId = item.id || item.conversationId;
	if (!conversationId) {
		uni.showToast({
			title: '会话ID不存在',
			icon: 'none'
		});
		return;
	}
	
	uni.navigateTo({
		url: `/pages/chat/detail?conversationId=${conversationId}&targetUserId=${item.targetUserId || ''}&nickname=${encodeURIComponent(getNickname(item))}&avatar=${encodeURIComponent(getAvatar(item))}`
	});
}

// 跳转到搜索页
function goToSearch() {
	uni.navigateTo({
		url: '/pages/user/search'
	});
}

// WebSocket消息监听
function handleWebSocketMessage(data) {
	console.log('收到WebSocket消息:', data);
	// 如果是新消息，刷新会话列表
	if (data.type === 'message' || data.type === 'new_message') {
		loadChatList();
	}
}

onMounted(() => {
	currentUser.value = getUserInfo();
	loadChatList();
	// 监听WebSocket消息
	onWebSocketMessage(handleWebSocketMessage);
});

onShow(() => {
	// 每次显示页面时刷新列表
	loadChatList();
});

// 下拉刷新（uni-app页面选项）
function onPullDownRefresh() {
	loadChatList().finally(() => {
		uni.stopPullDownRefresh();
	});
}
</script>

<style scoped>
.container {
	min-height: 100vh;
	background-color: #f5f5f5;
}

.nav-bar {
	display: flex;
	justify-content: space-between;
	align-items: center;
	padding: 60rpx 30rpx 24rpx;
	background-color: #ffffff;
	border-bottom: 1rpx solid #f0f0f0;
}

.nav-title {
	font-size: 34rpx;
	font-weight: 600;
	color: #333;
	text-align: center;
	flex: 1;
}

.nav-actions {
	display: flex;
	align-items: center;
	justify-content: flex-end;
	width: 140rpx;
}

.chat-list {
	background-color: #ffffff;
}

.chat-item {
	display: flex;
	padding: 24rpx 30rpx;
	border-bottom: 1rpx solid #f0f0f0;
	background-color: #fff;
	transition: background-color 0.2s ease;
}

.chat-item:active {
	background-color: #f5f5f5;
}

.avatar-container {
	position: relative;
	margin-right: 24rpx;
	flex-shrink: 0;
}

.avatar {
	width: 96rpx;
	height: 96rpx;
	border-radius: 50%;
	border: 2rpx solid #f0f0f0;
}

.online-indicator {
	position: absolute;
	top: 0;
	right: 0;
	width: 20rpx;
	height: 20rpx;
	background-color: #4CAF50;
	border-radius: 50%;
	border: 2rpx solid #fff;
}

.content {
	flex: 1;
	display: flex;
	flex-direction: column;
	justify-content: space-between;
	min-width: 0;
}

.header {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 12rpx;
}

.nickname {
	font-size: 32rpx;
	font-weight: 500;
	color: #333;
	flex: 1;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.time {
	font-size: 24rpx;
	color: #999;
	margin-left: 20rpx;
	flex-shrink: 0;
}

.footer {
	display: flex;
	justify-content: space-between;
	align-items: center;
}

.last-message {
	font-size: 28rpx;
	color: #666;
	flex: 1;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.badge {
	background-color: #ff3b30;
	border-radius: 20rpx;
	min-width: 36rpx;
	height: 36rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	padding: 0 12rpx;
	margin-left: 20rpx;
	flex-shrink: 0;
}

.badge-text {
	color: #fff;
	font-size: 20rpx;
	font-weight: 500;
}

.empty {
	display: flex;
	flex-direction: column;
	justify-content: center;
	align-items: center;
	padding: 200rpx 0;
}

.empty-icon {
	width: 120rpx;
	height: 120rpx;
	margin-bottom: 20rpx;
	opacity: 0.5;
}

.empty-text {
	font-size: 32rpx;
	color: #999;
	margin-bottom: 16rpx;
}

.empty-subtext {
	font-size: 28rpx;
	color: #ccc;
}

.loading {
	display: flex;
	justify-content: center;
	align-items: center;
	padding: 40rpx 0;
}
</style>