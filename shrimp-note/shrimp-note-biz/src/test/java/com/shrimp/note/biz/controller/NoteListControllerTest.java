package com.shrimp.note.biz.controller;

import com.shrimp.framework.biz.context.holder.LoginUserContextHolder;
import com.shrimp.framework.common.exception.BizException;
import com.shrimp.framework.common.response.Response;
import com.shrimp.count.api.CountFeignApi;
import com.shrimp.note.biz.domain.dataobject.ChannelDO;
import com.shrimp.note.biz.domain.dataobject.NoteDO;
import com.shrimp.note.biz.domain.mapper.ChannelDOMapper;
import com.shrimp.note.biz.domain.mapper.NoteDOMapper;
import com.shrimp.note.biz.domain.mapper.NoteLikeDOMapper;
import com.shrimp.note.biz.model.vo.req.DiscoverReqVO;
import com.shrimp.note.biz.model.vo.req.ProfileNotesReqVO;
import com.shrimp.user.api.UserFeignApi;
import com.shrimp.user.dto.resp.FindUserByIdRspDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NoteListControllerTest {
    @AfterEach void clear() { LoginUserContextHolder.removeLoginUserContext(); }
    @Test void otherUsersCannotReadPrivateCollections() {
        NoteListController controller = new NoteListController();
        NoteDOMapper mapper = mock(NoteDOMapper.class);
        ReflectionTestUtils.setField(controller, "notes", mapper);
        LoginUserContextHolder.setLoginUserId(2L);
        ProfileNotesReqVO request = new ProfileNotesReqVO(); request.setUserId(1L); request.setType(2); request.setPageNo(1);
        assertThrows(BizException.class, () -> controller.profile(request));
        verifyNoInteractions(mapper);
    }
    @Test void pageBeyondEndRetainsTotalAndNullData() {
        NoteListController controller = new NoteListController();
        NoteDOMapper mapper = mock(NoteDOMapper.class);
        ReflectionTestUtils.setField(controller, "notes", mapper);
        when(mapper.countProfile(1L, 1, false)).thenReturn(11L);
        when(mapper.findProfile(1L, 1, false, 20L)).thenReturn(List.of());
        ProfileNotesReqVO request = new ProfileNotesReqVO(); request.setUserId(1L); request.setType(1); request.setPageNo(3);
        var page = controller.profile(request);
        assertNull(page.getData()); assertEquals(11, page.getTotalCount()); assertEquals(2, page.getTotalPage());
    }
    @Test void discoverRetainsNotesWhenCountsFail() {
        var controller = new NoteListController();
        var notes = mock(NoteDOMapper.class);
        var channels = mock(ChannelDOMapper.class);
        var users = mock(UserFeignApi.class);
        var counts = mock(CountFeignApi.class);
        ReflectionTestUtils.setField(controller, "notes", notes);
        ReflectionTestUtils.setField(controller, "channels", channels);
        ReflectionTestUtils.setField(controller, "users", users);
        ReflectionTestUtils.setField(controller, "counts", counts);
        when(channels.selectEnabled()).thenReturn(List.of(ChannelDO.builder().id(1L).build()));
        when(notes.countDiscover(0L)).thenReturn(1L);
        when(notes.findDiscover(0L, 0L)).thenReturn(List.of(NoteDO.builder()
                .id(42L).creatorId(7L).title("测试笔记").type((byte) 0).visible((byte) 0).build()));
        when(users.findUserByIds(any())).thenReturn(Response.success(List.of(
                FindUserByIdRspDTO.builder().id(7L).nickName("测试作者").build())));
        when(counts.findNotes(any())).thenThrow(new IllegalStateException("模拟计数不可用"));
        var request = new DiscoverReqVO(); request.setChannelId(0L); request.setPageNo(1);

        var page = controller.discover(request);
        assertTrue(page.isSuccess());
        assertEquals(1, page.getTotalCount());
        assertEquals("42", page.getData().get(0).getId());
        assertEquals("测试作者", page.getData().get(0).getNickname());
        assertNull(page.getData().get(0).getLikeTotal());
        assertFalse(page.getData().get(0).isLikeTotalAvailable());
    }

    @Test void discoverReturnsPerViewerLikeStateInOneBatch() throws Exception {
        var controller = new NoteListController();
        var notes = mock(NoteDOMapper.class);
        var channels = mock(ChannelDOMapper.class);
        var users = mock(UserFeignApi.class);
        var counts = mock(CountFeignApi.class);
        var likes = mock(NoteLikeDOMapper.class);
        ReflectionTestUtils.setField(controller, "notes", notes);
        ReflectionTestUtils.setField(controller, "channels", channels);
        ReflectionTestUtils.setField(controller, "users", users);
        ReflectionTestUtils.setField(controller, "counts", counts);
        ReflectionTestUtils.setField(controller, "likes", likes);
        LoginUserContextHolder.setLoginUserId(7L);
        when(channels.selectEnabled()).thenReturn(List.of(ChannelDO.builder().id(1L).build()));
        when(notes.countDiscover(0L)).thenReturn(2L);
        when(notes.findDiscover(0L, 0L)).thenReturn(List.of(
                NoteDO.builder().id(42L).creatorId(7L).type((byte) 0).visible((byte) 0).build(),
                NoteDO.builder().id(43L).creatorId(7L).type((byte) 0).visible((byte) 0).build()));
        when(users.findUserByIds(any())).thenReturn(Response.success(List.of(
                FindUserByIdRspDTO.builder().id(7L).nickName("测试作者").build())));
        when(likes.selectLikedNoteIds(7L, List.of(42L, 43L))).thenReturn(List.of(42L));
        var request = new DiscoverReqVO(); request.setChannelId(0L); request.setPageNo(1);

        var page = controller.discover(request);
        assertEquals(Boolean.TRUE, page.getData().get(0).getIsLiked());
        assertEquals(Boolean.FALSE, page.getData().get(1).getIsLiked());
        assertTrue(new ObjectMapper().valueToTree(page.getData().get(0)).get("isLiked").asBoolean());
        verify(likes).selectLikedNoteIds(7L, List.of(42L, 43L));
        when(likes.selectLikedNoteIds(anyLong(), anyList())).thenThrow(new IllegalStateException());
        assertNull(controller.discover(request).getData().get(0).getIsLiked());
    }
}
