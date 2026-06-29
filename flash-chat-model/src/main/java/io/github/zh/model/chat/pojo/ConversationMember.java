package io.github.zh.model.chat.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("conversation_members")
public class ConversationMember {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String conversationId;

    private String userId;

    private Integer unreadCount;

    private String lastReadMessageId;

    private Integer isTop;

    private Integer isMute;

    private Date createdTime;
}