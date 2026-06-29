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
@TableName("user_friend")
public class UserFriend {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String userId;
    private String friendId;
    private String remark;
    private Integer isBlock;
    private Date createTime;
    private Date updateTime;
}
