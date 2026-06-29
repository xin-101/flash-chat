CREATE DATABASE IF NOT EXISTS flash_chat CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE flash_chat;

CREATE TABLE IF NOT EXISTS users (
    id              VARCHAR(32)  NOT NULL COMMENT '用户ID',
    flash_chat_num  VARCHAR(32)  DEFAULT NULL COMMENT '闪聊号',
    flash_chat_num_img VARCHAR(255) DEFAULT NULL COMMENT '闪聊二维码',
    mobile          VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    nickname        VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    real_name       VARCHAR(50)  DEFAULT NULL COMMENT '真实姓名',
    sex             INT          DEFAULT 2  COMMENT '性别：0女，1男，2保密',
    face            VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    email           VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    birthday        DATE         DEFAULT NULL COMMENT '生日',
    country         VARCHAR(50)  DEFAULT NULL COMMENT '国家',
    province        VARCHAR(50)  DEFAULT NULL COMMENT '省份',
    city            VARCHAR(50)  DEFAULT NULL COMMENT '城市',
    district        VARCHAR(50)  DEFAULT NULL COMMENT '区县',
    chat_bg         VARCHAR(255) DEFAULT NULL COMMENT '聊天背景',
    friend_circle_bg VARCHAR(255) DEFAULT NULL COMMENT '朋友圈背景',
    signature       VARCHAR(255) DEFAULT NULL COMMENT '个性签名',
    created_time    DATETIME     DEFAULT NULL COMMENT '创建时间',
    updated_time    DATETIME     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_mobile (mobile)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE IF NOT EXISTS conversations (
    id                  VARCHAR(32)  NOT NULL COMMENT '会话ID',
    type                INT          DEFAULT 1  COMMENT '会话类型：1单聊，2群聊',
    name                VARCHAR(100) DEFAULT NULL COMMENT '会话名称（群聊用）',
    owner_id            VARCHAR(32)  DEFAULT NULL COMMENT '群主ID（群聊用）',
    last_message_id     VARCHAR(32)  DEFAULT NULL COMMENT '最后一条消息ID',
    last_message_content VARCHAR(500) DEFAULT NULL COMMENT '最后一条消息内容',
    last_message_time   DATETIME     DEFAULT NULL COMMENT '最后一条消息时间',
    created_time        DATETIME     DEFAULT NULL COMMENT '创建时间',
    updated_time        DATETIME     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话表';

CREATE TABLE IF NOT EXISTS conversation_members (
    id                  VARCHAR(32) NOT NULL COMMENT '主键ID',
    conversation_id     VARCHAR(32) NOT NULL COMMENT '会话ID',
    user_id             VARCHAR(32) NOT NULL COMMENT '用户ID',
    unread_count        INT         DEFAULT 0  COMMENT '未读消息数',
    last_read_message_id VARCHAR(32) DEFAULT NULL COMMENT '最后已读消息ID',
    is_top              INT         DEFAULT 0  COMMENT '是否置顶：0否，1是',
    is_mute             INT         DEFAULT 0  COMMENT '是否免打扰：0否，1是',
    created_time        DATETIME    DEFAULT NULL COMMENT '加入时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_conversation_user (conversation_id, user_id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话成员表';

CREATE TABLE IF NOT EXISTS messages (
    id              VARCHAR(32) NOT NULL COMMENT '消息ID',
    conversation_id VARCHAR(32) NOT NULL COMMENT '会话ID',
    sender_id       VARCHAR(32) NOT NULL COMMENT '发送者ID',
    content         TEXT        COMMENT '消息内容',
    type            INT         DEFAULT 1  COMMENT '消息类型：1文本，2图片，3语音，4视频，5文件',
    status          INT         DEFAULT 1  COMMENT '消息状态：0撤回，1正常',
    created_time    DATETIME    DEFAULT NULL COMMENT '发送时间',
    PRIMARY KEY (id),
    KEY idx_conversation_id (conversation_id),
    KEY idx_sender_id (sender_id),
    KEY idx_created_time (created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';

CREATE TABLE IF NOT EXISTS friend_request (
    id              VARCHAR(32)  NOT NULL COMMENT '主键ID',
    from_user_id    VARCHAR(32)  NOT NULL COMMENT '申请人ID',
    to_user_id      VARCHAR(32)  NOT NULL COMMENT '被申请人ID',
    status          INT          DEFAULT 0  COMMENT '状态：0待处理，1已同意，2已拒绝',
    remark          VARCHAR(255) DEFAULT NULL COMMENT '申请备注',
    create_time     DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_time     DATETIME     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_from_user (from_user_id),
    KEY idx_to_user (to_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友申请表';

CREATE TABLE IF NOT EXISTS user_friend (
    id          VARCHAR(32)  NOT NULL COMMENT '主键ID',
    user_id     VARCHAR(32)  NOT NULL COMMENT '用户ID',
    friend_id   VARCHAR(32)  NOT NULL COMMENT '好友ID',
    remark      VARCHAR(100) DEFAULT NULL COMMENT '好友备注',
    is_block    INT          DEFAULT 0  COMMENT '是否拉黑：0否，1是',
    create_time DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_friend (user_id, friend_id),
    KEY idx_user_id (user_id),
    KEY idx_friend_id (friend_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友关系表';

CREATE TABLE IF NOT EXISTS moment (
    id          VARCHAR(32)  NOT NULL COMMENT '朋友圈ID',
    user_id     VARCHAR(32)  NOT NULL COMMENT '用户ID',
    content     TEXT         COMMENT '文字内容',
    images      TEXT         COMMENT '图片URL列表（逗号分隔）',
    status      INT          DEFAULT 1  COMMENT '状态：0删除，1正常',
    create_time DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='朋友圈表';

CREATE TABLE IF NOT EXISTS moment_like (
    id          VARCHAR(32) NOT NULL COMMENT '主键ID',
    moment_id   VARCHAR(32) NOT NULL COMMENT '朋友圈ID',
    user_id     VARCHAR(32) NOT NULL COMMENT '用户ID',
    create_time DATETIME    DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_moment_user (moment_id, user_id),
    KEY idx_moment_id (moment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='朋友圈点赞表';

CREATE TABLE IF NOT EXISTS moment_comment (
    id            VARCHAR(32)  NOT NULL COMMENT '主键ID',
    moment_id     VARCHAR(32)  NOT NULL COMMENT '朋友圈ID',
    user_id       VARCHAR(32)  NOT NULL COMMENT '评论用户ID',
    reply_user_id VARCHAR(32)  DEFAULT NULL COMMENT '回复目标用户ID',
    content       VARCHAR(500) NOT NULL COMMENT '评论内容',
    create_time   DATETIME     DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_moment_id (moment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='朋友圈评论表';
