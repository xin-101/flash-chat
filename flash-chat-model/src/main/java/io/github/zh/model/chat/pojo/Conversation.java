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
@TableName("conversations")
public class Conversation {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private Integer type;
    private String name;
    private String ownerId;
    private String lastMessageId;
    private String lastMessageContent;
    private Date lastMessageTime;
    private Date createTime;
    private Date updateTime;
}
