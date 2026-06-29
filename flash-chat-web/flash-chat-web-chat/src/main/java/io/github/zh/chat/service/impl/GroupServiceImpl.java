package io.github.zh.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.zh.chat.mapper.ConversationMapper;
import io.github.zh.chat.mapper.ConversationMemberMapper;
import io.github.zh.chat.service.GroupService;
import io.github.zh.common.enums.ConversationTypeEnum;
import io.github.zh.common.exception.BizException;
import io.github.zh.model.chat.pojo.Conversation;
import io.github.zh.model.chat.pojo.ConversationMember;
import io.github.zh.model.chat.vo.ConversationVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GroupServiceImpl implements GroupService {

    private final ConversationMapper conversationMapper;
    private final ConversationMemberMapper conversationMemberMapper;

    public GroupServiceImpl(ConversationMapper conversationMapper,
                            ConversationMemberMapper conversationMemberMapper) {
        this.conversationMapper = conversationMapper;
        this.conversationMemberMapper = conversationMemberMapper;
    }

    @Override
    @Transactional
    public ConversationVO create(String userId, String name, List<String> memberIds) {
        if (name == null || name.trim().isEmpty()) {
            throw new BizException("50001", "群名称不能为空");
        }
        if (memberIds == null || memberIds.isEmpty()) {
            throw new BizException("50001", "请选择群成员");
        }

        String conversationId = UUID.randomUUID().toString().replace("-", "");
        Date now = new Date();

        Conversation conversation = new Conversation();
        conversation.setId(conversationId);
        conversation.setType(ConversationTypeEnum.GROUP.getCode());
        conversation.setName(name.trim());
        conversation.setOwnerId(userId);
        conversation.setLastMessageTime(now);
        conversation.setCreateTime(now);
        conversation.setUpdateTime(now);
        conversationMapper.insert(conversation);

        List<String> allIds = new ArrayList<>(memberIds);
        if (!allIds.contains(userId)) {
            allIds.add(0, userId);
        }

        for (String id : allIds) {
            ConversationMember member = new ConversationMember();
            member.setId(UUID.randomUUID().toString().replace("-", ""));
            member.setConversationId(conversationId);
            member.setUserId(id);
            member.setUnreadCount(0);
            member.setIsTop(0);
            member.setIsMute(0);
            member.setCreatedTime(now);
            conversationMemberMapper.insert(member);
        }

        log.info("群聊创建成功: conversationId={}, name={}, owner={}", conversationId, name, userId);
        ConversationVO vo = new ConversationVO();
        vo.setId(conversationId);
        vo.setName(name.trim());
        vo.setType(ConversationTypeEnum.GROUP.getCode());
        return vo;
    }

    @Override
    @Transactional
    public void invite(String userId, String conversationId, List<String> newMemberIds) {
        if (newMemberIds == null || newMemberIds.isEmpty()) {
            throw new BizException("50001", "请选择要邀请的成员");
        }
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null || !ConversationTypeEnum.GROUP.getCode().equals(conversation.getType())) {
            throw new BizException("50001", "群聊不存在");
        }

        Date now = new Date();
        for (String newId : newMemberIds) {
            LambdaQueryWrapper<ConversationMember> check = new LambdaQueryWrapper<>();
            check.eq(ConversationMember::getConversationId, conversationId)
                    .eq(ConversationMember::getUserId, newId);
            if (conversationMemberMapper.selectCount(check) == 0) {
                ConversationMember member = new ConversationMember();
                member.setId(UUID.randomUUID().toString().replace("-", ""));
                member.setConversationId(conversationId);
                member.setUserId(newId);
                member.setUnreadCount(0);
                member.setIsTop(0);
                member.setIsMute(0);
                member.setCreatedTime(now);
                conversationMemberMapper.insert(member);
            }
        }
    }

    @Override
    @Transactional
    public void removeMember(String userId, String conversationId, String targetUserId) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null || !ConversationTypeEnum.GROUP.getCode().equals(conversation.getType())) {
            throw new BizException("50001", "群聊不存在");
        }
        if (conversation.getOwnerId().equals(targetUserId)) {
            throw new BizException("50001", "群主不能被移除，请先转让群主");
        }
        if (!conversation.getOwnerId().equals(userId) && !userId.equals(targetUserId)) {
            throw new BizException("50001", "无权操作");
        }

        LambdaQueryWrapper<ConversationMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationMember::getConversationId, conversationId)
                .eq(ConversationMember::getUserId, targetUserId);
        conversationMemberMapper.delete(wrapper);
    }

    @Override
    @Transactional
    public void transferOwner(String userId, String conversationId, String newOwnerId) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null || !ConversationTypeEnum.GROUP.getCode().equals(conversation.getType())) {
            throw new BizException("50001", "群聊不存在");
        }
        if (!conversation.getOwnerId().equals(userId)) {
            throw new BizException("50001", "仅群主可转让");
        }
        if (userId.equals(newOwnerId)) {
            throw new BizException("50001", "不能转让给自己");
        }

        // 校验新群主是否群成员
        LambdaQueryWrapper<ConversationMember> check = new LambdaQueryWrapper<>();
        check.eq(ConversationMember::getConversationId, conversationId)
                .eq(ConversationMember::getUserId, newOwnerId);
        if (conversationMemberMapper.selectCount(check) == 0) {
            throw new BizException("50001", "新群主不是群成员");
        }

        conversation.setOwnerId(newOwnerId);
        conversation.setUpdateTime(new Date());
        conversationMapper.updateById(conversation);
    }

    @Override
    @Transactional
    public void updateName(String userId, String conversationId, String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new BizException("50001", "群名称不能为空");
        }
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null || !ConversationTypeEnum.GROUP.getCode().equals(conversation.getType())) {
            throw new BizException("50001", "群聊不存在");
        }
        if (!conversation.getOwnerId().equals(userId)) {
            throw new BizException("50001", "仅群主可修改群名称");
        }
        conversation.setName(name);
        conversation.setUpdateTime(new Date());
        conversationMapper.updateById(conversation);
    }

    @Override
    public String getOwnerId(String conversationId) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        return conversation != null ? conversation.getOwnerId() : null;
    }

    @Override
    public List<String> getMemberIds(String conversationId) {
        LambdaQueryWrapper<ConversationMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationMember::getConversationId, conversationId);
        return conversationMemberMapper.selectList(wrapper).stream()
                .map(ConversationMember::getUserId).collect(Collectors.toList());
    }
}
