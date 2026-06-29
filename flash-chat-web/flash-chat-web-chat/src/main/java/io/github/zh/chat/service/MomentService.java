package io.github.zh.chat.service;

import io.github.zh.model.chat.vo.MomentVO;

import java.util.List;

public interface MomentService {

    MomentVO create(String userId, String content, List<String> images);

    void delete(String userId, String momentId);

    List<MomentVO> list(String userId, int page, int size);

    void like(String userId, String momentId);

    void unlike(String userId, String momentId);

    void comment(String userId, String momentId, String content, String replyUserId);

    void deleteComment(String userId, String commentId);
}
