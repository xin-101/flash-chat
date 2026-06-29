<template>
	<view class="container">
		<!-- 聊天头部 -->
		<view class="chat-header">
			<view class="header-left">
				<u-icon name="arrow-left" size="20" color="#333" @click="goBack"></u-icon>
				<image class="header-avatar" :src="targetAvatar" mode="aspectFill"></image>
				<view class="header-info">
					<text class="header-name">{{ decodeURIComponent(props.nickname || '用户') }}</text>
					<text class="header-status">在线</text>
				</view>
			</view>
			<view class="header-right">
				<u-icon name="more-dot-fill" size="20" color="#333" @click="showMoreActions"></u-icon>
			</view>
		</view>
		
		<!-- 消息列表 -->
		<scroll-view 
			class="message-list" 
			scroll-y 
			:scroll-top="scrollTop"
			:scroll-with-animation="true"
			@scrolltolower="loadMoreMessages"
			ref="messageScroll"
		>
			<view class="message-item" 
				v-for="(msg, index) in messageList" 
				:key="msg.id || index"
				:class="{ 'own': isOwnMessage(msg) }"
			>
				<!-- 对方消息显示头像 -->
				<image 
					v-if="!isOwnMessage(msg)" 
					class="avatar" 
					:src="targetAvatar" 
					mode="aspectFill"
				></image>
				
				<view class="message-content">
					<!-- 消息气泡 -->
					<view class="bubble" :class="{ 'own-bubble': isOwnMessage(msg) }">
						<text class="text">{{ msg.content }}</text>
						<view v-if="msg.status === 'sending'" class="sending-indicator">
							<u-loading-icon size="12" color="#999"></u-loading-icon>
						</view>
					</view>
					<!-- 时间 -->
					<text class="time">{{ formatMessageTime(msg.createdTime || msg.sendTime) }}</text>
				</view>
				
				<!-- 自己消息显示头像 -->
				<image 
					v-if="isOwnMessage(msg)" 
					class="avatar" 
					:src="myAvatar" 
					mode="aspectFill"
				></image>
			</view>
			
			<!-- 加载更多 -->
			<view v-if="loadingMore" class="loading-more">
				<u-loading-icon mode="spinner" size="20"></u-loading-icon>
			</view>
			
			<!-- 空状态 -->
			<view v-if="messageList.length === 0 && !loading" class="empty">
				<image class="empty-icon" src="/static/logo.png" mode="aspectFit"></image>
				<text class="empty-text">暂无消息</text>
				<text class="empty-subtext">发送第一条消息开始聊天吧</text>
			</view>
		</scroll-view>
		
		<!-- 输入栏 -->
		<view class="input-bar">
			<u-input 
				v-model="inputText" 
				placeholder="输入消息..." 
				:border="false"
				:clearable="true"
				@confirm="sendMessage"
				confirm-type="send"
				class="message-input"
			></u-input>
			<view class="input-actions">
				<u-icon name="plus-circle" size="24" color="#007AFF" @click="showMoreOptions"></u-icon>
				<u-button 
					type="primary" 
					size="mini" 
					text="发送" 
					@click="sendMessage"
					:disabled="!inputText.trim() || sending"
					:loading="sending"
					class="send-button"
				></u-button>
			</view>
		</view>
		
		<!-- 更多选项弹窗 -->
		<u-popup :show="showOptions" mode="bottom" @close="showOptions = false">
			<view class="options-container">
				<view class="option-item" @click="selectImage">
					<u-icon name="photo" size="24" color="#007AFF"></u-icon>
					<text class="option-text">图片</text>
				</view>
				<view class="option-item" @click="selectFile">
					<u-icon name="file-text" size="24" color="#007AFF"></u-icon>
					<text class="option-text">文件</text>
				</view>
				<view class="option-item" @click="selectLocation">
					<u-icon name="map" size="24" color="#007AFF"></u-icon>
					<text class="option-text">位置</text>
				</view>
			</view>
		</u-popup>
	</view>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue';
import { getMessageList, sendTextMessage } from '../../api/chat.js';
import { getUserInfo } from '../../utils/storage.js';
import { onWebSocketMessage, offWebSocketMessage, sendWebSocketMessage } from '../../utils/websocket.js';

const props = defineProps({
	conversationId: {
		type: String,
		required: true
	},
	targetUserId: {
		type: String,
		default: ''
	},
	nickname: {
		type: String,
		default: '用户'
	},
	avatar: {
		type: String,
		default: '/static/default-avatar.png'
	}
});

const messageList = ref([]);
const inputText = ref('');
const sending = ref(false);
const loading = ref(false);
const loadingMore = ref(false);
const scrollTop = ref(0);
const page = ref(1);
const pageSize = 20;
const hasMore = ref(true);
const currentUser = ref(null);
const targetAvatar = ref(props.avatar);
const myAvatar = ref('/static/default-avatar.png');
const showOptions = ref(false);

// 判断是否是自己发送的消息
function isOwnMessage(msg) {
	return msg.fromUserId === currentUser.value?.id || msg.senderId === currentUser.value?.id;
}

// 加载消息列表
async function loadMessages(isLoadMore = false) {
	if (isLoadMore) {
		if (!hasMore.value || loadingMore.value) return;
		loadingMore.value = true;
		page.value++;
	} else {
		loading.value = true;
		page.value = 1;
		hasMore.value = true;
	}
	
	try {
		const res = await getMessageList(props.conversationId, page.value, pageSize);
		if (res && res.data) {
			const messages = Array.isArray(res.data) ? res.data : (res.data.list || []);
			
			if (isLoadMore) {
				// 加载更多，插入到前面
				messageList.value = [...messages.reverse(), ...messageList.value];
			} else {
				// 首次加载，倒序显示（最新的在底部）
				messageList.value = messages.reverse();
				// 滚动到底部
				nextTick(() => {
					scrollToBottom();
				});
			}
			
			// 判断是否还有更多
			if (messages.length < pageSize) {
				hasMore.value = false;
			}
		} else {
			messageList.value = [];
		}
	} catch (e) {
		console.error('获取消息列表失败:', e);
		uni.showToast({
			title: e.message || '获取消息失败',
			icon: 'none'
		});
	} finally {
		loading.value = false;
		loadingMore.value = false;
	}
}

// 加载更多消息
function loadMoreMessages() {
	loadMessages(true);
}

// 发送消息
async function sendMessage() {
	const content = inputText.value.trim();
	if (!content || sending.value) return;
	
	sending.value = true;
	
	try {
		// 先显示本地消息（乐观更新）
		const tempMessage = {
			id: 'temp_' + Date.now(),
			content: content,
			fromUserId: currentUser.value?.id,
			senderId: currentUser.value?.id,
			createdTime: new Date().toISOString(),
			sendTime: new Date().toISOString(),
			status: 'sending'
		};
		messageList.value.push(tempMessage);
		inputText.value = '';
		
		// 滚动到底部
		nextTick(() => {
			scrollToBottom();
		});
		
		// 发送到服务器
		const res = await sendTextMessage(props.conversationId, content);
		
		// 更新消息状态
		if (res && res.data) {
			const index = messageList.value.findIndex(m => m.id === tempMessage.id);
			if (index !== -1) {
				messageList.value[index] = {
					...res.data,
					status: 'sent'
				};
			} else {
				// 如果服务器返回了新消息，替换临时消息
				messageList.value.push({
					...res.data,
					status: 'sent'
				});
			}
		}
		
		// 尝试通过WebSocket发送
		sendWebSocketMessage({
			type: 'message',
			conversationId: props.conversationId,
			content: content
		});
		
		// 滚动到底部
		nextTick(() => {
			scrollToBottom();
		});
	} catch (e) {
		console.error('发送消息失败:', e);
		uni.showToast({
			title: e.message || '发送失败',
			icon: 'none'
		});
		
		// 移除临时消息
		const index = messageList.value.findIndex(m => m.status === 'sending');
		if (index !== -1) {
			messageList.value.splice(index, 1);
		}
	} finally {
		sending.value = false;
	}
}

// 滚动到底部
function scrollToBottom() {
	// 使用一个很大的值来确保滚动到底部
	scrollTop.value = 999999;
}

// 格式化消息时间
function formatMessageTime(time) {
	if (!time) return '';
	
	const date = new Date(time);
	const now = new Date();
	const diff = now - date;
	
	// 5分钟内不显示时间
	if (diff < 5 * 60 * 1000) {
		return '';
	}
	
	const hours = date.getHours().toString().padStart(2, '0');
	const minutes = date.getMinutes().toString().padStart(2, '0');
	return `${hours}:${minutes}`;
}

// WebSocket消息处理
function handleWebSocketMessage(data) {
	console.log('收到WebSocket消息:', data);
	
	// 如果是当前会话的消息
	if (data.type === 'message' || data.type === 'new_message') {
		if (data.conversationId === props.conversationId) {
			// 添加到消息列表
			messageList.value.push({
				...data,
				createdTime: data.createdTime || data.sendTime || new Date().toISOString()
			});
			
			// 滚动到底部
			nextTick(() => {
				scrollToBottom();
			});
		}
	}
}

onMounted(() => {
	currentUser.value = getUserInfo();
	if (currentUser.value && currentUser.value.face) {
		myAvatar.value = currentUser.value.face;
	}
	
	// 设置导航栏标题
	uni.setNavigationBarTitle({
		title: decodeURIComponent(props.nickname || '聊天')
	});
	
	// 加载消息
	loadMessages();
	
	// 监听WebSocket消息
	onWebSocketMessage(handleWebSocketMessage);
});

// 返回上一页
function goBack() {
	uni.navigateBack();
}

// 显示更多操作
function showMoreActions() {
	uni.showActionSheet({
		itemList: ['清空聊天记录', '查看用户信息'],
		success: (res) => {
			if (res.tapIndex === 0) {
				clearChatHistory();
			} else if (res.tapIndex === 1) {
				viewUserInfo();
			}
		}
	});
}

// 显示更多选项
function showMoreOptions() {
	showOptions.value = true;
}

// 选择图片
function selectImage() {
	showOptions.value = false;
	uni.chooseImage({
		count: 1,
		sizeType: ['compressed'],
		sourceType: ['album', 'camera'],
		success: (res) => {
			console.log('选择图片:', res.tempFilePaths);
			uni.showToast({
				title: '图片功能开发中',
				icon: 'none'
			});
		}
	});
}

// 选择文件
function selectFile() {
	showOptions.value = false;
	uni.showToast({
		title: '文件功能开发中',
		icon: 'none'
	});
}

// 选择位置
function selectLocation() {
	showOptions.value = false;
	uni.chooseLocation({
		success: (res) => {
			console.log('选择位置:', res);
			uni.showToast({
				title: '位置功能开发中',
				icon: 'none'
			});
		}
	});
}

// 清空聊天记录
function clearChatHistory() {
	uni.showModal({
		title: '提示',
		content: '确定要清空聊天记录吗？',
		success: (res) => {
			if (res.confirm) {
				messageList.value = [];
				uni.showToast({
					title: '聊天记录已清空',
					icon: 'success'
				});
			}
		}
	});
}

// 查看用户信息
function viewUserInfo() {
	uni.showToast({
		title: '用户信息功能开发中',
		icon: 'none'
	});
}

onUnmounted(() => {
	// 移除WebSocket监听
	offWebSocketMessage(handleWebSocketMessage);
});
</script>

<style scoped>
.container {
	display: flex;
	flex-direction: column;
	height: 100vh;
	background-color: #f5f5f5;
}

.chat-header {
	display: flex;
	justify-content: space-between;
	align-items: center;
	padding: 60rpx 30rpx 20rpx;
	background-color: #fff;
	border-bottom: 1rpx solid #f0f0f0;
}

.header-left {
	display: flex;
	align-items: center;
	flex: 1;
}

.header-avatar {
	width: 64rpx;
	height: 64rpx;
	border-radius: 50%;
	margin: 0 20rpx;
}

.header-info {
	display: flex;
	flex-direction: column;
	flex: 1;
}

.header-name {
	font-size: 32rpx;
	font-weight: 500;
	color: #333;
	margin-bottom: 4rpx;
}

.header-status {
	font-size: 24rpx;
	color: #4CAF50;
}

.message-list {
	flex: 1;
	padding: 20rpx;
	overflow-y: auto;
	background-color: #f5f5f5;
}

.message-item {
	display: flex;
	margin-bottom: 30rpx;
	align-items: flex-start;
}

.message-item.own {
	flex-direction: row-reverse;
}

.avatar {
	width: 64rpx;
	height: 64rpx;
	border-radius: 50%;
	flex-shrink: 0;
}

.message-content {
	flex: 1;
	display: flex;
	flex-direction: column;
	margin: 0 20rpx;
	max-width: 60%;
}

.message-item.own .message-content {
	align-items: flex-end;
}

.bubble {
	background-color: #fff;
	border-radius: 20rpx;
	padding: 20rpx 24rpx;
	box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.1);
	word-wrap: break-word;
	word-break: break-all;
	position: relative;
}

.own-bubble {
	background-color: #007AFF;
	color: #fff;
}

.text {
	font-size: 28rpx;
	color: #333;
	line-height: 1.5;
}

.own-bubble .text {
	color: #fff;
}

.sending-indicator {
	display: flex;
	justify-content: flex-end;
	margin-top: 8rpx;
}

.time {
	font-size: 20rpx;
	color: #999;
	margin-top: 8rpx;
	text-align: center;
}

.message-item.own .time {
	text-align: right;
}

.input-bar {
	display: flex;
	align-items: center;
	padding: 20rpx 30rpx;
	background-color: #fff;
	border-top: 1rpx solid #e5e5e5;
}

.message-input {
	flex: 1;
	margin-right: 20rpx;
	background-color: #f5f5f5;
	border-radius: 24rpx;
	padding: 0 20rpx;
}

.input-actions {
	display: flex;
	align-items: center;
	gap: 20rpx;
}

.send-button {
	border-radius: 12rpx;
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

.loading-more {
	display: flex;
	justify-content: center;
	align-items: center;
	padding: 20rpx 0;
}

.options-container {
	display: flex;
	justify-content: space-around;
	padding: 40rpx 30rpx;
	background-color: #fff;
}

.option-item {
	display: flex;
	flex-direction: column;
	align-items: center;
	padding: 20rpx;
}

.option-text {
	font-size: 24rpx;
	color: #666;
	margin-top: 12rpx;
}
</style>
