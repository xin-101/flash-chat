package io.github.zh.chat.service;

import io.github.zh.model.chat.vo.ConversationVO;

import java.util.List;

public interface GroupService {

    ConversationVO create(String userId, String name, List<String> memberIds);

    void invite(String userId, String conversationId, List<String> newMemberIds);

    void removeMember(String userId, String conversationId, String targetUserId);

    void transferOwner(String userId, String conversationId, String newOwnerId);

    void updateName(String userId, String conversationId, String name);

    List<String> getMemberIds(String conversationId);

    String getOwnerId(String conversationId);
}
