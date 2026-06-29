package io.github.zh.common.enums;

import lombok.Getter;

@Getter
public enum MessageTypeEnum {
    TEXT(1, "文本"),
    IMAGE(2, "图片"),
    VOICE(3, "语音"),
    VIDEO(4, "视频"),
    FILE(5, "文件");

    private final Integer code;
    private final String message;

    MessageTypeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public static String getMessageByCode(Integer code) {
        if (code == null) return null;
        for (MessageTypeEnum value : values()) {
            if (value.getCode().equals(code)) return value.getMessage();
        }
        return null;
    }
}
