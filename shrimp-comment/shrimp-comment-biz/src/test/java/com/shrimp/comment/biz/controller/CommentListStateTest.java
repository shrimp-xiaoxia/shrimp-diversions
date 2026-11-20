package com.shrimp.comment.biz.controller;
import com.shrimp.comment.biz.domain.mapper.CommentLikeDOMapper;
import com.shrimp.comment.biz.domain.mapper.NoteCountDOMapper;
import com.shrimp.comment.biz.domain.mapper.CommentDOMapper;
import com.shrimp.comment.biz.domain.dataobject.CommentDO;
import com.shrimp.comment.biz.model.vo.FindChildCommentPageListReqVO;
import com.shrimp.comment.biz.model.vo.FindChildCommentItemRspVO;
import com.shrimp.comment.biz.model.vo.FindCommentItemRspVO;
import com.shrimp.comment.biz.model.vo.FindCommentPageListReqVO;
import com.shrimp.comment.biz.service.CommentService;
import com.shrimp.comment.biz.service.CommentUserFreshener;
import com.shrimp.comment.biz.rpc.UserRpcService;
import com.shrimp.user.dto.resp.FindUserByIdRspDTO;
import com.shrimp.framework.biz.context.holder.LoginUserContextHolder;
import com.shrimp.framework.common.response.PageResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;

class CommentListStateTest {
    @AfterEach void clear() { LoginUserContextHolder.removeLoginUserContext(); }
    @Test void userStateIsAddedWithoutMutatingCachedComment() throws Exception {
        CommentController controller = new CommentController();
        CommentService service = mock(CommentService.class); NoteCountDOMapper notes = mock(NoteCountDOMapper.class);
        CommentDOMapper comments = mock(CommentDOMapper.class);
        CommentLikeDOMapper likes = mock(CommentLikeDOMapper.class);
        ReflectionTestUtils.setField(controller, "commentService", service);
        ReflectionTestUtils.setField(controller, "noteCounts", notes);
        ReflectionTestUtils.setField(controller, "commentLikes", likes);
        ReflectionTestUtils.setField(controller, "comments", comments);
        ReflectionTestUtils.setField(controller, "commentUserFreshener", mock(CommentUserFreshener.class));
        LoginUserContextHolder.setLoginUserId(3L);
        when(notes.countCommentableNote(1L, 3L)).thenReturn(1L);
        FindCommentItemRspVO reply = FindCommentItemRspVO.builder().commentId(22L).build();
        FindCommentItemRspVO cached = FindCommentItemRspVO.builder().commentId(11L).firstReplyComment(reply).build();
        FindCommentPageListReqVO request = new FindCommentPageListReqVO(); request.setNoteId(1L); request.setPageNo(1);
        when(service.findCommentPageList(request)).thenReturn(PageResponse.success(List.of(cached), 1, 1));
        when(likes.selectLikedCommentIds(3L, List.of(11L, 22L))).thenReturn(List.of(22L));
        var result = controller.findCommentPageList(request).getData().get(0);
        assertFalse(result.getIsLiked()); assertTrue(result.getFirstReplyComment().getIsLiked());
        assertNull(cached.getIsLiked()); assertNull(reply.getIsLiked());
        String json = new ObjectMapper().writeValueAsString(result);
        assertTrue(json.contains("\"isLiked\":false"));
        assertTrue(json.contains("\"commentId\":\"11\""));
    }
    @Test void topLevelPagesDoNotCountReplies() {
        CommentController controller = new CommentController();
        CommentService service = mock(CommentService.class);
        NoteCountDOMapper notes = mock(NoteCountDOMapper.class);
        CommentDOMapper comments = mock(CommentDOMapper.class);
        ReflectionTestUtils.setField(controller, "commentService", service);
        ReflectionTestUtils.setField(controller, "noteCounts", notes);
        ReflectionTestUtils.setField(controller, "comments", comments);
        LoginUserContextHolder.setLoginUserId(3L);
        when(notes.countCommentableNote(1L, 3L)).thenReturn(1L);
        when(comments.countTopLevelByNoteId(1L)).thenReturn(3L, 11L);
        FindCommentPageListReqVO request = new FindCommentPageListReqVO();
        request.setNoteId(1L); request.setPageNo(1);
        when(service.findCommentPageList(request)).thenAnswer(i -> PageResponse.<FindCommentItemRspVO>success(null, 1, 11));
        var first = controller.findCommentPageList(request);
        assertEquals(11, first.getTotalCount());
        assertEquals(1, first.getTotalPage());
        assertEquals(2, controller.findCommentPageList(request).getTotalPage());
    }
    @Test void refreshedUserDataDoesNotMutateCachedComments() {
        CommentController controller = new CommentController();
        CommentService service = mock(CommentService.class);
        NoteCountDOMapper notes = mock(NoteCountDOMapper.class);
        CommentDOMapper comments = mock(CommentDOMapper.class);
        UserRpcService users = mock(UserRpcService.class);
        ReflectionTestUtils.setField(controller, "commentService", service);
        ReflectionTestUtils.setField(controller, "noteCounts", notes);
        ReflectionTestUtils.setField(controller, "comments", comments);
        ReflectionTestUtils.setField(controller, "commentUserFreshener", new CommentUserFreshener(users));
        when(notes.countCommentableNote(1L, null)).thenReturn(1L);
        when(users.findUserByIds(anyList())).thenReturn(List.of(FindUserByIdRspDTO.builder().id(7L).avatar("new-avatar").build()));
        var cachedReply = FindCommentItemRspVO.builder().commentId(12L).userId(7L).avatar("old-avatar").build();
        var cached = FindCommentItemRspVO.builder().commentId(11L).userId(7L).avatar("old-avatar").firstReplyComment(cachedReply).build();
        var request = new FindCommentPageListReqVO(); request.setNoteId(1L); request.setPageNo(1);
        when(service.findCommentPageList(request)).thenReturn(PageResponse.success(List.of(cached), 1, 1));
        var result = controller.findCommentPageList(request).getData().get(0);
        assertEquals("new-avatar", result.getAvatar());
        assertEquals("new-avatar", result.getFirstReplyComment().getAvatar());
        assertEquals("old-avatar", cached.getAvatar());
        assertEquals("old-avatar", cachedReply.getAvatar());
    }
    @Test void childPageCountRespectsWhetherFirstReplyIsIncluded() {
        CommentController controller = new CommentController();
        CommentService service = mock(CommentService.class);
        NoteCountDOMapper notes = mock(NoteCountDOMapper.class);
        CommentDOMapper comments = mock(CommentDOMapper.class);
        ReflectionTestUtils.setField(controller, "commentService", service);
        ReflectionTestUtils.setField(controller, "noteCounts", notes);
        ReflectionTestUtils.setField(controller, "comments", comments);
        LoginUserContextHolder.setLoginUserId(3L);
        when(comments.selectByPrimaryKey(11L)).thenReturn(CommentDO.builder().noteId(1L).build());
        when(notes.countCommentableNote(1L, 3L)).thenReturn(1L);
        FindChildCommentPageListReqVO request = new FindChildCommentPageListReqVO();
        request.setParentCommentId(11L); request.setPageNo(1);
        when(service.findChildCommentPageList(request)).thenAnswer(invocation -> PageResponse.<FindChildCommentItemRspVO>success(null, 1, 7, 6));

        request.setIncludeFirst(true);
        assertEquals(2, controller.findChildCommentPageList(request).getTotalPage());
        request.setIncludeFirst(false);
        assertEquals(1, controller.findChildCommentPageList(request).getTotalPage());
        request.setIncludeFirst(null);
        assertEquals(1, controller.findChildCommentPageList(request).getTotalPage());
    }
}
