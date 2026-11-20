package com.shrimp.note.biz.controller;

import com.shrimp.framework.biz.context.holder.LoginUserContextHolder;
import com.shrimp.framework.common.exception.BizException;
import com.shrimp.note.biz.domain.dataobject.NoteDO;
import com.shrimp.note.biz.domain.mapper.NoteDOMapper;
import com.shrimp.note.biz.domain.mapper.NoteLikeDOMapper;
import com.shrimp.note.biz.domain.mapper.NoteCollectionDOMapper;
import com.shrimp.note.biz.model.vo.req.InteractionStateReqVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InteractionStateControllerTest {
    private final InteractionStateController controller = new InteractionStateController();
    private final NoteDOMapper notes = mock(NoteDOMapper.class);
    private final NoteLikeDOMapper likes = mock(NoteLikeDOMapper.class);
    private final NoteCollectionDOMapper collections = mock(NoteCollectionDOMapper.class);

    InteractionStateControllerTest() {
        ReflectionTestUtils.setField(controller, "notes", notes);
        ReflectionTestUtils.setField(controller, "likes", likes);
        ReflectionTestUtils.setField(controller, "collections", collections);
    }
    @AfterEach void clear() { LoginUserContextHolder.removeLoginUserContext(); }
    @Test void publicGuestAndPrivateNoteRespectVisibility() {
        NoteDO note = NoteDO.builder().id(1L).creatorId(2L).status((byte) 1).visible((byte) 0).build();
        when(notes.selectByPrimaryKey(1L)).thenReturn(note);
        InteractionStateReqVO request = new InteractionStateReqVO(); request.setNoteId(1L);
        assertFalse(controller.state(request).getData().isLiked());
        verifyNoInteractions(likes, collections);
        note.setVisible((byte) 1);
        assertThrows(BizException.class, () -> controller.state(request));
    }
    @Test void queryFailureIsNotReportedAsFalse() {
        when(notes.selectByPrimaryKey(1L)).thenReturn(NoteDO.builder().id(1L).creatorId(2L).status((byte) 1).visible((byte) 0).build());
        LoginUserContextHolder.setLoginUserId(3L);
        when(likes.selectCountByUserIdAndNoteId(3L, 1L)).thenThrow(new IllegalStateException());
        InteractionStateReqVO request = new InteractionStateReqVO(); request.setNoteId(1L);
        assertThrows(BizException.class, () -> controller.state(request));
    }
}
