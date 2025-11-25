package com.shrimp.note.biz.service;

import com.shrimp.framework.common.response.Response;
import com.shrimp.note.biz.model.vo.req.*;
import com.shrimp.note.biz.model.vo.rsp.FindNoteDetailRspVO;

/**
 * @author ccb
 */
public interface NoteService {

    Response<?> publishNote(PublishNoteReqVO publishNoteReqVO);

    Response<FindNoteDetailRspVO> findNoteDetail(FindNoteDetailReqVO findNoteDetailReqVO);

    Response<?> updateNote(UpdateNoteReqVO updateNoteReqVO);

    void deleteNoteDetailLocalCache(Long noteId);

    Response<?> deleteNote(DeleteNoteReqVO deleteNoteReqVO);

    Response<?> updateNoteVisibleOnlyMe(UpdateNoteVisibleOnlyMeReqVO updateNoteVisibleOnlyMeReqVO);

    Response<?> updateNoteIsTop(UpdateNoteIsTopReqVO updateNoteIsTopReqVO);
}
