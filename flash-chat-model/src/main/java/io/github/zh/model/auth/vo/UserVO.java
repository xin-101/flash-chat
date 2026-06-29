package io.github.zh.model.auth.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import io.github.zh.common.constants.DateConstants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 用户表
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;

    /**
    * 闪聊号
    */
    private String flashChatNum;

    /**
    * 闪聊二维码
    */
    private String flashChatNumImg;

    /**
    * 手机号
    */
    private String mobile;

    /**
    * 昵称
    */
    private String nickname;

    /**
    * 真实姓名
    */
    private String realName;

    /**
    * 性别：0: 女，1: 男，2: 保密
    */
    private Integer sex;

    /**
    * 用户头像
    */
    private String face;

    /**
    * 邮箱
    */
    private String email;

    /**
    * 生日
    */
    @JsonFormat(pattern = DateConstants.DATE_FORMAT_Y_M_D_STR, timezone = DateConstants.GMT_8)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate birthday;

    /**
    * 国家
    */
    private String country;

    /**
    * 省份
    */
    private String province;

    /**
    * 城市
    */
    private String city;

    /**
    * 区县
    */
    private String district;

    /**
    * 聊天背景
    */
    private String chatBg;

    /**
    * 朋友圈背景图
    */
    private String friendCircleBg;

    /**
    * 签名
    */
    private String signature;

    /**
    * 创建时间
    */
    @JsonFormat(pattern = DateConstants.DATE_FORMAT_Y_M_D_H_M_S_STR, timezone = DateConstants.GMT_8)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime createdTime;

    /**
    * 更新时间
    */
    @JsonFormat(pattern = DateConstants.DATE_FORMAT_Y_M_D_H_M_S_STR, timezone = DateConstants.GMT_8)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime updatedTime;

    /**
    * Token
    */
    private String token;
}