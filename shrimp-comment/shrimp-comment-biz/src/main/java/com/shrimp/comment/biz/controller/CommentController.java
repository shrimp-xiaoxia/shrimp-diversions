package com.shrimp.comment.biz.controller;

import com.shrimp.comment.biz.model.vo.*;
import com.shrimp.comment.biz.service.CommentService;
import com.shrimp.comment.biz.service.CommentUserFreshener;
import com.shrimp.framework.biz.operationlog.aspect.ApiOperationLog;
import com.shrimp.framework.common.response.PageResponse;
import com.shrimp.framework.common.response.Response;
import com.shrimp.kv.dto.req.FindCommentContentReqDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.shrimp.comment.biz.domain.mapper.CommentLikeDOMapper;
import com.shrimp.comment.biz.domain.mapper.NoteCountDOMapper;
import com.shrimp.comment.biz.domain.mapper.CommentDOMapper;
import com.shrimp.comment.biz.domain.dataobject.CommentDO;
import com.shrimp.comment.biz.enums.ResponseCodeEnum;
import com.shrimp.framework.biz.context.holder.LoginUserContextHolder;
import com.shrimp.framework.common.exception.BizException;
import org.springframework.beans.BeanUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * @author ccb
 */
@RestController
@RequestMapping("/comment")
@Slf4j
public class CommentController {
    @Resource
    private CommentService commentService;
    @Resource private CommentLikeDOMapper commentLikes;
    @Resource private NoteCountDOMapper noteCounts;
    @Resource private CommentDOMapper comments;
    @Resource private CommentUserFreshener commentUserFreshener;

    @PostMapping("/publish")
    @ApiOperationLog(description = "发布评论接口")
    public Response<?> publishComment(@Validated @RequestBody PublishCommentReqVO publishCommentReqVO) {
        return commentService.publishComment(publishCommentReqVO);
    }

    @PostMapping("/list")
    @ApiOperationLog(description = "评论分页查询")
    public PageResponse<FindCommentItemRspVO> findCommentPageList(@Validated @RequestBody FindCommentPageListReqVO findCommentPageListReqVO) {
        checkVisible(findCommentPageListReqVO.getNoteId());
        PageResponse<FindCommentItemRspVO> response = commentService.findCommentPageList(findCommentPageListReqVO);
        long topLevelCount = comments.countTopLevelByNoteId(findCommentPageListReqVO.getNoteId());
        response.setTotalPage(PageResponse.getTotalPage(topLevelCount, 10));
        if (response.getData() == null) return response;
        List<FindCommentItemRspVO> copies = response.getData().stream().map(this::copy).toList();
        List<Long> ids = new ArrayList<>();
        for (FindCommentItemRspVO item : copies) {
            ids.add(item.getCommentId());
            if (item.getFirstReplyComment() != null) ids.add(item.getFirstReplyComment().getCommentId());
        }
        Set<Long> liked = liked(ids);
        for (FindCommentItemRspVO item : copies) {
            apply(item, liked);
            if (item.getFirstReplyComment() != null) apply(item.getFirstReplyComment(), liked);
        }
        commentUserFreshener.refreshTopLevel(copies);
        response.setData(copies);
        return response;
    }

    @PostMapping("/child/list")
    @ApiOperationLog(description = "二级评论分页查询")
    public PageResponse<FindChildCommentItemRspVO> findChildCommentPageList(@Validated @RequestBody FindChildCommentPageListReqVO findChildCommentPageListReqVO) {
        CommentDO parent = comments.selectByPrimaryKey(findChildCommentPageListReqVO.getParentCommentId());
        if (parent == null) throw new BizException(ResponseCodeEnum.PARENT_COMMENT_NOT_FOUND);
        checkVisible(parent.getNoteId());
        PageResponse<FindChildCommentItemRspVO> response = commentService.findChildCommentPageList(findChildCommentPageListReqVO);
        long skipped = Boolean.TRUE.equals(findChildCommentPageListReqVO.getIncludeFirst()) ? 0 : 1;
        response.setTotalPage(PageResponse.getTotalPage(Math.max(0, response.getTotalCount() - skipped), 6));
        if (response.getData() == null) return response;
        List<FindChildCommentItemRspVO> copies = response.getData().stream().map(item -> {
            FindChildCommentItemRspVO copy = new FindChildCommentItemRspVO(); BeanUtils.copyProperties(item, copy); return copy;
        }).toList();
        Set<Long> liked = liked(copies.stream().map(FindChildCommentItemRspVO::getCommentId).toList());
        copies.forEach(item -> { item.setIsLiked(liked == null ? null : liked.contains(item.getCommentId())); item.setLikeStateAvailable(liked != null); });
        commentUserFreshener.refreshChildren(copies);
        response.setData(copies);
        return response;
    }

    private void checkVisible(Long noteId) {
        if (noteCounts.countCommentableNote(noteId, LoginUserContextHolder.getLoginUserId()) == 0)
            throw new BizException(ResponseCodeEnum.NOTE_NOT_COMMENTABLE);
    }

    private Set<Long> liked(List<Long> ids) {
        Long userId = LoginUserContextHolder.getLoginUserId();
        if (userId == null || ids.isEmpty()) return Set.of();
        try { return new HashSet<>(commentLikes.selectLikedCommentIds(userId, ids)); }
        catch (RuntimeException e) { log.warn("评论点赞状态查询失败", e); return null; }
    }

    private FindCommentItemRspVO copy(FindCommentItemRspVO source) {
        FindCommentItemRspVO result = new FindCommentItemRspVO(); BeanUtils.copyProperties(source, result);
        if (source.getFirstReplyComment() != null) {
            FindCommentItemRspVO reply = new FindCommentItemRspVO(); BeanUtils.copyProperties(source.getFirstReplyComment(), reply);
            result.setFirstReplyComment(reply);
        }
        return result;
    }

    private void apply(FindCommentItemRspVO item, Set<Long> liked) {
        item.setIsLiked(liked == null ? null : liked.contains(item.getCommentId())); item.setLikeStateAvailable(liked != null);
    }

    @PostMapping("/like")
    @ApiOperationLog(description = "评论点赞")
    public Response<?> likeComment(@Validated @RequestBody LikeCommentReqVO likeCommentReqVO) {
        return commentService.likeComment(likeCommentReqVO);
    }

    @PostMapping("/unlike")
    @ApiOperationLog(description = "取消评论点赞")
    public Response<?> unlikeComment(@Validated @RequestBody UnlikeCommentReqVO unlikeCommentReqVO) {
        return commentService.unlikeComment(unlikeCommentReqVO);
    }

    @PostMapping("/delete")
    @ApiOperationLog(description = "删除评论")
    public Response<?> deleteComment(@Validated @RequestBody DeleteCommentReqVO deleteCommentReqVO) {
        return commentService.deleteComment(deleteCommentReqVO);
    }
}
