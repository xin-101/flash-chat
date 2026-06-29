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
@TableName("moment_comment")
public class MomentComment {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String momentId;
    private String userId;
    private String replyUserId;
    private String content;
    private Date createTime;
}
