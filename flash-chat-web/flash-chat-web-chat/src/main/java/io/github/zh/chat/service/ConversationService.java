package io.github.zh.chat.service;

import io.github.zh.model.chat.vo.ConversationVO;

import java.util.List;

public interface ConversationService {
    List<ConversationVO> getConversationList(String userId);
    ConversationVO createConversation(String userId,String targetUserId);
    void markAsRead(String userId, String conversationId);
}
