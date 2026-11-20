package com.shrimp.search.biz.service;

import com.shrimp.framework.common.exception.BizException;
import com.shrimp.search.biz.model.vo.SearchNoteReqVO;
import com.shrimp.search.biz.service.Impl.NoteServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class NoteSearchFailureTest {
    @Test void unavailableClientDoesNotReturnEmptySuccess() {
        var service = new NoteServiceImpl();
        var request = SearchNoteReqVO.builder().keyword("test").pageNo(1).build();
        assertThrows(BizException.class, () -> service.searchNote(request));
    }
}
