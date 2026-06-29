package io.github.zh.chat.exception;

import io.github.zh.common.exception.BaseExceptionInterface;

public enum ResponseEnum implements BaseExceptionInterface {
    CHAT_CONVERSATION_NOT_FOUND("chat-50001", "会话不存在"),
    CHAT_NO_PERMISSION("chat-50002", "无权访问该会话"),
    CHAT_MESSAGE_SEND_FAILED("chat-50003", "消息发送失败");

    ResponseEnum(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    private final String errorCode;
    private final String errorMessage;

    @Override
    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }
}
