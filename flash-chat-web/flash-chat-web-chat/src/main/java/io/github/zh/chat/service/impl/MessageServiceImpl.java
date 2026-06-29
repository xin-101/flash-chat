package io.github.zh.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.zh.chat.mapper.ChatUsersMapper;
import io.github.zh.chat.config.RabbitMQConfig;
import io.github.zh.chat.mapper.ConversationMapper;
import io.github.zh.chat.mapper.ConversationMemberMapper;
import io.github.zh.chat.mapper.MessageMapper;
import io.github.zh.chat.mapper.UserFriendMapper;
import io.github.zh.chat.service.MessageService;
import io.github.zh.common.enums.MessageTypeEnum;
import io.github.zh.common.enums.StatusEnum;
import io.github.zh.common.enums.ConversationTypeEnum;
import io.github.zh.common.exception.BizException;
import io.github.zh.model.auth.pojo.Users;

import io.github.zh.model.chat.bo.SendMessageBO;
import io.github.zh.model.chat.pojo.Conversation;
import io.github.zh.model.chat.pojo.ConversationMember;
import io.github.zh.model.chat.pojo.Message;
import io.github.zh.model.chat.pojo.UserFriend;
import io.github.zh.model.chat.vo.MessageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private ConversationMapper conversationMapper;

    @Autowired
    private ConversationMemberMapper conversationMemberMapper;

    @Autowired
    private UserFriendMapper userFriendMapper;

    @Autowired
    private ChatUsersMapper usersMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public List<MessageVO> getMessageList(String conversationId, String userId, int page, int size) {
        // 1. 查询消息列表
        Page<Message> messagePage = new Page<>(page, size);
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getConversationId, conversationId)
                .eq(Message::getStatus, StatusEnum.NORMAL.getCode())
                .orderByDesc(Message::getCreateTime);
        Page<Message> result = messageMapper.selectPage(messagePage, wrapper);

        // 2. 转换为VO
        return result.getRecords().stream().map(message -> {
            MessageVO vo = new MessageVO();
            BeanUtils.copyProperties(message, vo);

            // 查询发送者信息
            Users sender = usersMapper.selectById(message.getSenderId());
            if (sender != null) {
                vo.setSenderName(sender.getNickname());
                vo.setSenderFace(sender.getFace());
            }

            // 判断是否是自己发送的
            vo.setMe(message.getSenderId().equals(userId));

            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MessageVO sendMessage(String userId, SendMessageBO bo) {
        // 1. 验证会话是否存在
        Conversation conversation = conversationMapper.selectById(bo.getConversationId());
        if (conversation == null) {
            throw new BizException("50001", "会话不存在");
        }

        // 2. 验证是否是会话成员
        LambdaQueryWrapper<ConversationMember> memberWrapper = new LambdaQueryWrapper<>();
        memberWrapper.eq(ConversationMember::getConversationId, bo.getConversationId())
                .eq(ConversationMember::getUserId, userId);
        if (conversationMemberMapper.selectCount(memberWrapper) == 0) {
            throw new BizException("50002", "无权访问该会话");
        }

        // 2.5 单聊校验：对方是否拉黑或删除了你
        if (ConversationTypeEnum.SINGLE.getCode().equals(conversation.getType())) {
            LambdaQueryWrapper<ConversationMember> otherWrapper = new LambdaQueryWrapper<>();
            otherWrapper.eq(ConversationMember::getConversationId, bo.getConversationId())
                    .ne(ConversationMember::getUserId, userId);
            ConversationMember otherMember = conversationMemberMapper.selectOne(otherWrapper);
            if (otherMember != null) {
                String otherUserId = otherMember.getUserId();
                // 检查对方是否拉黑了你
                LambdaQueryWrapper<UserFriend> blockCheck = new LambdaQueryWrapper<>();
                blockCheck.eq(UserFriend::getUserId, otherUserId)
                        .eq(UserFriend::getFriendId, userId)
                        .eq(UserFriend::getIsBlock, 1);
                if (userFriendMapper.selectCount(blockCheck) > 0) {
                    throw new BizException("50004", "对方已将你拉黑");
                }
                // 检查好友关系是否存在（任一方删除即不存在）
                LambdaQueryWrapper<UserFriend> friendCheck = new LambdaQueryWrapper<>();
                friendCheck.eq(UserFriend::getUserId, userId)
                        .eq(UserFriend::getFriendId, otherUserId);
                if (userFriendMapper.selectCount(friendCheck) == 0) {
                    throw new BizException("50005", "对方已不是你的好友");
                }
            }
        }

        // 3. 保存消息
        String messageId = UUID.randomUUID().toString().replace("-", "");
        Message message = new Message();
        message.setId(messageId);
        message.setConversationId(bo.getConversationId());
        message.setSenderId(userId);
        message.setContent(bo.getContent());
        message.setType(bo.getType() != null ? bo.getType() : MessageTypeEnum.TEXT.getCode());
        message.setStatus(StatusEnum.NORMAL.getCode());
        message.setCreateTime(new Date());
        messageMapper.insert(message);

        // 4. 更新会话最后消息
        conversation.setLastMessageId(messageId);
        conversation.setLastMessageContent(bo.getContent());
        conversation.setLastMessageTime(new Date());
        conversation.setUpdateTime(new Date());
        conversationMapper.updateById(conversation);

        // 5. 更新其他成员未读数（累加+1）
        UpdateWrapper<ConversationMember> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("conversation_id", bo.getConversationId())
                .ne("user_id", userId)
                .setSql("unread_count = unread_count + 1");
        conversationMemberMapper.update(null, updateWrapper);

        // 6. 查询发送者信息
        Users sender = usersMapper.selectById(userId);

        // 7. 发送到RabbitMQ（各实例监听后广播给WebSocket客户端）
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "message");
            payload.put("id", messageId);
            payload.put("conversationId", bo.getConversationId());
            payload.put("senderId", userId);
            payload.put("content", bo.getContent());
            payload.put("msgType", message.getType());
            payload.put("createTime", message.getCreateTime());
            if (sender != null) {
                payload.put("senderName", sender.getNickname());
                payload.put("senderFace", sender.getFace());
            }
            String json = objectMapper.writeValueAsString(payload);
            log.info("[Chat] 发布消息到RabbitMQ: conversationId={}, sender={}", bo.getConversationId(), userId);
            rabbitTemplate.convertAndSend(RabbitMQConfig.CHAT_EXCHANGE, "", json);
        } catch (Exception e) {
            log.error("[Chat] 发布消息到RabbitMQ失败", e);
        }

        // 8. 返回消息VO
        MessageVO vo = new MessageVO();
        BeanUtils.copyProperties(message, vo);
        vo.setMe(true);

        if (sender != null) {
            vo.setSenderName(sender.getNickname());
            vo.setSenderFace(sender.getFace());
        }

        return vo;
    }
}