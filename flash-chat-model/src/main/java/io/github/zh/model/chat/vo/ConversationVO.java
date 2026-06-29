package io.github.zh.model.chat.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConversationVO {
    private String id;
    private String name;
    private String face;
    private String lastMessage;
    private Date lastMessageTime;
    private Integer unreadCount;
    private Integer type;
}
