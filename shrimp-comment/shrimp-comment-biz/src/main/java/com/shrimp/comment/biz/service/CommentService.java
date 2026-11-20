package com.shrimp.comment.biz.service;

import com.shrimp.comment.biz.model.vo.*;
import com.shrimp.framework.common.response.PageResponse;
import com.shrimp.framework.common.response.Response;

/**
 * @author ccb
 */
public interface CommentService {

    Response<?> publishComment(PublishCommentReqVO publishCommentReqVO);

    /**
     * 评论列表分页查询
     *
     * @param findCommentPageListReqVO
     * @return
     */
    PageResponse<FindCommentItemRspVO> findCommentPageList(FindCommentPageListReqVO findCommentPageListReqVO);


    /**
     * 二级评论分页查询
     *
     * @param findChildCommentPageListReqVO
     * @return
     */
    PageResponse<FindChildCommentItemRspVO> findChildCommentPageList(FindChildCommentPageListReqVO findChildCommentPageListReqVO);

    /**
     * 评论点赞
     *
     * @param likeCommentReqVO
     * @return
     */
    Response<?> likeComment(LikeCommentReqVO likeCommentReqVO);

    /**
     * 取消评论点赞
     *
     * @param unlikeCommentReqVO
     * @return
     */
    Response<?> unlikeComment(UnlikeCommentReqVO unlikeCommentReqVO);

    /**
     * 删除评论
     *
     * @param deleteCommentReqVO
     * @return
     */
    Response<?> deleteComment(DeleteCommentReqVO deleteCommentReqVO);

    /**
     * 对外提供删除评论本地缓存的接口
     *
     * @param commentId
     */
    void removeCommentLocalCache(Long commentId);
}
