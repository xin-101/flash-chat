const StorageKeys = {
  TOKEN: 'flash_chat_token',
  USER: 'flash_chat_user',
};

const Storage = {
  setToken(token) { localStorage.setItem(StorageKeys.TOKEN, token); },
  getToken() { return localStorage.getItem(StorageKeys.TOKEN) || ''; },
  removeToken() { localStorage.removeItem(StorageKeys.TOKEN); },

  setUserInfo(info) { localStorage.setItem(StorageKeys.USER, JSON.stringify(info)); },
  getUserInfo() { try { return JSON.parse(localStorage.getItem(StorageKeys.USER)); } catch { return null; } },
  getUserId() { const info = this.getUserInfo(); return info ? info.id || '' : ''; },
  removeUserInfo() { localStorage.removeItem(StorageKeys.USER); },

  clearAuth() { this.removeToken(); this.removeUserInfo(); },
  isLoggedIn() { return !!this.getToken() && !!this.getUserId(); },
};

function formatChatTime(time) {
  if (!time) return '';
  const date = new Date(time);
  const now = new Date();
  const diff = now - date;
  if (diff < 60 * 1000) return '刚刚';

  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const targetDay = new Date(date.getFullYear(), date.getMonth(), date.getDate());
  const dayDiff = Math.floor((today - targetDay) / (24 * 60 * 60 * 1000));

  const hh = String(date.getHours()).padStart(2, '0');
  const mm = String(date.getMinutes()).padStart(2, '0');

  if (dayDiff === 0) return `${hh}:${mm}`;
  if (dayDiff === 1) return `昨天 ${hh}:${mm}`;
  if (dayDiff < 7) {
    const weekdays = ['日', '一', '二', '三', '四', '五', '六'];
    return `周${weekdays[date.getDay()]} ${hh}:${mm}`;
  }
  const M = String(date.getMonth() + 1).padStart(2, '0');
  const D = String(date.getDate()).padStart(2, '0');
  return `${M}-${D} ${hh}:${mm}`;
}

function formatMessageTime(time) {
  if (!time) return '';
  const date = new Date(time);
  const now = new Date();
  const diff = now - date;
  if (diff < 5 * 60 * 1000) return '';
  const hh = String(date.getHours()).padStart(2, '0');
  const mm = String(date.getMinutes()).padStart(2, '0');
  return `${hh}:${mm}`;
}

function formatBriefTime(time) {
  if (!time) return '';
  const date = new Date(time);
  const now = new Date();
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const targetDay = new Date(date.getFullYear(), date.getMonth(), date.getDate());
  const dayDiff = Math.floor((today - targetDay) / (24 * 60 * 60 * 1000));

  const hh = String(date.getHours()).padStart(2, '0');
  const mm = String(date.getMinutes()).padStart(2, '0');

  if (dayDiff === 0) return `${hh}:${mm}`;
  if (dayDiff === 1) return '昨天';
  if (dayDiff < 7) {
    const weekdays = ['日', '一', '二', '三', '四', '五', '六'];
    return `周${weekdays[date.getDay()]}`;
  }
  const M = String(date.getMonth() + 1).padStart(2, '0');
  const D = String(date.getDate()).padStart(2, '0');
  return `${M}-${D}`;
}

function escHtml(s) {
  const d = document.createElement('div');
  d.textContent = s;
  return d.innerHTML;
}

const DefaultAvatar = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 40 40'%3E%3Ccircle cx='20' cy='20' r='20' fill='%23e0e0e0'/%3E%3Ccircle cx='20' cy='16' r='7' fill='%23ccc'/%3E%3Cpath d='M8 35c0-7 5-12 12-12s12 5 12 12' fill='%23ccc'/%3E%3C/svg%3E";

function imgUrl(url) {
  if (!url) return DefaultAvatar;
  if (url.startsWith('http') || url.startsWith('data:')) return url;
  if (url.startsWith('/')) return AppConfig.baseUrl + url;
  return AppConfig.baseUrl + '/' + url;
}
