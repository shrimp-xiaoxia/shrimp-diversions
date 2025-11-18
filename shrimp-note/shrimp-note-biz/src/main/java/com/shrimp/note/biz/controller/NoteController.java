package com.shrimp.note.biz.controller;

import com.shrimp.framework.biz.operationlog.aspect.ApiOperationLog;
import com.shrimp.framework.common.response.Response;
import com.shrimp.note.biz.model.vo.req.FindNoteDetailReqVO;
import com.shrimp.note.biz.model.vo.req.PublishNoteReqVO;
import com.shrimp.note.biz.model.vo.req.UpdateNoteReqVO;
import com.shrimp.note.biz.model.vo.rsp.FindNoteDetailRspVO;
import com.shrimp.note.biz.service.NoteService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/note")
@Slf4j
public class NoteController {
    @Resource
    private NoteService noteService;

    @PostMapping("/publish")
    @ApiOperationLog(description = "发布笔记接口")
    public Response<?> publishNote(@Validated @RequestBody PublishNoteReqVO publishNoteReqVO) {
        return noteService.publishNote(publishNoteReqVO);
    }

    @PostMapping("/detail")
    @ApiOperationLog(description = "查询笔记详情接口")
    public Response<FindNoteDetailRspVO> findNoteDetail(@Validated @RequestBody FindNoteDetailReqVO findNoteDetailReqVO) {
        return noteService.findNoteDetail(findNoteDetailReqVO);
    }

    @PostMapping("/update")
    @ApiOperationLog(description = "更新笔记接口")
    public Response<?> updateNote(@Validated @RequestBody UpdateNoteReqVO updateNoteReqVO) {
        return noteService.updateNote(updateNoteReqVO);
    }
}
