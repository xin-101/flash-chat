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
@TableName("friend_request")
public class FriendRequest {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String fromUserId;
    private String toUserId;
    private Integer status;
    private String remark;
    private Date createTime;
    private Date updateTime;
}
