package com.shrimp.user.api;

import com.shrimp.framework.common.response.Response;
import com.shrimp.user.Constants.ApiConstants;
import com.shrimp.user.dto.req.*;
import com.shrimp.user.dto.resp.FindUserByIdRspDTO;
import com.shrimp.user.dto.resp.FindUserByPhoneRspDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = ApiConstants.SERVICE_NAME)
public interface UserFeignApi {
    String PREFIX = "/user";

    @PostMapping(PREFIX + "/register")
    Response<Long> register(@RequestBody RegisterUserReqDTO registerUserReqDTO);

    @PostMapping(PREFIX + "/findByPhone")
    Response<FindUserByPhoneRspDTO> findUserByPhone(@RequestBody FindUserByPhoneReqDTO findUserByPhoneReqDTO);

    @PostMapping(PREFIX + "/password/update")
    Response<?> updatePassword(@RequestBody UpdateUserPasswordReqDTO updateUserPasswordReqDTO);

    @PostMapping(PREFIX + "/findById")
    Response<FindUserByIdRspDTO> findUserById(@RequestBody FindUserByIdReqDTO findUserByIdReqDTO);

    @PostMapping(PREFIX + "/findByIds")
    Response<List<FindUserByIdRspDTO>> findUserByIds(@RequestBody FindUserByIdsReqDTO findUserByIdsReqDTO);
}

