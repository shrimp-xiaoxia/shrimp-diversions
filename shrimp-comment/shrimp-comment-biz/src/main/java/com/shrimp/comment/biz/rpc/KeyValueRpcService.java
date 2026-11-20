package com.shrimp.comment.biz.rpc;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import com.google.common.collect.Lists;
import com.shrimp.comment.biz.domain.bo.CommentBO;
import com.shrimp.comment.biz.domain.dataobject.CommentDO;
import com.shrimp.framework.common.constants.DateConstants;
import com.shrimp.framework.common.response.Response;
import com.shrimp.kv.api.KeyValueFeignApi;
import com.shrimp.kv.dto.req.*;
import com.shrimp.kv.dto.rsp.FindCommentContentRspDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
@Slf4j
public class KeyValueRpcService {
    @Resource
    private KeyValueFeignApi keyValueFeignApi;

    public boolean batchSaveCommentContent(List<CommentBO> commentBOS) {
        List<CommentContentReqDTO> commentContentReqDTOS = Lists.newArrayList();
        commentBOS.forEach(commentBO -> {
            CommentContentReqDTO commentContentReqDTO = CommentContentReqDTO.builder()
                    .noteId(commentBO.getNoteId())
                    .contentId(commentBO.getContentUuid())
                    .content(commentBO.getContent())
                    .yearMonth(commentBO.getCreateTime().format(DateConstants.DATE_FORMAT_Y_M))
                    .build();
            commentContentReqDTOS.add(commentContentReqDTO);
        });

        BatchAddCommentContentReqDTO batchAddCommentContentReqDTO = BatchAddCommentContentReqDTO.builder()
                .comments(commentContentReqDTOS).build();

        Response<?> response = keyValueFeignApi.batchAddCommentContent(batchAddCommentContentReqDTO);
        if (!response.isSuccess()) {
            throw new RuntimeException("批量保存评论内容失败");
        }
        return true;
    }

    public List<FindCommentContentRspDTO> batchFindCommentContent(Long noteId, List<FindCommentContentReqDTO> findCommentContentReqDTOS) {
        BatchFindCommentContentReqDTO batchFindCommentContentReqDTO = BatchFindCommentContentReqDTO.builder()
                .noteId(noteId)
                .commentContentKeys(findCommentContentReqDTOS)
                .build();

        Response<List<FindCommentContentRspDTO>> response = keyValueFeignApi.batchFindCommentContent(batchFindCommentContentReqDTO);
        if (!response.isSuccess() || CollUtil.isEmpty(response.getData())) {
            return null;
        }
        return response.getData();
    }

    public boolean deleteCommentContent(CommentDO commentDO) {
        Long noteId = commentDO.getNoteId();
        LocalDateTime createTime = commentDO.getCreateTime();
        String contentUuid = commentDO.getContentUuid();

        DeleteCommentContentReqDTO deleteCommentContentReqDTO = DeleteCommentContentReqDTO.builder()
                .noteId(noteId)
                .yearMonth(createTime.format(DateConstants.DATE_FORMAT_Y_M))
                .commentId(contentUuid)
                .build();

        Response<?> response = keyValueFeignApi.deleteCommentContent(deleteCommentContentReqDTO);

        if (Objects.isNull(response) || !response.isSuccess()) {
            return false;
        }

        return true;
    }
}
