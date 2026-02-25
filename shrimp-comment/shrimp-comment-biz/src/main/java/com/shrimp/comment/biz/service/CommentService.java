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
     * @param findCommentPageListReqVO
     * @return
     */
    PageResponse<FindCommentItemRspVO> findCommentPageList(FindCommentPageListReqVO findCommentPageListReqVO);


    /**
     * 二级评论分页查询
     * @param findChildCommentPageListReqVO
     * @return
     */
    PageResponse<FindChildCommentItemRspVO> findChildCommentPageList(FindChildCommentPageListReqVO findChildCommentPageListReqVO);
}
