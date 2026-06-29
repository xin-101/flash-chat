import { get, post, put, del } from '../utils/request'

export function getConversationList() {
  return get('/chat/conversation/list')
}

export function createConversation(targetUserId) {
  return post(`/chat/conversation/create?targetUserId=${targetUserId}`)
}

export function getMessageList(conversationId, page = 1, size = 20) {
  return get('/chat/message/list', { conversationId, page, size })
}

export function sendTextMessage(conversationId, content) {
  return post('/chat/message/send', { conversationId, content, type: 1 })
}

export function sendImageMessage(conversationId, imageUrl) {
  return post('/chat/message/send', { conversationId, content: imageUrl, type: 2 })
}

export function searchUser(keyword) {
  return get('/chat/user/search', { keyword })
}

export function getUserInfo(userId) {
  return get(`/chat/user/info/${userId}`)
}

export function markConversationRead(conversationId) {
  return put(`/chat/conversation/read/${conversationId}`)
}

export function sendFriendRequest(targetUserId, remark = '') {
  return post(`/chat/friend/request?targetUserId=${targetUserId}&remark=${encodeURIComponent(remark)}`)
}

export function approveFriendRequest(requestId) {
  return put(`/chat/friend/request/${requestId}/approve`)
}

export function rejectFriendRequest(requestId) {
  return put(`/chat/friend/request/${requestId}/reject`)
}

export function getFriendRequests() {
  return get('/chat/friend/requests')
}
export function getFriendRequestsPage(page = 1, size = 20) {
  return get('/chat/friend/requests/page', { page, size })
}

export function getFriendList() {
  return get('/chat/friend/list')
}

export function deleteFriend(friendId) {
  return del(`/chat/friend/${friendId}`)
}

export function setFriendRemark(friendId, remark) {
  return put(`/chat/friend/${friendId}/remark?remark=${encodeURIComponent(remark)}`)
}

export function blockFriend(friendId) {
  return put(`/chat/friend/${friendId}/block`)
}

export function unblockFriend(friendId) {
  return put(`/chat/friend/${friendId}/unblock`)
}

export function getMomentList(page = 1, size = 10) {
  return get('/chat/moment/list', { page, size })
}

export function createMoment(content, images = []) {
  return post('/chat/moment/create', { content, images })
}

export function deleteMoment(momentId) {
  return del(`/chat/moment/${momentId}`)
}

export function likeMoment(momentId) {
  return post(`/chat/moment/${momentId}/like`)
}

export function unlikeMoment(momentId) {
  return del(`/chat/moment/${momentId}/like`)
}

export function commentMoment(momentId, content, replyUserId = '') {
  return post(`/chat/moment/${momentId}/comment`, { content, replyUserId })
}

export function deleteMomentComment(commentId) {
  return del(`/chat/moment/comment/${commentId}`)
}

export function createGroup(name, memberIds) {
  return post('/chat/group/create', { name, memberIds })
}

export function inviteGroupMembers(conversationId, memberIds) {
  return post('/chat/group/invite', { conversationId, memberIds })
}

export function removeGroupMember(conversationId, targetUserId) {
  return del('/chat/group/member', { conversationId, targetUserId })
}

export function transferGroup(conversationId, newOwnerId) {
  return put('/chat/group/transfer', { conversationId, newOwnerId })
}

export function updateGroupName(conversationId, name) {
  return put('/chat/group/name', { conversationId, name })
}

export function getGroupMembers(conversationId) {
  return get('/chat/group/members', { conversationId })
}

export function getGroupMemberDetail(conversationId) {
  return get('/chat/group/members/detail', { conversationId })
}

export function getGroupInfo(conversationId) {
  return get(`/chat/group/info/${conversationId}`)
}
