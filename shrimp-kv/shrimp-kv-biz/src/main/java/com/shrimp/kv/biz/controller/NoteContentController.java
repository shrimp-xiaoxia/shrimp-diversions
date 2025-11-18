package com.shrimp.kv.biz.controller;

import com.shrimp.framework.biz.operationlog.aspect.ApiOperationLog;
import com.shrimp.framework.common.response.Response;
import com.shrimp.kv.biz.service.NoteContentService;
import com.shrimp.kv.dto.req.AddNoteContentReqDTO;
import com.shrimp.kv.dto.req.DeleteNoteContentReqDTO;
import com.shrimp.kv.dto.req.FindNoteContentReqDTO;
import com.shrimp.kv.dto.rsp.FindNoteContentRspDTO;
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
@RequestMapping("/kv")
@Slf4j
public class NoteContentController {
    @Resource
    private NoteContentService noteContentService;

    @PostMapping("/note/content/add")
    @ApiOperationLog(description = "添加笔记内容接口")
    public Response<?> addNoteContent(@Validated @RequestBody AddNoteContentReqDTO addNoteContentReqDTO){
        return noteContentService.addNoteContent(addNoteContentReqDTO);
    }

    @PostMapping("/note/content/find")
    @ApiOperationLog(description = "查询笔记内存接口")
    public Response<FindNoteContentRspDTO>  findNoteContent(@Validated @RequestBody FindNoteContentReqDTO findNoteContentReqDTO){
        return noteContentService.findNoteContent(findNoteContentReqDTO);
    }

    @PostMapping("/note/content/delete")
    @ApiOperationLog(description = "删除笔记内存接口")
    public Response<?>  deleteNoteContent(@Validated @RequestBody DeleteNoteContentReqDTO deleteNoteContentReqDTO){
        return noteContentService.deleteNoteContent(deleteNoteContentReqDTO);
    }
}
