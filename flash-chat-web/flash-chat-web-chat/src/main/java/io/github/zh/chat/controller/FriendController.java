package io.github.zh.chat.controller;

import io.github.zh.chat.service.FriendService;
import io.github.zh.common.response.PageResult;
import io.github.zh.common.response.Response;
import io.github.zh.model.chat.vo.FriendRequestVO;
import io.github.zh.model.chat.vo.UserFriendVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
public class FriendController {

    private final FriendService friendService;

    public FriendController(FriendService friendService) {
        this.friendService = friendService;
    }

    @PostMapping("/friend/request")
    public Response<Void> sendRequest(@RequestHeader("userId") String userId,
                                      @RequestParam("targetUserId") String targetUserId,
                                      @RequestParam(value = "remark", defaultValue = "") String remark) {
        friendService.sendRequest(userId, targetUserId, remark);
        return Response.success();
    }

    @PutMapping("/friend/request/{requestId}/approve")
    public Response<Void> approveRequest(@RequestHeader("userId") String userId,
                                         @PathVariable("requestId") String requestId) {
        friendService.approveRequest(userId, requestId);
        return Response.success();
    }

    @PutMapping("/friend/request/{requestId}/reject")
    public Response<Void> rejectRequest(@RequestHeader("userId") String userId,
                                        @PathVariable("requestId") String requestId) {
        friendService.rejectRequest(userId, requestId);
        return Response.success();
    }

    @GetMapping("/friend/requests")
    public Response<List<FriendRequestVO>> getIncomingRequests(@RequestHeader("userId") String userId) {
        return Response.success(friendService.getIncomingRequests(userId));
    }

    @GetMapping("/friend/requests/page")
    public Response<PageResult<FriendRequestVO>> getIncomingRequestsPage(
            @RequestHeader("userId") String userId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return Response.success(friendService.getIncomingRequests(userId, page, size));
    }

    @GetMapping("/friend/list")
    public Response<List<UserFriendVO>> getFriendList(@RequestHeader("userId") String userId) {
        return Response.success(friendService.getFriendList(userId));
    }

    @DeleteMapping("/friend/{friendId}")
    public Response<Void> deleteFriend(@RequestHeader("userId") String userId,
                                       @PathVariable("friendId") String friendId) {
        friendService.deleteFriend(userId, friendId);
        return Response.success();
    }

    @PutMapping("/friend/{friendId}/remark")
    public Response<Void> setRemark(@RequestHeader("userId") String userId,
                                    @PathVariable("friendId") String friendId,
                                    @RequestParam("remark") String remark) {
        friendService.setRemark(userId, friendId, remark);
        return Response.success();
    }

    @PutMapping("/friend/{friendId}/block")
    public Response<Void> blockFriend(@RequestHeader("userId") String userId,
                                      @PathVariable("friendId") String friendId) {
        friendService.blockFriend(userId, friendId);
        return Response.success();
    }

    @PutMapping("/friend/{friendId}/unblock")
    public Response<Void> unblockFriend(@RequestHeader("userId") String userId,
                                        @PathVariable("friendId") String friendId) {
        friendService.unblockFriend(userId, friendId);
        return Response.success();
    }
}
