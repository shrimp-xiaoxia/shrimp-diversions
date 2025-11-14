package com.shrimp.user.biz.controller;

import com.shrimp.framework.biz.operationlog.aspect.ApiOperationLog;
import com.shrimp.framework.common.response.Response;
import com.shrimp.user.biz.model.vo.UpdateUserInfoReqVO;
import com.shrimp.user.biz.service.UserService;
import com.shrimp.user.dto.req.FindUserByPhoneReqDTO;
import com.shrimp.user.dto.req.RegisterUserReqDTO;
import com.shrimp.user.dto.req.UpdateUserPasswordReqDTO;
import com.shrimp.user.dto.resp.FindUserByPhoneRspDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author ccb
 */
@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {
    @Resource
    private UserService userService;

    @PostMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Response<?> updateUserInfo(@Validated UpdateUserInfoReqVO updateUserInfoReqVO) {
        return userService.updateUserInfo(updateUserInfoReqVO);
    }

    @PostMapping("/register")
    @ApiOperationLog(description = "用户注册接口")
    public Response<Long> registerUser(@Validated @RequestBody RegisterUserReqDTO registerUserReqDTO) {
        return userService.registerUser(registerUserReqDTO);
    }

    @PostMapping("/findByPhone")
    @ApiOperationLog(description = "通过手机号查询用户接口")
    public Response<FindUserByPhoneRspDTO> findUserByPhone(@Validated @RequestBody FindUserByPhoneReqDTO findUserByPhoneReqDTO){
        return userService.findUserByPhone(findUserByPhoneReqDTO);
    }

    @PostMapping("/password/update")
    @ApiOperationLog(description = "修改密码接口")
    public Response<?> updatePassword(@Validated @RequestBody UpdateUserPasswordReqDTO updateUserPasswordReqDTO){
        return userService.updatePassword(updateUserPasswordReqDTO);
    }
}
