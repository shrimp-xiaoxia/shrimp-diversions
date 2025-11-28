package com.shrimp.note.biz.rpc;

import com.shrimp.framework.common.response.Response;
import com.shrimp.user.api.UserFeignApi;
import com.shrimp.user.dto.req.FindUserByIdReqDTO;
import com.shrimp.user.dto.resp.FindUserByIdRspDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class UserRpcService {
    @Resource
    private UserFeignApi userFeignApi;

    public FindUserByIdRspDTO findUserById(Long userId) {
        FindUserByIdReqDTO findUserByIdReqDTO = new FindUserByIdReqDTO(userId);
        Response<FindUserByIdRspDTO> findUserByIdRspDTOResponse = userFeignApi.findUserById(findUserByIdReqDTO);
        if (Objects.isNull(findUserByIdRspDTOResponse) || !findUserByIdRspDTOResponse.isSuccess()) {
            return null;
        }


        return findUserByIdRspDTOResponse.getData();
    }
}
