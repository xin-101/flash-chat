<script>
import { connectWebSocket, closeWebSocket } from './utils/websocket.js';
import { getToken, getUserInfo, isLoggedIn } from './utils/storage.js';
import { BASE_URL } from './config/env.js';

export default {
	onLaunch: function() {
		console.log('App Launch');
		
		// 检查登录状态
		this.checkLoginStatus();
	},
	onShow: function() {
		console.log('App Show');
	},
	onHide: function() {
		console.log('App Hide');
	},
	onError: function(err) {
		console.error('App Error:', err);
		// 可以在这里添加全局错误处理，如上报错误日志
	},
	methods: {
		// 检查登录状态
		checkLoginStatus() {
			const token = getToken();
			const userInfo = getUserInfo();
			
			// 如果已登录，尝试连接WebSocket
			if (token && userInfo && userInfo.id) {
				// 延迟连接，确保应用完全启动
				setTimeout(() => {
					this.connectWebSocketIfNeeded();
				}, 1000);
			} else {
				// 未登录，跳转到登录页
				const pages = getCurrentPages();
				if (pages.length === 0 || pages[pages.length - 1].route !== 'pages/login/index') {
					uni.reLaunch({
						url: '/pages/login/index'
					});
				}
			}
		},
		
		// 连接WebSocket（如果需要）
		connectWebSocketIfNeeded() {
			const token = getToken();
			const userInfo = getUserInfo();
			
			if (!token || !userInfo) {
				return;
			}
			
			// WebSocket 路径：通过网关 /chat/ws，网关会去掉 /chat 前缀变成 /ws
			const wsUrl = BASE_URL.replace('http://', 'ws://').replace('https://', 'wss://') + '/chat/ws';
			
			// 连接WebSocket
			connectWebSocket(wsUrl, token, true);
		}
	}
}
</script>

<style>
/*每个页面公共css */
page {
	background-color: #f5f5f5;
	font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Oxygen', 'Ubuntu', 'Cantarell', 'Fira Sans', 'Droid Sans', 'Helvetica Neue', sans-serif;
}

/* 全局样式重置 */
page, view, text, image, input, button, navigator, swiper, scroll-view, rich-text, label, picker, picker-view, radio-group, radio, checkbox-group, checkbox, switch, textarea, slider, progress, icon, map, canvas, video, audio, camera, live-player, live-pusher, ad, web-view {
	box-sizing: border-box;
}

/* 统一按钮样式 */
button::after {
	border: none;
}
</style>