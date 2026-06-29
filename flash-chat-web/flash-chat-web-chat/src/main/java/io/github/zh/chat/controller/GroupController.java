package io.github.zh.chat.controller;

import io.github.zh.chat.mapper.ChatUsersMapper;
import io.github.zh.chat.service.GroupService;
import io.github.zh.common.response.Response;
import io.github.zh.model.auth.pojo.Users;
import io.github.zh.model.chat.vo.ConversationVO;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping
public class GroupController {

    private final GroupService groupService;
    private final ChatUsersMapper usersMapper;

    public GroupController(GroupService groupService, ChatUsersMapper usersMapper) {
        this.groupService = groupService;
        this.usersMapper = usersMapper;
    }

    @PostMapping("/group/create")
    public Response<ConversationVO> create(@RequestHeader("userId") String userId,
                                           @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        Object rawIds = body.get("memberIds");
        List<String> memberIds = null;
        if (rawIds instanceof List) {
            memberIds = ((List<?>) rawIds).stream().map(Object::toString).collect(Collectors.toList());
        }
        return Response.success(groupService.create(userId, name, memberIds));
    }

    @PostMapping("/group/invite")
    public Response<Void> invite(@RequestHeader("userId") String userId,
                                 @RequestBody Map<String, Object> body) {
        String conversationId = (String) body.get("conversationId");
        Object rawIds = body.get("memberIds");
        List<String> memberIds = null;
        if (rawIds instanceof List) {
            memberIds = ((List<?>) rawIds).stream().map(Object::toString).collect(Collectors.toList());
        }
        groupService.invite(userId, conversationId, memberIds);
        return Response.success();
    }

    @DeleteMapping("/group/member")
    public Response<Void> removeMember(@RequestHeader("userId") String userId,
                                       @RequestBody Map<String, String> body) {
        groupService.removeMember(userId, body.get("conversationId"), body.get("targetUserId"));
        return Response.success();
    }

    @PutMapping("/group/transfer")
    public Response<Void> transferOwner(@RequestHeader("userId") String userId,
                                        @RequestBody Map<String, String> body) {
        groupService.transferOwner(userId, body.get("conversationId"), body.get("newOwnerId"));
        return Response.success();
    }

    @PutMapping("/group/name")
    public Response<Void> updateName(@RequestHeader("userId") String userId,
                                     @RequestBody Map<String, String> body) {
        groupService.updateName(userId, body.get("conversationId"), body.get("name"));
        return Response.success();
    }

    @GetMapping("/group/members")
    public Response<List<String>> getMemberIds(@RequestHeader("userId") String userId,
                                               @RequestParam("conversationId") String conversationId) {
        return Response.success(groupService.getMemberIds(conversationId));
    }

    @GetMapping("/group/members/detail")
    public Response<List<Map<String, Object>>> getMemberDetail(@RequestHeader("userId") String userId,
                                                               @RequestParam("conversationId") String conversationId) {
        List<String> ids = groupService.getMemberIds(conversationId);
        List<Users> users = usersMapper.selectByIds(ids);
        return Response.success(users.stream().map(u -> {
            Map<String, Object> m = new HashMap<>();
            m.put("userId", u.getId());
            m.put("nickname", u.getNickname());
            m.put("face", u.getFace());
            return m;
        }).collect(Collectors.toList()));
    }

    @GetMapping("/group/info/{conversationId}")
    public Response<Map<String, Object>> getGroupInfo(@RequestHeader("userId") String userId,
                                                      @PathVariable("conversationId") String conversationId) {
        String ownerId = groupService.getOwnerId(conversationId);
        List<String> memberIds = groupService.getMemberIds(conversationId);
        Map<String, Object> result = new HashMap<>();
        result.put("ownerId", ownerId);
        result.put("memberCount", memberIds.size());
        return Response.success(result);
    }
}
