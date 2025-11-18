package com.shrimp.kv.api;

import com.shrimp.framework.common.response.Response;
import com.shrimp.kv.constants.ApiConstants;
import com.shrimp.kv.dto.req.AddNoteContentReqDTO;
import com.shrimp.kv.dto.req.DeleteNoteContentReqDTO;
import com.shrimp.kv.dto.req.FindNoteContentReqDTO;
import com.shrimp.kv.dto.rsp.FindNoteContentRspDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author ccb
 */
@FeignClient(name = ApiConstants.SERVICE_NAME)
public interface KeyValueFeignApi {
    String PREFIX = "/kv";

    @PostMapping(PREFIX + "/note/content/add")
    Response<?> addNoteContent(@RequestBody AddNoteContentReqDTO addNoteContentReqDTO);

    @PostMapping(PREFIX + "/note/content/find")
    Response<FindNoteContentRspDTO> findNoteContent(@RequestBody FindNoteContentReqDTO findNoteContentReqDTO);

    @PostMapping(PREFIX + "/note/content/delete")
    public Response<?> deleteNoteContent(@RequestBody DeleteNoteContentReqDTO deleteNoteContentReqDTO);
}
