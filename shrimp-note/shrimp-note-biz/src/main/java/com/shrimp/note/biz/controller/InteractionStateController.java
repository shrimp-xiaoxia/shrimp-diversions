package com.shrimp.note.biz.controller;

import com.shrimp.framework.biz.context.holder.LoginUserContextHolder;
import com.shrimp.framework.common.exception.BizException;
import com.shrimp.framework.common.response.Response;
import com.shrimp.note.biz.domain.dataobject.NoteDO;
import com.shrimp.note.biz.domain.mapper.NoteCollectionDOMapper;
import com.shrimp.note.biz.domain.mapper.NoteDOMapper;
import com.shrimp.note.biz.domain.mapper.NoteLikeDOMapper;
import com.shrimp.note.biz.enums.ResponseCodeEnum;
import com.shrimp.note.biz.model.vo.req.InteractionStateReqVO;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InteractionStateController {
    @Resource private NoteDOMapper notes;
    @Resource private NoteLikeDOMapper likes;
    @Resource private NoteCollectionDOMapper collections;
    public record InteractionState(boolean isLiked, boolean isCollected) {}
    @PostMapping("/note/interaction-state")
    public Response<InteractionState> state(@Validated @RequestBody InteractionStateReqVO request) {
        NoteDO note = notes.selectByPrimaryKey(request.getNoteId());
        if (note == null || note.getStatus() == null || note.getStatus() != 1) throw new BizException(ResponseCodeEnum.NOTE_NOT_EXITS);
        Long userId = LoginUserContextHolder.getLoginUserId();
        if (note.getVisible() != null && note.getVisible() == 1 && !note.getCreatorId().equals(userId)) throw new BizException(ResponseCodeEnum.NOTE_PRIVATE);
        if (userId == null) return Response.success(new InteractionState(false, false));
        try {
            return Response.success(new InteractionState(likes.selectCountByUserIdAndNoteId(userId, request.getNoteId()) > 0,
                    collections.selectCollectCountByUserIdAndNoteId(userId, request.getNoteId()) > 0));
        } catch (RuntimeException e) { throw new BizException(ResponseCodeEnum.INTERACTION_QUERY_FAIL); }
    }
}
