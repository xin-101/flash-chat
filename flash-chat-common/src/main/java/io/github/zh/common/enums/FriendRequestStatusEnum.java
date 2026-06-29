package io.github.zh.common.enums;

import lombok.Getter;

@Getter
public enum FriendRequestStatusEnum {
    PENDING(0, "待处理"),
    APPROVED(1, "已同意"),
    REJECTED(2, "已拒绝");

    private final Integer code;
    private final String message;

    FriendRequestStatusEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public static String getMessageByCode(Integer code) {
        if (code == null) return null;
        for (FriendRequestStatusEnum value : values()) {
            if (value.getCode().equals(code)) return value.getMessage();
        }
        return null;
    }
}
