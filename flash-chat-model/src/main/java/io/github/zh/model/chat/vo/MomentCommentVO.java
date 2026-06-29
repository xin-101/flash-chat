package io.github.zh.model.chat.vo;

import lombok.Data;

import java.util.Date;

@Data
public class MomentCommentVO {
    private String id;
    private String userId;
    private String nickname;
    private String face;
    private String replyUserId;
    private String replyNickname;
    private String content;
    private Date createTime;
}
