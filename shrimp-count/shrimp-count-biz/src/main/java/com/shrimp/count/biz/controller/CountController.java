package com.shrimp.count.biz.controller;

import com.shrimp.count.biz.domain.dataobject.NoteCountDO;
import com.shrimp.count.biz.domain.dataobject.UserCountDO;
import com.shrimp.count.biz.domain.mapper.NoteCountDOMapper;
import com.shrimp.count.biz.domain.mapper.UserCountDOMapper;
import com.shrimp.count.biz.enums.CountResponseCodeEnum;
import com.shrimp.count.dto.CountQueryReqDTO;
import com.shrimp.count.dto.CountResultDTO;
import com.shrimp.count.dto.CountSingleReqDTO;
import com.shrimp.framework.common.response.Response;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class CountController {
    @Resource private NoteCountDOMapper notes;
    @Resource private UserCountDOMapper users;

    @PostMapping("/count/note/find")
    public Response<CountResultDTO> findNote(@Validated @RequestBody CountSingleReqDTO request) {
        CountQueryReqDTO batch = new CountQueryReqDTO(); batch.setIds(List.of(request.getId()));
        Response<List<CountResultDTO>> result = findNotes(batch);
        return result.isSuccess() ? Response.success(result.getData().get(0)) : Response.fail(CountResponseCodeEnum.COUNT_QUERY_FAILED);
    }

    @PostMapping("/count/user/find")
    public Response<CountResultDTO> findUser(@Validated @RequestBody CountSingleReqDTO request) {
        CountQueryReqDTO batch = new CountQueryReqDTO(); batch.setIds(List.of(request.getId()));
        Response<List<CountResultDTO>> result = findUsers(batch);
        return result.isSuccess() ? Response.success(result.getData().get(0)) : Response.fail(CountResponseCodeEnum.COUNT_QUERY_FAILED);
    }

    @PostMapping("/count/note/batch-find")
    public Response<List<CountResultDTO>> findNotes(@Validated @RequestBody CountQueryReqDTO request) {
        try {
            Map<Long, NoteCountDO> rows = notes.selectByNoteIds(request.getIds()).stream()
                    .collect(Collectors.toMap(NoteCountDO::getNoteId, Function.identity()));
            return Response.success(request.getIds().stream().distinct().map(id -> {
                NoteCountDO row = rows.get(id);
                CountResultDTO result = new CountResultDTO(); result.setId(id);
                result.setLikeTotal(row == null ? Long.valueOf(0L) : row.getLikeTotal());
                result.setCollectTotal(row == null ? Long.valueOf(0L) : row.getCollectTotal());
                result.setCommentTotal(row == null ? Long.valueOf(0L) : row.getCommentTotal());
                return result;
            }).toList());
        } catch (RuntimeException e) { log.warn("笔记计数查询失败", e); return Response.fail(CountResponseCodeEnum.COUNT_QUERY_FAILED); }
    }

    @PostMapping("/count/user/batch-find")
    public Response<List<CountResultDTO>> findUsers(@Validated @RequestBody CountQueryReqDTO request) {
        try {
            Map<Long, UserCountDO> rows = users.selectByUserIds(request.getIds()).stream()
                    .collect(Collectors.toMap(UserCountDO::getUserId, Function.identity()));
            return Response.success(request.getIds().stream().distinct().map(id -> {
                UserCountDO row = rows.get(id);
                CountResultDTO result = new CountResultDTO(); result.setId(id);
                result.setLikeTotal(row == null ? Long.valueOf(0L) : row.getLikeTotal());
                result.setCollectTotal(row == null ? Long.valueOf(0L) : row.getCollectTotal());
                result.setFansTotal(row == null ? Long.valueOf(0L) : row.getFansTotal());
                result.setFollowingTotal(row == null ? Long.valueOf(0L) : row.getFollowingTotal());
                result.setNoteTotal(row == null ? Long.valueOf(0L) : row.getNoteTotal());
                return result;
            }).toList());
        } catch (RuntimeException e) { log.warn("用户计数查询失败", e); return Response.fail(CountResponseCodeEnum.COUNT_QUERY_FAILED); }
    }
}
