package com.shrimp.comment.biz.service;

import com.shrimp.comment.biz.model.vo.FindChildCommentItemRspVO;
import com.shrimp.comment.biz.model.vo.FindCommentItemRspVO;
import com.shrimp.comment.biz.rpc.UserRpcService;
import com.shrimp.user.dto.resp.FindUserByIdRspDTO;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.stream.LongStream;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class CommentUserFreshenerTest {
    @Test void splitsRequestsIntoAtMostTenUsersAndContinuesAfterFailure() {
        var rpc = mock(UserRpcService.class);
        when(rpc.findUserByIds(anyList())).thenThrow(new IllegalStateException("unavailable"))
                .thenAnswer(call -> ((List<Long>) call.getArgument(0)).stream()
                        .map(id -> FindUserByIdRspDTO.builder().id(id).avatar("fresh").build()).toList());
        var comments = LongStream.rangeClosed(1, 12)
                .mapToObj(id -> FindCommentItemRspVO.builder().userId(id).avatar("cached").build()).toList();
        new CommentUserFreshener(rpc).refreshTopLevel(comments);
        ArgumentCaptor<List<Long>> batches = ArgumentCaptor.forClass(List.class);
        verify(rpc, times(2)).findUserByIds(batches.capture());
        assertEquals(List.of(10, 2), batches.getAllValues().stream().map(List::size).toList());
        assertEquals(2, comments.stream().filter(comment -> "fresh".equals(comment.getAvatar())).count());
        assertEquals(10, comments.stream().filter(comment -> "cached".equals(comment.getAvatar())).count());
    }

    @Test void refreshesAuthorsAndReplyTargetsInOneBatch() {
        var rpc = mock(UserRpcService.class);
        when(rpc.findUserByIds(anyList())).thenReturn(List.of(
                FindUserByIdRspDTO.builder().id(1L).nickName("新昵称").avatar("新头像").build(),
                FindUserByIdRspDTO.builder().id(2L).nickName("被回复者").build()));
        var freshener = new CommentUserFreshener(rpc);
        var child = FindChildCommentItemRspVO.builder().userId(1L).avatar("旧头像").replyUserId(2L).replyUserName("旧昵称").build();
        freshener.refreshChildren(List.of(child));
        assertEquals("新头像", child.getAvatar());
        assertEquals("新昵称", child.getNickname());
        assertEquals("被回复者", child.getReplyUserName());
        verify(rpc, times(1)).findUserByIds(anyList());
    }

    @Test void refreshesTopAndFirstReplyAndFallsBackOnFailure() {
        var rpc = mock(UserRpcService.class);
        when(rpc.findUserByIds(anyList())).thenReturn(List.of(FindUserByIdRspDTO.builder().id(1L).avatar("当前头像").build()))
                .thenThrow(new IllegalStateException("service unavailable"));
        var freshener = new CommentUserFreshener(rpc);
        var reply = FindCommentItemRspVO.builder().userId(1L).avatar("旧头像").build();
        var top = FindCommentItemRspVO.builder().userId(1L).avatar("旧头像").firstReplyComment(reply).build();
        freshener.refreshTopLevel(List.of(top));
        assertEquals("当前头像", top.getAvatar());
        assertEquals("当前头像", reply.getAvatar());
        freshener.refreshTopLevel(List.of(top));
        assertEquals("当前头像", top.getAvatar());
    }
}
