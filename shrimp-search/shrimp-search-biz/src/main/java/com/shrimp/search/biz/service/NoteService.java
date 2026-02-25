package com.shrimp.search.biz.service;

import com.shrimp.framework.common.response.PageResponse;
import com.shrimp.framework.common.response.Response;
import com.shrimp.search.biz.model.vo.SearchNoteReqVO;
import com.shrimp.search.biz.model.vo.SearchNoteRspVO;
import com.shrimp.search.dto.RebuildNoteDocumentReqDTO;

/**
 * @author ccb
 */
public interface NoteService {
    PageResponse<SearchNoteRspVO> searchNote(SearchNoteReqVO searchNoteReqVO);

    /**
     * 重建笔记文档
     * @param rebuildNoteDocumentReqDTO
     * @return
     */
    Response<Long> rebuildDocument(RebuildNoteDocumentReqDTO rebuildNoteDocumentReqDTO);
}
