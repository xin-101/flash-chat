package io.github.zh.model.chat.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class MomentVO {
    private String id;
    private String userId;
    private String nickname;
    private String face;
    private String content;
    private List<String> imageList;
    private Integer status;
    private Date createTime;
    private int likeCount;
    private boolean liked;
    private List<MomentCommentVO> comments;
}
