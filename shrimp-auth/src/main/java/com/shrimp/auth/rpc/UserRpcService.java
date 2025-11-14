package com.shrimp.auth.rpc;

import com.shrimp.framework.common.response.Response;
import com.shrimp.user.api.UserFeignApi;
import com.shrimp.user.dto.req.FindUserByPhoneReqDTO;
import com.shrimp.user.dto.req.RegisterUserReqDTO;
import com.shrimp.user.dto.req.UpdateUserPasswordReqDTO;
import com.shrimp.user.dto.resp.FindUserByPhoneRspDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class UserRpcService {
    @Resource
    private UserFeignApi userFeignApi;

    public Long register(String phone) {
        RegisterUserReqDTO registerUserReqDTO = RegisterUserReqDTO.builder()
                .phone(phone)
                .build();
        Response<Long> response = userFeignApi.register(registerUserReqDTO);
        if (!response.isSuccess()) {
            return null;
        }

        return response.getData();
    }

    public FindUserByPhoneRspDTO findUserByPhone(FindUserByPhoneReqDTO findUserByPhoneReqDTO){
        Response<FindUserByPhoneRspDTO> response = userFeignApi.findUserByPhone(findUserByPhoneReqDTO);
        if(!response.isSuccess()){
//            TODO 这样的提示信息不够友好？
            return null;
        }
        return response.getData();
    }

    public boolean updatePassword(String encodePassword){
        UpdateUserPasswordReqDTO updateUserPasswordReqDTO = new UpdateUserPasswordReqDTO(encodePassword);
        Response<?> response = userFeignApi.updatePassword(updateUserPasswordReqDTO);
        return response.isSuccess();
    }
}
