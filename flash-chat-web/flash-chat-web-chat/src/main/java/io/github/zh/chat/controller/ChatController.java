package io.github.zh.chat.controller;

import io.github.zh.chat.mapper.ChatUsersMapper;
import io.github.zh.chat.service.ConversationService;
import io.github.zh.chat.service.MessageService;
import io.github.zh.common.response.Response;
import io.github.zh.model.auth.pojo.Users;
import io.github.zh.model.chat.bo.SendMessageBO;
import io.github.zh.model.chat.vo.ConversationVO;
import io.github.zh.model.chat.vo.MessageVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping
public class ChatController {
    @Autowired
    private ConversationService conversationService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private ChatUsersMapper usersMapper;

    /**
     * 获取会话列表
     */
    @GetMapping("/conversation/list")
    public Response<List<ConversationVO>> getConversationList(
            @RequestHeader("userId") String userId) {
        List<ConversationVO> list = conversationService.getConversationList(userId);
        return Response.success(list);
    }

    /**
     * 创建会话
     */
    @PostMapping("/conversation/create")
    public Response<ConversationVO> createConversation(
            @RequestHeader("userId") String userId,
            @RequestParam("targetUserId") String targetUserId) {
        ConversationVO conversation = conversationService.createConversation(userId, targetUserId);
        return Response.success(conversation);
    }

    /**
     * 获取消息列表
     */
    @GetMapping("/message/list")
    public Response<List<MessageVO>> getMessageList(
            @RequestHeader("userId") String userId,
            @RequestParam("conversationId") String conversationId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        List<MessageVO> list = messageService.getMessageList(conversationId, userId, page, size);
        return Response.success(list);
    }

    /**
     * 标记会话已读
     */
    @PutMapping("/conversation/read/{conversationId}")
    public Response<Void> markAsRead(
            @RequestHeader("userId") String userId,
            @PathVariable("conversationId") String conversationId) {
        conversationService.markAsRead(userId, conversationId);
        return Response.success();
    }

    /**
     * 发送消息
     */
    @PostMapping("/message/send")
    public Response<MessageVO> sendMessage(
            @RequestHeader("userId") String userId,
            @RequestBody SendMessageBO bo) {
        MessageVO message = messageService.sendMessage(userId, bo);
        return Response.success(message);
    }

    /**
     * 搜索用户
     */
    @GetMapping("/user/search")
    public Response<List<Map<String, Object>>> searchUser(
            @RequestHeader("userId") String userId,
            @RequestParam("keyword") String keyword) {
        List<Users> users = usersMapper.searchByKeyword(keyword);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Users user : users) {
            if (user.getId().equals(userId)) continue;
            Map<String, Object> item = new HashMap<>();
            item.put("id", user.getId());
            item.put("nickname", user.getNickname());
            item.put("face", user.getFace());
            result.add(item);
        }
        return Response.success(result);
    }

    /**
     * 获取用户信息
     */
    @GetMapping("/user/info/{targetUserId}")
    public Response<Map<String, Object>> getUserInfo(
            @RequestHeader("userId") String userId,
            @PathVariable("targetUserId") String targetUserId) {
        Users user = usersMapper.selectByPrimaryKey(targetUserId);
        if (user == null) {
            return Response.fail("用户不存在");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("nickname", user.getNickname());
        result.put("face", user.getFace());
        return Response.success(result);
    }
}
