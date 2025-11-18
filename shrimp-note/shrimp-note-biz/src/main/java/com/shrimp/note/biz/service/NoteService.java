package com.shrimp.note.biz.service;

import com.shrimp.framework.common.response.Response;
import com.shrimp.note.biz.model.vo.req.FindNoteDetailReqVO;
import com.shrimp.note.biz.model.vo.req.PublishNoteReqVO;
import com.shrimp.note.biz.model.vo.req.UpdateNoteReqVO;
import com.shrimp.note.biz.model.vo.rsp.FindNoteDetailRspVO;

public interface NoteService {

    Response<?> publishNote(PublishNoteReqVO publishNoteReqVO);

    Response<FindNoteDetailRspVO> findNoteDetail(FindNoteDetailReqVO findNoteDetailReqVO);

    Response<?> updateNote(UpdateNoteReqVO updateNoteReqVO);
}
