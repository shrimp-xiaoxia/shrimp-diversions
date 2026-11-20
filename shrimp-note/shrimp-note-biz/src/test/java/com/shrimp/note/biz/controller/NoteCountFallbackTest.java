package com.shrimp.note.biz.controller;

import com.shrimp.count.api.CountFeignApi;
import com.shrimp.framework.common.response.Response;
import com.shrimp.note.biz.model.vo.req.FindNoteDetailReqVO;
import com.shrimp.note.biz.model.vo.rsp.FindNoteDetailRspVO;
import com.shrimp.note.biz.service.NoteService;
import com.shrimp.note.biz.rpc.UserRpcService;
import com.shrimp.user.dto.resp.FindUserByIdRspDTO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NoteCountFallbackTest {
    @Test
    void detailRemainsReadableWithoutInventingCountsWhenCountServiceFails() {
        var controller = new NoteController();
        var notes = mock(NoteService.class);
        var counts = mock(CountFeignApi.class);
        ReflectionTestUtils.setField(controller, "noteService", notes);
        ReflectionTestUtils.setField(controller, "countFeignApi", counts);
        var request = FindNoteDetailReqVO.builder().id(42L).build();
        when(notes.findNoteDetail(request)).thenReturn(Response.success(
                FindNoteDetailRspVO.builder().id(42L).title("测试笔记").build()));
        when(counts.findNotes(any())).thenThrow(new IllegalStateException("模拟计数不可用"));

        var result = controller.findNoteDetail(request);
        assertTrue(result.isSuccess());
        assertEquals("测试笔记", result.getData().getTitle());
        assertNull(result.getData().getLikeTotal());
        assertNull(result.getData().getCollectTotal());
        assertNull(result.getData().getCommentTotal());
        assertFalse(result.getData().isLikeTotalAvailable());
        assertFalse(result.getData().isCollectTotalAvailable());
        assertFalse(result.getData().isCommentTotalAvailable());
    }

    @Test
    void detailRefreshesCreatorWithoutChangingCachedObject() {
        var controller = new NoteController();
        var notes = mock(NoteService.class);
        var counts = mock(CountFeignApi.class);
        var users = mock(UserRpcService.class);
        ReflectionTestUtils.setField(controller, "noteService", notes);
        ReflectionTestUtils.setField(controller, "countFeignApi", counts);
        ReflectionTestUtils.setField(controller, "userRpcService", users);
        var request = FindNoteDetailReqVO.builder().id(42L).build();
        var cached = FindNoteDetailRspVO.builder().id(42L).creatorId(7L).creatorName("旧昵称").avatar("old").build();
        when(notes.findNoteDetail(request)).thenReturn(Response.success(cached));
        when(users.findUserById(7L)).thenReturn(FindUserByIdRspDTO.builder().id(7L).nickName("新昵称").avatar("new").build());

        var result = controller.findNoteDetail(request);
        assertEquals("新昵称", result.getData().getCreatorName());
        assertEquals("new", result.getData().getAvatar());
        assertEquals("旧昵称", cached.getCreatorName());
        assertEquals("old", cached.getAvatar());

        when(users.findUserById(7L)).thenThrow(new IllegalStateException("用户服务不可用"));
        var unavailable = controller.findNoteDetail(request);
        assertNull(unavailable.getData().getCreatorName());
        assertNull(unavailable.getData().getAvatar());
    }
}
