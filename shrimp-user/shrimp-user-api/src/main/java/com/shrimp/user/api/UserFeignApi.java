package com.shrimp.user.api;

import com.shrimp.framework.common.response.Response;
import com.shrimp.user.Constants.ApiConstants;
import com.shrimp.user.dto.req.FindUserByPhoneReqDTO;
import com.shrimp.user.dto.req.RegisterUserReqDTO;
import com.shrimp.user.dto.req.UpdateUserPasswordReqDTO;
import com.shrimp.user.dto.resp.FindUserByPhoneRspDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = ApiConstants.SERVICE_NAME)
public interface UserFeignApi {
    String PREFIX = "/user";

    @PostMapping(PREFIX + "/register")
    Response<Long> register(@RequestBody RegisterUserReqDTO registerUserReqDTO);

    @PostMapping(PREFIX + "/findByPhone")
    Response<FindUserByPhoneRspDTO> findUserByPhone(@RequestBody FindUserByPhoneReqDTO findUserByPhoneReqDTO);

    @PostMapping(PREFIX + "/password/update")
    Response<?> updatePassword(@RequestBody UpdateUserPasswordReqDTO updateUserPasswordReqDTO);
}
