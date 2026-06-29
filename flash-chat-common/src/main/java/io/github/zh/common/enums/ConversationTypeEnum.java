package io.github.zh.common.enums;

import lombok.Getter;

@Getter
public enum ConversationTypeEnum {
    SINGLE(1, "单聊"),
    GROUP(2, "群聊");

    private final Integer code;
    private final String message;

    ConversationTypeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public static String getMessageByCode(Integer code) {
        if (code == null) return null;
        for (ConversationTypeEnum value : values()) {
            if (value.getCode().equals(code)) return value.getMessage();
        }
        return null;
    }
}
