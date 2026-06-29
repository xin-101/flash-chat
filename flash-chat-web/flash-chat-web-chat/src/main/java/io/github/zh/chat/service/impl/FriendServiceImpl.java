package io.github.zh.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.zh.chat.config.RabbitMQConfig;
import io.github.zh.chat.mapper.ChatUsersMapper;
import io.github.zh.chat.mapper.FriendRequestMapper;
import io.github.zh.chat.mapper.UserFriendMapper;
import io.github.zh.chat.service.FriendService;
import io.github.zh.common.enums.FriendRequestStatusEnum;
import io.github.zh.common.exception.BizException;
import io.github.zh.common.response.PageResult;
import io.github.zh.model.auth.pojo.Users;
import io.github.zh.model.chat.pojo.FriendRequest;
import io.github.zh.model.chat.pojo.UserFriend;
import io.github.zh.model.chat.vo.FriendRequestVO;
import io.github.zh.model.chat.vo.UserFriendVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FriendServiceImpl implements FriendService {

    private final FriendRequestMapper friendRequestMapper;
    private final UserFriendMapper userFriendMapper;
    private final ChatUsersMapper usersMapper;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public FriendServiceImpl(FriendRequestMapper friendRequestMapper,
                             UserFriendMapper userFriendMapper,
                             ChatUsersMapper usersMapper,
                             RabbitTemplate rabbitTemplate,
                             ObjectMapper objectMapper) {
        this.friendRequestMapper = friendRequestMapper;
        this.userFriendMapper = userFriendMapper;
        this.usersMapper = usersMapper;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void sendRequest(String userId, String targetUserId, String remark) {
        if (userId.equals(targetUserId)) {
            throw new BizException("50001", "不能添加自己为好友");
        }

        // 检查是否已是好友
        LambdaQueryWrapper<UserFriend> friendCheck = new LambdaQueryWrapper<>();
        friendCheck.eq(UserFriend::getUserId, userId).eq(UserFriend::getFriendId, targetUserId);
        if (userFriendMapper.selectCount(friendCheck) > 0) {
            throw new BizException("50001", "对方已是您的好友");
        }

        // 检查是否已有待处理的申请
        LambdaQueryWrapper<FriendRequest> requestCheck = new LambdaQueryWrapper<>();
        requestCheck.eq(FriendRequest::getFromUserId, userId)
                .eq(FriendRequest::getToUserId, targetUserId)
                .eq(FriendRequest::getStatus, FriendRequestStatusEnum.PENDING.getCode());
        if (friendRequestMapper.selectCount(requestCheck) > 0) {
            throw new BizException("50001", "已有待处理的申请");
        }

        FriendRequest request = new FriendRequest();
        request.setId(UUID.randomUUID().toString().replace("-", ""));
        request.setFromUserId(userId);
        request.setToUserId(targetUserId);
        request.setStatus(FriendRequestStatusEnum.PENDING.getCode());
        request.setRemark(remark);
        request.setCreateTime(new Date());
        request.setUpdateTime(new Date());
        friendRequestMapper.insert(request);

        // 通过 WebSocket 实时推送好友申请通知
        try {
            Users fromUser = usersMapper.selectById(userId);
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "friend_request");
            notification.put("requestId", request.getId());
            notification.put("fromUserId", userId);
            notification.put("toUserId", targetUserId);
            notification.put("fromNickname", fromUser != null ? fromUser.getNickname() : "");
            notification.put("fromFace", fromUser != null ? fromUser.getFace() : "");
            notification.put("remark", remark);
            String json = objectMapper.writeValueAsString(notification);
            rabbitTemplate.convertAndSend(RabbitMQConfig.CHAT_EXCHANGE, "", json);
        } catch (Exception e) {
            log.error("[Friend] 发布好友申请通知到RabbitMQ失败", e);
        }

        log.info("好友申请已发送: from={}, to={}", userId, targetUserId);
    }

    @Override
    @Transactional
    public void approveRequest(String userId, String requestId) {
        FriendRequest request = friendRequestMapper.selectById(requestId);
        if (request == null || !request.getToUserId().equals(userId)) {
            throw new BizException("50001", "申请不存在");
        }
        if (!FriendRequestStatusEnum.PENDING.getCode().equals(request.getStatus())) {
            throw new BizException("50001", "申请已处理");
        }

        request.setStatus(FriendRequestStatusEnum.APPROVED.getCode());
        request.setUpdateTime(new Date());
        friendRequestMapper.updateById(request);

        Date nowDate = new Date();

        UserFriend f1 = new UserFriend();
        f1.setId(UUID.randomUUID().toString().replace("-", ""));
        f1.setUserId(request.getFromUserId());
        f1.setFriendId(request.getToUserId());
        f1.setIsBlock(0);
        f1.setCreateTime(nowDate);
        f1.setUpdateTime(nowDate);
        userFriendMapper.insert(f1);

        UserFriend f2 = new UserFriend();
        f2.setId(UUID.randomUUID().toString().replace("-", ""));
        f2.setUserId(request.getToUserId());
        f2.setFriendId(request.getFromUserId());
        f2.setIsBlock(0);
        f2.setCreateTime(nowDate);
        f2.setUpdateTime(nowDate);
        userFriendMapper.insert(f2);

        log.info("好友申请已通过: from={}, to={}", request.getFromUserId(), request.getToUserId());
    }

    @Override
    @Transactional
    public void rejectRequest(String userId, String requestId) {
        FriendRequest request = friendRequestMapper.selectById(requestId);
        if (request == null || !request.getToUserId().equals(userId)) {
            throw new BizException("50001", "申请不存在");
        }
        if (!FriendRequestStatusEnum.PENDING.getCode().equals(request.getStatus())) {
            throw new BizException("50001", "申请已处理");
        }

        request.setStatus(FriendRequestStatusEnum.REJECTED.getCode());
        request.setUpdateTime(new Date());
        friendRequestMapper.updateById(request);
        log.info("好友申请已拒绝: from={}, to={}", request.getFromUserId(), request.getToUserId());
    }

    @Override
    public List<FriendRequestVO> getIncomingRequests(String userId) {
        LambdaQueryWrapper<FriendRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FriendRequest::getToUserId, userId)
                .eq(FriendRequest::getStatus, 0)
                .orderByDesc(FriendRequest::getCreateTime);
        List<FriendRequest> requests = friendRequestMapper.selectList(wrapper);

        return requests.stream().map(r -> {
            FriendRequestVO vo = new FriendRequestVO();
            BeanUtils.copyProperties(r, vo);
            Users fromUser = usersMapper.selectById(r.getFromUserId());
            if (fromUser != null) {
                vo.setFromNickname(fromUser.getNickname());
                vo.setFromFace(fromUser.getFace());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public PageResult<FriendRequestVO> getIncomingRequests(String userId, int page, int size) {
        Page<FriendRequest> requestPage = new Page<>(page, size);
        LambdaQueryWrapper<FriendRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FriendRequest::getToUserId, userId)
                .eq(FriendRequest::getStatus, 0)
                .orderByDesc(FriendRequest::getCreateTime);
        Page<FriendRequest> result = friendRequestMapper.selectPage(requestPage, wrapper);

        List<FriendRequestVO> voList = result.getRecords().stream().map(r -> {
            FriendRequestVO vo = new FriendRequestVO();
            BeanUtils.copyProperties(r, vo);
            Users fromUser = usersMapper.selectById(r.getFromUserId());
            if (fromUser != null) {
                vo.setFromNickname(fromUser.getNickname());
                vo.setFromFace(fromUser.getFace());
            }
            return vo;
        }).collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), page, size);
    }

    @Override
    public List<UserFriendVO> getFriendList(String userId) {
        LambdaQueryWrapper<UserFriend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFriend::getUserId, userId).orderByDesc(UserFriend::getCreateTime);
        List<UserFriend> friends = userFriendMapper.selectList(wrapper);

        return friends.stream().map(f -> {
            UserFriendVO vo = new UserFriendVO();
            BeanUtils.copyProperties(f, vo);
            vo.setFriendId(f.getFriendId());
            Users friendUser = usersMapper.selectById(f.getFriendId());
            if (friendUser != null) {
                vo.setNickname(friendUser.getNickname());
                vo.setFace(friendUser.getFace());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteFriend(String userId, String friendId) {
        LambdaQueryWrapper<UserFriend> wrapper1 = new LambdaQueryWrapper<>();
        wrapper1.eq(UserFriend::getUserId, userId).eq(UserFriend::getFriendId, friendId);
        userFriendMapper.delete(wrapper1);

        LambdaQueryWrapper<UserFriend> wrapper2 = new LambdaQueryWrapper<>();
        wrapper2.eq(UserFriend::getUserId, friendId).eq(UserFriend::getFriendId, userId);
        userFriendMapper.delete(wrapper2);
        log.info("好友已删除: user={}, friend={}", userId, friendId);
    }

    @Override
    @Transactional
    public void setRemark(String userId, String friendId, String remark) {
        LambdaQueryWrapper<UserFriend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFriend::getUserId, userId).eq(UserFriend::getFriendId, friendId);
        UserFriend friend = userFriendMapper.selectOne(wrapper);
        if (friend == null) {
            throw new BizException("50001", "好友不存在");
        }
        friend.setRemark(remark);
        friend.setUpdateTime(new Date());
        userFriendMapper.updateById(friend);
    }

    @Override
    @Transactional
    public void blockFriend(String userId, String friendId) {
        LambdaQueryWrapper<UserFriend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFriend::getUserId, userId).eq(UserFriend::getFriendId, friendId);
        UserFriend friend = userFriendMapper.selectOne(wrapper);
        if (friend == null) {
            throw new BizException("50001", "好友不存在");
        }
        friend.setIsBlock(1);
        friend.setUpdateTime(new Date());
        userFriendMapper.updateById(friend);
    }

    @Override
    @Transactional
    public void unblockFriend(String userId, String friendId) {
        LambdaQueryWrapper<UserFriend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFriend::getUserId, userId).eq(UserFriend::getFriendId, friendId);
        UserFriend friend = userFriendMapper.selectOne(wrapper);
        if (friend == null) {
            throw new BizException("50001", "好友不存在");
        }
        friend.setIsBlock(0);
        friend.setUpdateTime(new Date());
        userFriendMapper.updateById(friend);
    }
}
