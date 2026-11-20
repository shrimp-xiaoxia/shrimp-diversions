package com.shrimp.count.biz.controller;
import com.shrimp.count.biz.domain.dataobject.NoteCountDO;
import com.shrimp.count.biz.domain.mapper.NoteCountDOMapper;
import com.shrimp.count.dto.CountQueryReqDTO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CountControllerTest {
    @Test void missingRowIsZeroButNullFieldRemainsUnavailable() {
        CountController controller = new CountController();
        NoteCountDOMapper mapper = mock(NoteCountDOMapper.class);
        ReflectionTestUtils.setField(controller, "notes", mapper);
        when(mapper.selectByNoteIds(List.of(1L, 2L))).thenReturn(List.of(NoteCountDO.builder().noteId(1L).likeTotal(null).collectTotal(7L).build()));
        CountQueryReqDTO request = new CountQueryReqDTO(); request.setIds(List.of(1L, 2L));
        var response = controller.findNotes(request);
        assertTrue(response.isSuccess());
        assertNull(response.getData().get(0).getLikeTotal());
        assertEquals(7L, response.getData().get(0).getCollectTotal());
        assertEquals(0L, response.getData().get(1).getLikeTotal());
    }
    @Test void databaseFailureHasCountError() {
        CountController controller = new CountController();
        NoteCountDOMapper mapper = mock(NoteCountDOMapper.class);
        ReflectionTestUtils.setField(controller, "notes", mapper);
        when(mapper.selectByNoteIds(anyList())).thenThrow(new IllegalStateException());
        CountQueryReqDTO request = new CountQueryReqDTO(); request.setIds(List.of(1L));
        assertEquals("COUNT-20100", controller.findNotes(request).getErrorCode());
    }
}
