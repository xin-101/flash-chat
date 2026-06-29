const AuthAPI = {
  sendCode(phone) { return Api.get('/auth/auth/sendSms', { phone }); },
  login(phone, code) { return Api.post('/auth/auth/login', { phone, code }); },
  logout() { return Api.post('/auth/auth/logout'); },
  updateUserInfo(data) { return Api.put('/auth/auth/user/info', data); },
  updateFlashChatNum(flashChatNum) { return Api.put('/auth/auth/user/flashChatNum', { flashChatNum }); },
};

const ChatAPI = {
  getConversationList() { return Api.get('/chat/conversation/list'); },
  createConversation(targetUserId) { return Api.post(`/chat/conversation/create?targetUserId=${targetUserId}`); },
  getMessageList(conversationId, page, size) { return Api.get('/chat/message/list', { conversationId, page, size }); },
  sendTextMessage(conversationId, content) { return Api.post('/chat/message/send', { conversationId, content, type: 1 }); },
  sendImageMessage(conversationId, content) { return Api.post('/chat/message/send', { conversationId, content, type: 2 }); },
  markAsRead(conversationId) { return Api.put(`/chat/conversation/read/${conversationId}`); },
  searchUser(keyword) { return Api.get('/chat/user/search', { keyword }); },
  getUserInfo(userId) { return Api.get(`/chat/user/info/${userId}`); },
};

const FriendAPI = {
  getFriendList() { return Api.get('/chat/friend/list'); },
  sendRequest(targetUserId, remark) { return Api.post(`/chat/friend/request?targetUserId=${targetUserId}&remark=${encodeURIComponent(remark || '')}`); },
  getRequests() { return Api.get('/chat/friend/requests'); },
  approveRequest(requestId) { return Api.put(`/chat/friend/request/${requestId}/approve`); },
  rejectRequest(requestId) { return Api.put(`/chat/friend/request/${requestId}/reject`); },
  deleteFriend(friendId) { return Api.delete(`/chat/friend/${friendId}`); },
  setRemark(friendId, remark) { return Api.put(`/chat/friend/${friendId}/remark?remark=${encodeURIComponent(remark)}`); },
  block(friendId) { return Api.put(`/chat/friend/${friendId}/block`); },
  unblock(friendId) { return Api.put(`/chat/friend/${friendId}/unblock`); },
};

const GroupAPI = {
  create(name, memberIds) { return Api.post('/chat/group/create', { name, memberIds }); },
  invite(conversationId, memberIds) { return Api.post('/chat/group/invite', { conversationId, memberIds }); },
  removeMember(conversationId, targetUserId) { return Api.delete('/chat/group/member?conversationId=' + conversationId + '&targetUserId=' + targetUserId); },
  transferOwner(conversationId, newOwnerId) { return Api.put('/chat/group/transfer', { conversationId, newOwnerId }); },
  updateName(conversationId, name) { return Api.put('/chat/group/name', { conversationId, name }); },
  getMembers(conversationId) { return Api.get('/chat/group/members', { conversationId }); },
  getMembersDetail(conversationId) { return Api.get('/chat/group/members/detail', { conversationId }); },
  getInfo(conversationId) { return Api.get('/chat/group/info/' + conversationId); },
};

const MomentAPI = {
  create(content, images) { return Api.post('/chat/moment/create', { content, images: images || [] }); },
  delete(momentId) { return Api.delete('/chat/moment/' + momentId); },
  list(page, size) { return Api.get('/chat/moment/list', { page, size }); },
  like(momentId) { return Api.post('/chat/moment/' + momentId + '/like'); },
  unlike(momentId) { return Api.delete('/chat/moment/' + momentId + '/like'); },
  comment(momentId, content, replyUserId) {
    const body = { content };
    if (replyUserId) body.replyUserId = replyUserId;
    return Api.post('/chat/moment/' + momentId + '/comment', body);
  },
  deleteComment(commentId) { return Api.delete('/chat/moment/comment/' + commentId); },
};

const FileAPI = {
  upload(file, dir) {
    const form = new FormData();
    form.append('file', file);
    if (dir) form.append('dir', dir);
    const token = Storage.getToken();
    const userId = Storage.getUserId();
    const headers = {};
    if (token) headers['Authorization'] = 'Bearer ' + token;
    if (userId) headers['userId'] = userId;
    return fetch(AppConfig.baseUrl + '/file/upload', { method: 'POST', headers, body: form }).then(r => r.json());
  },
};

const Api = {
  async request(method, path, body) {
    const headers = { 'Content-Type': 'application/json' };
    const token = Storage.getToken();
    const userId = Storage.getUserId();
    if (token) headers['Authorization'] = 'Bearer ' + token;
    if (userId) headers['userId'] = userId;
    const opts = { method, headers };
    if (body && method !== 'GET') opts.body = JSON.stringify(body);
    const res = await fetch(AppConfig.baseUrl + path, opts);
    if (res.status === 401) { Storage.clearAuth(); window.location.reload(); throw new Error('登录已过期'); }
    const data = await res.json();
    if (data.success === false) throw new Error(data.message || '请求失败');
    return data;
  },
  get(path, params) {
    const qs = Object.entries(params||{}).map(([k,v])=>`${k}=${encodeURIComponent(v)}`).join('&');
    return this.request('GET', qs ? path+'?'+qs : path);
  },
  post(path, body) { return this.request('POST', path, body); },
  put(path, body) { return this.request('PUT', path, body); },
  delete(path) { return this.request('DELETE', path); },
};
