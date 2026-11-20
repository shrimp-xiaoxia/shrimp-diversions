package com.shrimp.comment.biz.service.Impl;

import com.shrimp.comment.biz.domain.dataobject.CommentDO;
import com.shrimp.comment.biz.model.vo.FindChildCommentItemRspVO;
import com.shrimp.user.dto.resp.FindUserByIdRspDTO;

import java.util.Map;
import java.util.Objects;

/** 将评论作者和被回复用户映射到不同字段。 */
final class ChildCommentUserAssembler {
    private ChildCommentUserAssembler() {}

    static void apply(CommentDO comment, FindChildCommentItemRspVO result,
                      Map<Long, FindUserByIdRspDTO> users) {
        if (users == null || users.isEmpty()) return;
        FindUserByIdRspDTO author = comment.getUserId() == null ? null : users.get(comment.getUserId());
        if (author != null) {
            result.setAvatar(author.getAvatar());
            result.setNickname(author.getNickName());
        }
        Long replyCommentId = comment.getReplyCommentId();
        Long replyUserId = comment.getReplyUserId();
        if (replyCommentId == null || Objects.equals(replyCommentId, comment.getParentId()) || replyUserId == null) return;
        FindUserByIdRspDTO repliedUser = users.get(replyUserId);
        result.setReplyUserId(replyUserId);
        if (repliedUser != null) result.setReplyUserName(repliedUser.getNickName());
    }
}
