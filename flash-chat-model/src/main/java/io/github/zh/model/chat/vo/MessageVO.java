package io.github.zh.model.chat.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageVO {
    private String id;
    private String conversationId;
    private String senderId;
    private String senderName;
    private String senderFace;
    private String content;
    private Integer type;
    private Date createTime;
    private boolean isMe;
}
