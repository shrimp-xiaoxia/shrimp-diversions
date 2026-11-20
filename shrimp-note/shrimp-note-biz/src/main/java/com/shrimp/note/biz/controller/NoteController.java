package com.shrimp.note.biz.controller;

import com.shrimp.framework.biz.operationlog.aspect.ApiOperationLog;
import com.shrimp.framework.common.response.Response;
import com.shrimp.note.biz.model.vo.req.*;
import com.shrimp.note.biz.model.vo.rsp.FindNoteDetailRspVO;
import com.shrimp.note.biz.service.NoteService;
import com.shrimp.note.biz.rpc.UserRpcService;
import com.shrimp.user.dto.resp.FindUserByIdRspDTO;
import com.shrimp.count.api.CountFeignApi;
import com.shrimp.count.dto.CountQueryReqDTO;
import com.shrimp.count.dto.CountResultDTO;
import java.util.List;
import org.springframework.beans.BeanUtils;
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
    @Resource private CountFeignApi countFeignApi;
    @Resource private UserRpcService userRpcService;

    @PostMapping("/publish")
    @ApiOperationLog(description = "发布笔记接口")
    public Response<?> publishNote(@Validated @RequestBody PublishNoteReqVO publishNoteReqVO) {
        return noteService.publishNote(publishNoteReqVO);
    }

    @PostMapping("/detail")
    @ApiOperationLog(description = "查询笔记详情接口")
    public Response<FindNoteDetailRspVO> findNoteDetail(@Validated @RequestBody FindNoteDetailReqVO findNoteDetailReqVO) {
        Response<FindNoteDetailRspVO> response = noteService.findNoteDetail(findNoteDetailReqVO);
        if (response == null || !response.isSuccess() || response.getData() == null) return response;
        FindNoteDetailRspVO data = new FindNoteDetailRspVO();
        BeanUtils.copyProperties(response.getData(), data);
        data.setCreatorName(null);
        data.setAvatar(null);
        if (data.getCreatorId() != null) {
            try {
                FindUserByIdRspDTO creator = userRpcService.findUserById(data.getCreatorId());
                if (creator != null) {
                    data.setCreatorName(creator.getNickName());
                    data.setAvatar(creator.getAvatar());
                }
            } catch (RuntimeException e) { log.warn("笔记作者资料查询失败", e); }
        }
        try {
            CountQueryReqDTO query = new CountQueryReqDTO(); query.setIds(List.of(findNoteDetailReqVO.getId()));
            Response<List<CountResultDTO>> counts = countFeignApi.findNotes(query);
            if (counts != null && counts.isSuccess() && counts.getData() != null && !counts.getData().isEmpty()) {
                CountResultDTO value = counts.getData().get(0);
                data.setLikeTotal(value.getLikeTotal()); data.setLikeTotalAvailable(value.getLikeTotal() != null);
                data.setCollectTotal(value.getCollectTotal()); data.setCollectTotalAvailable(value.getCollectTotal() != null);
                data.setCommentTotal(value.getCommentTotal()); data.setCommentTotalAvailable(value.getCommentTotal() != null);
            }
        } catch (RuntimeException e) { log.warn("笔记计数查询失败", e); }
        return Response.success(data);
    }

    @PostMapping("/update")
    @ApiOperationLog(description = "更新笔记接口")
    public Response<?> updateNote(@Validated @RequestBody UpdateNoteReqVO updateNoteReqVO) {
        return noteService.updateNote(updateNoteReqVO);
    }

    @PostMapping("/delete")
    @ApiOperationLog(description = "删除笔记接口")
    public Response<?> deleteNote(@Validated @RequestBody DeleteNoteReqVO deleteNoteReqVO) {
        return noteService.deleteNote(deleteNoteReqVO);
    }

    @PostMapping("/visible/onlyme")
    @ApiOperationLog(description = "笔记仅对自己可见接口")
    public Response<?> visibleOnlyMe(@Validated @RequestBody UpdateNoteVisibleOnlyMeReqVO updateNoteVisibleOnlyMeReqVO) {
        return noteService.updateNoteVisibleOnlyMe(updateNoteVisibleOnlyMeReqVO);
    }

    @PostMapping("/top")
    @ApiOperationLog(description = "更新笔记置顶情况接口")
    public Response<?> updateNoteIsTop(@Validated @RequestBody UpdateNoteIsTopReqVO updateNoteIsTopReqVO) {
        return noteService.updateNoteIsTop(updateNoteIsTopReqVO);
    }

    @PostMapping("/like")
    @ApiOperationLog(description = "点赞笔记接口")
    public Response<?> likeNote(@Validated @RequestBody LikeNoteReqVO likeNoteReqVO) {
        return noteService.likeNote(likeNoteReqVO);
    }

    @PostMapping("/unlike")
    @ApiOperationLog(description = "取消点赞笔记接口")
    public Response<?> unlikeNote(@Validated @RequestBody UnlikeNoteReqVO unlikeNoteReqVO) {
        return noteService.unlikeNote(unlikeNoteReqVO);
    }

    @PostMapping("/collect")
    @ApiOperationLog(description = "收藏笔记接口")
    public Response<?> collectNote(@Validated @RequestBody CollectNoteReqVO collectNoteReqVO) {
        return noteService.collectNote(collectNoteReqVO);
    }

    @PostMapping("/uncollect")
    @ApiOperationLog(description = "取消收藏笔记接口")
    public Response<?> unCollectNote(@Validated @RequestBody UnCollectNoteReqVO unCollectNoteReqVO) {
        return noteService.unCollectNote(unCollectNoteReqVO);
    }
}
