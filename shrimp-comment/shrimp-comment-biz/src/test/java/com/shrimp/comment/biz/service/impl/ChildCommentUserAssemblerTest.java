package com.shrimp.comment.biz.service.Impl;

import com.shrimp.comment.biz.domain.dataobject.CommentDO;
import com.shrimp.comment.biz.model.vo.FindChildCommentItemRspVO;
import com.shrimp.user.dto.resp.FindUserByIdRspDTO;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ChildCommentUserAssemblerTest {
    @Test void keepsAuthorAndRepliedUserSeparate() {
        var comment = CommentDO.builder().userId(1L).parentId(10L).replyCommentId(11L).replyUserId(2L).build();
        var result = new FindChildCommentItemRspVO();
        var author = FindUserByIdRspDTO.builder().id(1L).nickName("作者").avatar("author-avatar").build();
        var target = FindUserByIdRspDTO.builder().id(2L).nickName("被回复用户").avatar("target-avatar").build();
        ChildCommentUserAssembler.apply(comment, result, Map.of(1L, author, 2L, target));
        assertEquals("作者", result.getNickname());
        assertEquals("author-avatar", result.getAvatar());
        assertEquals("被回复用户", result.getReplyUserName());
        assertEquals(2L, result.getReplyUserId());
    }

    @Test void directReplyAndMissingUserDoNotInventNames() {
        var comment = CommentDO.builder().userId(1L).parentId(10L).replyCommentId(10L).replyUserId(2L).build();
        var result = new FindChildCommentItemRspVO();
        ChildCommentUserAssembler.apply(comment, result, Map.of(2L, FindUserByIdRspDTO.builder().nickName("目标").build()));
        assertNull(result.getNickname());
        assertNull(result.getAvatar());
        assertNull(result.getReplyUserName());
        assertNull(result.getReplyUserId());
    }
}
