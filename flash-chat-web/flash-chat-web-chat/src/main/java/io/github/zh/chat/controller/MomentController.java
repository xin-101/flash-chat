package io.github.zh.chat.controller;

import io.github.zh.chat.service.MomentService;
import io.github.zh.common.response.Response;
import io.github.zh.model.chat.vo.MomentVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping
public class MomentController {

    private final MomentService momentService;

    public MomentController(MomentService momentService) {
        this.momentService = momentService;
    }

    @PostMapping("/moment/create")
    public Response<MomentVO> create(@RequestHeader("userId") String userId,
                                     @RequestBody Map<String, Object> body) {
        String content = (String) body.get("content");
        Object rawImages = body.get("images");
        List<String> images = null;
        if (rawImages instanceof List) {
            images = ((List<?>) rawImages).stream().map(Object::toString).collect(java.util.stream.Collectors.toList());
        }
        return Response.success(momentService.create(userId, content, images));
    }

    @DeleteMapping("/moment/{momentId}")
    public Response<Void> delete(@RequestHeader("userId") String userId,
                                 @PathVariable("momentId") String momentId) {
        momentService.delete(userId, momentId);
        return Response.success();
    }

    @GetMapping("/moment/list")
    public Response<List<MomentVO>> list(@RequestHeader("userId") String userId,
                                         @RequestParam(value = "page", defaultValue = "1") int page,
                                         @RequestParam(value = "size", defaultValue = "10") int size) {
        return Response.success(momentService.list(userId, page, size));
    }

    @PostMapping("/moment/{momentId}/like")
    public Response<Void> like(@RequestHeader("userId") String userId,
                               @PathVariable("momentId") String momentId) {
        momentService.like(userId, momentId);
        return Response.success();
    }

    @DeleteMapping("/moment/{momentId}/like")
    public Response<Void> unlike(@RequestHeader("userId") String userId,
                                 @PathVariable("momentId") String momentId) {
        momentService.unlike(userId, momentId);
        return Response.success();
    }

    @PostMapping("/moment/{momentId}/comment")
    public Response<Void> comment(@RequestHeader("userId") String userId,
                                   @PathVariable("momentId") String momentId,
                                   @RequestBody Map<String, Object> body) {
        String content = (String) body.get("content");
        String replyUserId = (String) body.get("replyUserId");
        momentService.comment(userId, momentId, content, replyUserId);
        return Response.success();
    }

    @DeleteMapping("/moment/comment/{commentId}")
    public Response<Void> deleteComment(@RequestHeader("userId") String userId,
                                        @PathVariable("commentId") String commentId) {
        momentService.deleteComment(userId, commentId);
        return Response.success();
    }
}
