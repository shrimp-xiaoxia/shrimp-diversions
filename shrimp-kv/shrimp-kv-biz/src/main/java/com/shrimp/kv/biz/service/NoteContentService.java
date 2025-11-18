package com.shrimp.kv.biz.service;


import com.shrimp.framework.common.response.Response;
import com.shrimp.kv.dto.req.AddNoteContentReqDTO;
import com.shrimp.kv.dto.req.DeleteNoteContentReqDTO;
import com.shrimp.kv.dto.req.FindNoteContentReqDTO;
import com.shrimp.kv.dto.rsp.FindNoteContentRspDTO;

public interface NoteContentService {

    Response<?> addNoteContent(AddNoteContentReqDTO addNoteContentReqDTO);

    Response<FindNoteContentRspDTO> findNoteContent(FindNoteContentReqDTO findNoteContentReqDTO);

    Response<?> deleteNoteContent(DeleteNoteContentReqDTO deleteNoteContentReqDTO);
}
