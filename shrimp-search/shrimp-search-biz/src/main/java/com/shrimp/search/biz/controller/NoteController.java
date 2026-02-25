package com.shrimp.search.biz.controller;

import com.shrimp.framework.biz.operationlog.aspect.ApiOperationLog;
import com.shrimp.framework.common.response.PageResponse;
import com.shrimp.framework.common.response.Response;
import com.shrimp.search.biz.model.vo.SearchNoteReqVO;
import com.shrimp.search.biz.model.vo.SearchNoteRspVO;
import com.shrimp.search.biz.service.NoteService;
import com.shrimp.search.dto.RebuildNoteDocumentReqDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author ccb
 */
@RestController
@RequestMapping("/search")
@Slf4j
public class NoteController {
    @Resource
    private NoteService noteService;

    @RequestMapping("/note")
    @ApiOperationLog(description = "搜索笔记接口")
    public PageResponse<SearchNoteRspVO> searchNote(@Validated @RequestBody SearchNoteReqVO searchNoteReqVO) {
        return noteService.searchNote(searchNoteReqVO);
    }
    // ===================================== 对其他服务提供的接口 =====================================
    @PostMapping("/note/document/rebuild")
    @ApiOperationLog(description = "用户文档重建")
    public Response<Long> rebuildDocument(@Validated @RequestBody RebuildNoteDocumentReqDTO rebuildNoteDocumentReqDTO) {
        return noteService.rebuildDocument(rebuildNoteDocumentReqDTO);
    }

}
