package io.github.zh.model.chat.vo;

import lombok.Data;

import java.util.Date;

@Data
public class UserFriendVO {
    private String id;
    private String friendId;
    private String nickname;
    private String face;
    private String remark;
    private Integer isBlock;
    private Date createTime;
}
