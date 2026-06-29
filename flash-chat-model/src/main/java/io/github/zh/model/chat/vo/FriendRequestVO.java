package io.github.zh.model.chat.vo;

import lombok.Data;

import java.util.Date;

@Data
public class FriendRequestVO {
    private String id;
    private String fromUserId;
    private String fromNickname;
    private String fromFace;
    private String remark;
    private Integer status;
    private Date createTime;
}
