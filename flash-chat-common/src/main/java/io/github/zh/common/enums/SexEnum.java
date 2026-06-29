package io.github.zh.common.enums;

import lombok.Getter;

@Getter
public enum SexEnum {

    WOMAN(0, "女"),
    MAN(1, "男"),
    SECRET(2, "保密");
    private final Integer code;
    private final String message;
    SexEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
    public static String getMessageByCode(Integer code) {
        for (SexEnum value : SexEnum.values()) {
            if (value.getCode().equals(code)) {
                return value.getMessage();
            }
        }
        return null;
    }
}
