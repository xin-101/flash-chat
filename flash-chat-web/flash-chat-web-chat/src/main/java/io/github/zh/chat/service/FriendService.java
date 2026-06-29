package io.github.zh.chat.service;

import io.github.zh.common.response.PageResult;
import io.github.zh.model.chat.vo.FriendRequestVO;
import io.github.zh.model.chat.vo.UserFriendVO;

import java.util.List;

public interface FriendService {

    void sendRequest(String userId, String targetUserId, String remark);

    void approveRequest(String userId, String requestId);

    void rejectRequest(String userId, String requestId);

    List<FriendRequestVO> getIncomingRequests(String userId);

    PageResult<FriendRequestVO> getIncomingRequests(String userId, int page, int size);

    List<UserFriendVO> getFriendList(String userId);

    void deleteFriend(String userId, String friendId);

    void setRemark(String userId, String friendId, String remark);

    void blockFriend(String userId, String friendId);

    void unblockFriend(String userId, String friendId);
}
