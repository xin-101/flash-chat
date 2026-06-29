package io.github.zh.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import io.github.zh.chat.mapper.ChatUsersMapper;
import io.github.zh.chat.mapper.ConversationMapper;
import io.github.zh.chat.mapper.ConversationMemberMapper;
import io.github.zh.chat.mapper.UserFriendMapper;
import io.github.zh.chat.service.ConversationService;
import io.github.zh.common.enums.ConversationTypeEnum;
import io.github.zh.model.chat.pojo.Conversation;
import io.github.zh.model.chat.pojo.ConversationMember;
import io.github.zh.model.chat.pojo.UserFriend;
import io.github.zh.model.chat.vo.ConversationVO;
import io.github.zh.model.auth.pojo.Users;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ConversationServiceImpl implements ConversationService {

    @Autowired
    private ConversationMapper conversationMapper;

    @Autowired
    private ConversationMemberMapper conversationMemberMapper;

    @Autowired
    private ChatUsersMapper usersMapper;

    @Autowired
    private UserFriendMapper userFriendMapper;

    @Override
    public List<ConversationVO> getConversationList(String userId) {
        // 1. 查询用户参与的所有会话成员记录
        LambdaQueryWrapper<ConversationMember> memberWrapper = new LambdaQueryWrapper<>();
        memberWrapper.eq(ConversationMember::getUserId, userId);
        List<ConversationMember> members = conversationMemberMapper.selectList(memberWrapper);
        if (members.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 批量查询会话
        List<String> conversationIds = members.stream()
                .map(ConversationMember::getConversationId)
                .collect(Collectors.toList());
        List<Conversation> conversations = conversationMapper.selectBatchIds(conversationIds);
        Map<String, Conversation> conversationMap = conversations.stream()
                .collect(Collectors.toMap(Conversation::getId, Function.identity()));

        // 3. 批量查询所有会话成员（用于单聊查找对方用户）
        List<Conversation> singleConversations = conversations.stream()
                .filter(c -> ConversationTypeEnum.SINGLE.getCode().equals(c.getType())).collect(Collectors.toList());

        Map<String, String> conversationOtherUserMap = new HashMap<>();
        if (!singleConversations.isEmpty()) {
            LambdaQueryWrapper<ConversationMember> allMembersWrapper = new LambdaQueryWrapper<>();
            allMembersWrapper.in(ConversationMember::getConversationId,
                    singleConversations.stream().map(Conversation::getId).collect(Collectors.toList()))
                    .ne(ConversationMember::getUserId, userId);
            List<ConversationMember> otherMembers = conversationMemberMapper.selectList(allMembersWrapper);
            for (ConversationMember m : otherMembers) {
                conversationOtherUserMap.put(m.getConversationId(), m.getUserId());
            }
        }

        // 4. 批量查询用户信息
        Map<String, Users> userMap = Collections.emptyMap();
        if (!conversationOtherUserMap.isEmpty()) {
            List<String> allOtherUserIds = new ArrayList<>(new HashSet<>(conversationOtherUserMap.values()));
            List<Users> userList = usersMapper.selectByIds(allOtherUserIds);
            userMap = userList.stream()
                    .collect(Collectors.toMap(Users::getId, Function.identity()));
        }

        // 5. 组装VO
        List<ConversationVO> result = new ArrayList<>();
        for (ConversationMember member : members) {
            Conversation conversation = conversationMap.get(member.getConversationId());
            if (conversation == null) {
                continue;
            }

            ConversationVO vo = new ConversationVO();
            vo.setId(conversation.getId());
            vo.setLastMessage(conversation.getLastMessageContent());
            vo.setLastMessageTime(conversation.getLastMessageTime());
            vo.setUnreadCount(member.getUnreadCount());

            if (ConversationTypeEnum.SINGLE.getCode().equals(conversation.getType())) {
                String otherUserId = conversationOtherUserMap.get(member.getConversationId());
                if (otherUserId != null) {
                    Users user = userMap.get(otherUserId);
                    if (user != null) {
                        vo.setName(user.getNickname());
                        vo.setFace(user.getFace());
                    }
                }
            }

            result.add(vo);
        }

        return result;
    }

    @Override
    public void markAsRead(String userId, String conversationId) {
        UpdateWrapper<ConversationMember> wrapper = new UpdateWrapper<>();
        wrapper.eq("conversation_id", conversationId)
                .eq("user_id", userId)
                .setSql("unread_count = 0");
        conversationMemberMapper.update(null, wrapper);
    }

    @Override
    @Transactional
    public ConversationVO createConversation(String userId, String targetUserId) {
        // 0. 校验好友关系（双向）
        LambdaQueryWrapper<UserFriend> friendCheck = new LambdaQueryWrapper<>();
        friendCheck.eq(UserFriend::getUserId, userId).eq(UserFriend::getFriendId, targetUserId);
        if (userFriendMapper.selectCount(friendCheck) == 0) {
            throw new RuntimeException("只有好友才能聊天");
        }

        // 1. 检查是否已存在会话
        LambdaQueryWrapper<ConversationMember> memberWrapper = new LambdaQueryWrapper<>();
        memberWrapper.eq(ConversationMember::getUserId, userId);
        List<ConversationMember> userMembers = conversationMemberMapper.selectList(memberWrapper);

        for (ConversationMember userMember : userMembers) {
            LambdaQueryWrapper<ConversationMember> otherWrapper = new LambdaQueryWrapper<>();
            otherWrapper.eq(ConversationMember::getConversationId, userMember.getConversationId())
                    .eq(ConversationMember::getUserId, targetUserId);
            if (conversationMemberMapper.selectCount(otherWrapper) > 0) {
                // 已存在会话，返回
                Conversation conversation = conversationMapper.selectById(userMember.getConversationId());
                return buildConversationVO(conversation, userId);
            }
        }

        // 2. 创建新会话
        String conversationId = UUID.randomUUID().toString().replace("-", "");
        Conversation conversation = new Conversation();
        conversation.setId(conversationId);
        conversation.setType(ConversationTypeEnum.SINGLE.getCode()); // 单聊
        conversation.setLastMessageTime(new Date());
        conversation.setCreateTime(new Date());
        conversation.setUpdateTime(new Date());
        conversationMapper.insert(conversation);

        // 3. 添加会话成员
        ConversationMember member1 = new ConversationMember();
        member1.setId(UUID.randomUUID().toString().replace("-", ""));
        member1.setConversationId(conversationId);
        member1.setUserId(userId);
        member1.setUnreadCount(0);
        member1.setIsTop(0);
        member1.setIsMute(0);
        member1.setCreatedTime(new Date());
        conversationMemberMapper.insert(member1);

        ConversationMember member2 = new ConversationMember();
        member2.setId(UUID.randomUUID().toString().replace("-", ""));
        member2.setConversationId(conversationId);
        member2.setUserId(targetUserId);
        member2.setUnreadCount(0);
        member2.setIsTop(0);
        member2.setIsMute(0);
        member2.setCreatedTime(new Date());
        conversationMemberMapper.insert(member2);

        // 4. 返回会话信息
        return buildConversationVO(conversation, userId);
    }

    private ConversationVO buildConversationVO(Conversation conversation, String userId) {
        ConversationVO vo = new ConversationVO();
        vo.setId(conversation.getId());
        vo.setLastMessage(conversation.getLastMessageContent());
        vo.setLastMessageTime(conversation.getLastMessageTime());

        if (ConversationTypeEnum.SINGLE.getCode().equals(conversation.getType())) {
            String otherUserId = getOtherUserId(conversation.getId(), userId);
            if (otherUserId != null) {
                Users user = usersMapper.selectById(otherUserId);
                if (user != null) {
                    vo.setName(user.getNickname());
                    vo.setFace(user.getFace());
                }
            }
        }

        return vo;
    }

    private String getOtherUserId(String conversationId, String userId) {
        LambdaQueryWrapper<ConversationMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationMember::getConversationId, conversationId)
                .ne(ConversationMember::getUserId, userId);
        ConversationMember member = conversationMemberMapper.selectOne(wrapper);
        return member != null ? member.getUserId() : null;
    }
}