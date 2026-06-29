<template>
	<view class="container">
		<!-- 搜索栏 -->
		<view class="search-bar">
			<u-search 
				v-model="keyword" 
				placeholder="搜索手机号或昵称" 
				:show-action="true"
				action-text="搜索"
				shape="round"
				@search="handleSearch"
				@custom="handleSearch"
				@change="handleKeywordChange"
			></u-search>
		</view>
		
		<!-- 搜索结果 -->
		<view class="result-list" v-if="keyword">
			<view 
				v-for="(user, index) in userList" 
				:key="user.id || index"
				class="user-item"
				@click="startChat(user)"
			>
				<!-- 头像 -->
				<image 
					class="avatar" 
					:src="getAvatar(user)" 
					mode="aspectFill"
				></image>
				
				<!-- 用户信息 -->
				<view class="user-info">
					<text class="nickname">{{ user.nickname || '未知用户' }}</text>
					<text class="mobile" v-if="user.mobile">{{ user.mobile }}</text>
					<text class="flash-chat-num" v-if="user.flashChatNum">闪聊号: {{ user.flashChatNum }}</text>
				</view>
				
				<!-- 箭头 -->
				<text class="arrow">></text>
			</view>
			
			<!-- 空状态 -->
			<view v-if="userList.length === 0 && !loading && searched" class="empty">
				<text class="empty-text">未找到用户</text>
			</view>
			
			<!-- 加载中 -->
			<view v-if="loading" class="loading">
				<u-loading-icon></u-loading-icon>
			</view>
		</view>
		
		<!-- 搜索提示 -->
		<view v-else class="tip">
			<text class="tip-text">请输入手机号或昵称进行搜索</text>
		</view>
	</view>
</template>

<script setup>
import { ref } from 'vue';
import { searchUser, createConversation } from '../../api/chat.js';
import { getUserInfo } from '../../utils/storage.js';

const keyword = ref('');
const userList = ref([]);
const loading = ref(false);
const searched = ref(false);
const currentUser = ref(null);

// 获取头像
function getAvatar(user) {
	if (user.face) {
		return user.face;
	}
	return '/static/default-avatar.png';
}

// 搜索用户
async function handleSearch() {
	const searchKeyword = keyword.value.trim();
	if (!searchKeyword) {
		uni.showToast({
			title: '请输入搜索关键词',
			icon: 'none'
		});
		return;
	}
	
	loading.value = true;
	searched.value = true;
	
	try {
		const res = await searchUser(searchKeyword);
		if (res && res.data) {
			// 过滤掉当前用户
			const users = Array.isArray(res.data) ? res.data : [];
			userList.value = users.filter(user => user.id !== currentUser.value?.id);
		} else {
			userList.value = [];
		}
	} catch (e) {
		console.error('搜索用户失败:', e);
		uni.showToast({
			title: e.message || '搜索失败',
			icon: 'none'
		});
		userList.value = [];
	} finally {
		loading.value = false;
	}
}

// 关键词变化
function handleKeywordChange(value) {
	if (!value.trim()) {
		userList.value = [];
		searched.value = false;
	}
}

// 开始聊天
async function startChat(user) {
	if (!user || !user.id) {
		uni.showToast({
			title: '用户信息不完整',
			icon: 'none'
		});
		return;
	}
	
	// 检查是否是当前用户
	if (user.id === currentUser.value?.id) {
		uni.showToast({
			title: '不能和自己聊天',
			icon: 'none'
		});
		return;
	}
	
	try {
		// 创建或获取会话
		uni.showLoading({
			title: '加载中...'
		});
		
		const res = await createConversation(user.id);
		
		uni.hideLoading();
		
		if (res && res.data) {
			const conversationId = res.data.id || res.data.conversationId || res.data;
			const nickname = user.nickname || '用户';
			const avatar = getAvatar(user);
			
			// 跳转到聊天详情页
			uni.navigateTo({
				url: `/pages/chat/detail?conversationId=${conversationId}&targetUserId=${user.id}&nickname=${encodeURIComponent(nickname)}&avatar=${encodeURIComponent(avatar)}`
			});
		} else {
			uni.showToast({
				title: '创建会话失败',
				icon: 'none'
			});
		}
	} catch (e) {
		uni.hideLoading();
		console.error('创建会话失败:', e);
		uni.showToast({
			title: e.message || '创建会话失败',
			icon: 'none'
		});
	}
}

// 初始化
currentUser.value = getUserInfo();
</script>

<style scoped>
.container {
	min-height: 100vh;
	background-color: #f5f5f5;
}

.search-bar {
	padding: 20rpx;
	background-color: #fff;
}

.result-list {
	background-color: #fff;
	margin-top: 20rpx;
}

.user-item {
	display: flex;
	align-items: center;
	padding: 24rpx 30rpx;
	border-bottom: 1rpx solid #f0f0f0;
	background-color: #fff;
}

.user-item:active {
	background-color: #f5f5f5;
}

.avatar {
	width: 96rpx;
	height: 96rpx;
	border-radius: 12rpx;
	margin-right: 24rpx;
	flex-shrink: 0;
}

.user-info {
	flex: 1;
	display: flex;
	flex-direction: column;
	min-width: 0;
}

.nickname {
	font-size: 32rpx;
	font-weight: 500;
	color: #333;
	margin-bottom: 8rpx;
}

.mobile {
	font-size: 26rpx;
	color: #666;
	margin-bottom: 4rpx;
}

.flash-chat-num {
	font-size: 24rpx;
	color: #999;
}

.arrow {
	font-size: 32rpx;
	color: #ccc;
	margin-left: 20rpx;
	flex-shrink: 0;
}

.empty {
	display: flex;
	justify-content: center;
	align-items: center;
	padding: 200rpx 0;
}

.empty-text {
	font-size: 28rpx;
	color: #999;
}

.loading {
	display: flex;
	justify-content: center;
	align-items: center;
	padding: 40rpx 0;
}

.tip {
	display: flex;
	justify-content: center;
	align-items: center;
	padding: 200rpx 0;
}

.tip-text {
	font-size: 28rpx;
	color: #999;
}
</style>
