package io.github.zh.chat.service;

import io.github.zh.model.chat.bo.SendMessageBO;
import io.github.zh.model.chat.vo.MessageVO;

import java.util.List;

public interface MessageService {

    List<MessageVO> getMessageList(String conversationId, String userId, int page, int size);

    MessageVO sendMessage(String userId, SendMessageBO bo);
}