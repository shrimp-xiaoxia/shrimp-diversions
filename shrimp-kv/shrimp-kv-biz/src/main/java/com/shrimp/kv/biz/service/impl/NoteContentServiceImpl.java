package com.shrimp.kv.biz.service.impl;

import com.shrimp.framework.common.exception.BizException;
import com.shrimp.framework.common.response.Response;
import com.shrimp.kv.biz.domain.dataobject.NoteContentDO;
import com.shrimp.kv.biz.domain.repository.NoteContentRepository;
import com.shrimp.kv.biz.enums.ResponseCodeEnum;
import com.shrimp.kv.biz.service.NoteContentService;
import com.shrimp.kv.dto.req.AddNoteContentReqDTO;
import com.shrimp.kv.dto.req.DeleteNoteContentReqDTO;
import com.shrimp.kv.dto.req.FindNoteContentReqDTO;
import com.shrimp.kv.dto.rsp.FindNoteContentRspDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * @author ccb
 */
@Service
@Slf4j
public class NoteContentServiceImpl implements NoteContentService {
    @Resource
    private NoteContentRepository noteContentRepository;

    @Override
    public Response<?> addNoteContent(AddNoteContentReqDTO addNoteContentReqDTO) {
        String uuid = addNoteContentReqDTO.getUuid();
        String content = addNoteContentReqDTO.getContent();
        NoteContentDO noteContentDO = NoteContentDO.builder()
                .id(UUID.fromString(uuid))
                .content(content)
                .build();

        noteContentRepository.save(noteContentDO);
        return Response.success();
    }

    @Override
    public Response<FindNoteContentRspDTO> findNoteContent(FindNoteContentReqDTO findNoteContentReqDTO) {
        String noteId = findNoteContentReqDTO.getUuid();
        UUID uuid = UUID.fromString(noteId);

        Optional<NoteContentDO> noteContentOptional = noteContentRepository.findById(uuid);
        if (noteContentOptional.isEmpty()) {
            throw new BizException(ResponseCodeEnum.NOTE_CONTENT_NOT_FOUND);
        }


        NoteContentDO noteContentDO = noteContentOptional.get();
        FindNoteContentRspDTO findNoteContentRspDTO = FindNoteContentRspDTO.builder()
                .uuid(String.valueOf(noteContentDO.getId()))
                .content(noteContentDO.getContent())
                .build();

        return Response.success(findNoteContentRspDTO);
    }

    @Override
    public Response<?> deleteNoteContent(DeleteNoteContentReqDTO deleteNoteContentReqDTO) {
        String uuid = deleteNoteContentReqDTO.getUuid();
        noteContentRepository.deleteById(UUID.fromString(uuid));
        return Response.success();
    }
}
