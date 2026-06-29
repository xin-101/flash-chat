package io.github.zh.common.enums;

import lombok.Getter;

@Getter
public enum StatusEnum {
    DELETED(0, "删除/撤回"),
    NORMAL(1, "正常");

    private final Integer code;
    private final String message;

    StatusEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public static String getMessageByCode(Integer code) {
        if (code == null) return null;
        for (StatusEnum value : values()) {
            if (value.getCode().equals(code)) return value.getMessage();
        }
        return null;
    }
}
