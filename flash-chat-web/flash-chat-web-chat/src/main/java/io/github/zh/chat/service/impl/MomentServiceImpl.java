package io.github.zh.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.zh.chat.mapper.ChatUsersMapper;
import io.github.zh.chat.mapper.MomentCommentMapper;
import io.github.zh.chat.mapper.MomentLikeMapper;
import io.github.zh.chat.mapper.MomentMapper;
import io.github.zh.chat.service.MomentService;
import io.github.zh.common.enums.StatusEnum;
import io.github.zh.common.exception.BizException;
import io.github.zh.model.auth.pojo.Users;
import io.github.zh.model.chat.pojo.Moment;
import io.github.zh.model.chat.pojo.MomentComment;
import io.github.zh.model.chat.pojo.MomentLike;
import io.github.zh.model.chat.vo.MomentCommentVO;
import io.github.zh.model.chat.vo.MomentVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MomentServiceImpl implements MomentService {

    private final MomentMapper momentMapper;
    private final MomentLikeMapper momentLikeMapper;
    private final MomentCommentMapper momentCommentMapper;
    private final ChatUsersMapper usersMapper;

    public MomentServiceImpl(MomentMapper momentMapper, MomentLikeMapper momentLikeMapper,
                             MomentCommentMapper momentCommentMapper, ChatUsersMapper usersMapper) {
        this.momentMapper = momentMapper;
        this.momentLikeMapper = momentLikeMapper;
        this.momentCommentMapper = momentCommentMapper;
        this.usersMapper = usersMapper;
    }

    @Override
    @Transactional
    public MomentVO create(String userId, String content, List<String> images) {
        if (content == null || content.trim().isEmpty()) {
            throw new BizException("50001", "内容不能为空");
        }

        Moment moment = new Moment();
        moment.setId(UUID.randomUUID().toString().replace("-", ""));
        moment.setUserId(userId);
        moment.setContent(content.trim());
        moment.setImages(images != null && !images.isEmpty() ? String.join(",", images) : null);
        moment.setStatus(StatusEnum.NORMAL.getCode());
        moment.setCreateTime(new Date());
        moment.setUpdateTime(new Date());
        momentMapper.insert(moment);

        return buildMomentVO(moment, userId);
    }

    @Override
    @Transactional
    public void delete(String userId, String momentId) {
        Moment moment = momentMapper.selectById(momentId);
        if (moment == null || !moment.getUserId().equals(userId)) {
            throw new BizException("50001", "朋友圈不存在");
        }
        moment.setStatus(StatusEnum.DELETED.getCode());
        moment.setUpdateTime(new Date());
        momentMapper.updateById(moment);
    }

    @Override
    public List<MomentVO> list(String userId, int page, int size) {
        Page<Moment> momentPage = new Page<>(page, size);
        LambdaQueryWrapper<Moment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Moment::getStatus, StatusEnum.NORMAL.getCode()).orderByDesc(Moment::getCreateTime);
        List<Moment> moments = momentMapper.selectPage(momentPage, wrapper).getRecords();

        return moments.stream().map(m -> buildMomentVO(m, userId)).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void like(String userId, String momentId) {
        LambdaQueryWrapper<MomentLike> check = new LambdaQueryWrapper<>();
        check.eq(MomentLike::getMomentId, momentId).eq(MomentLike::getUserId, userId);
        if (momentLikeMapper.selectCount(check) > 0) return;

        MomentLike like = new MomentLike();
        like.setId(UUID.randomUUID().toString().replace("-", ""));
        like.setMomentId(momentId);
        like.setUserId(userId);
        like.setCreateTime(new Date());
        momentLikeMapper.insert(like);
    }

    @Override
    @Transactional
    public void unlike(String userId, String momentId) {
        LambdaQueryWrapper<MomentLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MomentLike::getMomentId, momentId).eq(MomentLike::getUserId, userId);
        momentLikeMapper.delete(wrapper);
    }

    @Override
    @Transactional
    public void comment(String userId, String momentId, String content, String replyUserId) {
        if (content == null || content.trim().isEmpty()) {
            throw new BizException("50001", "评论内容不能为空");
        }

        MomentComment comment = new MomentComment();
        comment.setId(UUID.randomUUID().toString().replace("-", ""));
        comment.setMomentId(momentId);
        comment.setUserId(userId);
        comment.setReplyUserId(replyUserId);
        comment.setContent(content.trim());
        comment.setCreateTime(new Date());
        momentCommentMapper.insert(comment);
    }

    @Override
    @Transactional
    public void deleteComment(String userId, String commentId) {
        MomentComment comment = momentCommentMapper.selectById(commentId);
        if (comment == null || !comment.getUserId().equals(userId)) {
            throw new BizException("50001", "评论不存在");
        }
        momentCommentMapper.deleteById(commentId);
    }

    private MomentVO buildMomentVO(Moment moment, String currentUserId) {
        MomentVO vo = new MomentVO();
        BeanUtils.copyProperties(moment, vo);
        vo.setImageList(moment.getImages() != null ? Arrays.asList(moment.getImages().split(",")) : Collections.emptyList());

        Users user = usersMapper.selectById(moment.getUserId());
        if (user != null) {
            vo.setNickname(user.getNickname());
            vo.setFace(user.getFace());
        }

        LambdaQueryWrapper<MomentLike> likeCountWrapper = new LambdaQueryWrapper<>();
        likeCountWrapper.eq(MomentLike::getMomentId, moment.getId());
        vo.setLikeCount(Math.toIntExact(momentLikeMapper.selectCount(likeCountWrapper)));

        LambdaQueryWrapper<MomentLike> likedWrapper = new LambdaQueryWrapper<>();
        likedWrapper.eq(MomentLike::getMomentId, moment.getId()).eq(MomentLike::getUserId, currentUserId);
        vo.setLiked(momentLikeMapper.selectCount(likedWrapper) > 0);

        LambdaQueryWrapper<MomentComment> commentWrapper = new LambdaQueryWrapper<>();
        commentWrapper.eq(MomentComment::getMomentId, moment.getId()).orderByAsc(MomentComment::getCreateTime);
        List<MomentComment> comments = momentCommentMapper.selectList(commentWrapper);
        vo.setComments(comments.stream().map(c -> {
            MomentCommentVO cvo = new MomentCommentVO();
            BeanUtils.copyProperties(c, cvo);
            Users cu = usersMapper.selectById(c.getUserId());
            if (cu != null) {
                cvo.setNickname(cu.getNickname());
                cvo.setFace(cu.getFace());
            }
            if (c.getReplyUserId() != null) {
                Users ru = usersMapper.selectById(c.getReplyUserId());
                if (ru != null) cvo.setReplyNickname(ru.getNickname());
            }
            return cvo;
        }).collect(Collectors.toList()));

        return vo;
    }
}
