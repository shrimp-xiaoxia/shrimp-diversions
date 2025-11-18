package com.shrimp.note.biz.rpc;

import cn.hutool.core.util.ObjUtil;
import com.shrimp.framework.common.response.Response;
import com.shrimp.kv.api.KeyValueFeignApi;
import com.shrimp.kv.dto.req.AddNoteContentReqDTO;
import com.shrimp.kv.dto.req.DeleteNoteContentReqDTO;
import com.shrimp.kv.dto.req.FindNoteContentReqDTO;
import com.shrimp.kv.dto.rsp.FindNoteContentRspDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class KeyValueRpcService {

    @Resource
    private KeyValueFeignApi keyValueFeignApi;

    public boolean addNoteContent(String content,String contentUuid){
        AddNoteContentReqDTO addNoteContentReqDTO = AddNoteContentReqDTO.builder()
                .uuid(contentUuid)
                .content(content)
                .build();
        Response<?> response = keyValueFeignApi.addNoteContent(addNoteContentReqDTO);
        return ObjUtil.isNotNull(response) && response.isSuccess();
    }

    public boolean deleteNoteContent(String contentUuid){
        DeleteNoteContentReqDTO deleteNoteContentReqDTO = DeleteNoteContentReqDTO.builder()
                .uuid(contentUuid)
                .build();

        Response<?> response = keyValueFeignApi.deleteNoteContent(deleteNoteContentReqDTO);
        return ObjUtil.isNotNull(response) && response.isSuccess();
    }

    public FindNoteContentRspDTO findNoteContent(String contentUuid){
        FindNoteContentReqDTO findNoteContentReqDTO = FindNoteContentReqDTO.builder()
                .uuid(contentUuid)
                .build();
        Response<FindNoteContentRspDTO> response = keyValueFeignApi.findNoteContent(findNoteContentReqDTO);
        if(!response.isSuccess()){
            return null;
        }
        return response.getData();
    }
}
