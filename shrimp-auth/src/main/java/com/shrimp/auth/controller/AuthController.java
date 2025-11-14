package com.shrimp.auth.controller;

import com.shrimp.auth.model.vo.user.UpdatePasswordReqVO;
import com.shrimp.auth.model.vo.user.UserLoginReqVO;
import com.shrimp.auth.service.AuthService;
import com.shrimp.framework.biz.operationlog.aspect.ApiOperationLog;
import com.shrimp.framework.common.response.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @author ccb
 */
@RestController
@RequestMapping
@Slf4j
public class AuthController {

    @Resource
    private AuthService authService;

    @PostMapping("/login")
    @ApiOperationLog(description = "用户登录注册")
    public Response<String> loginAndRegister(@Validated @RequestBody UserLoginReqVO userLoginReqVO){
        return authService.loginAndRegister(userLoginReqVO);
    }

    @PostMapping("/logout")
    @ApiOperationLog(description = "账号登出")
    public Response<?> logout(){
//        TODO 账号登出逻辑
        authService.logout();
        return Response.success();
    }

    @PostMapping("/password/update")
    @ApiOperationLog(description = "修改密码")
    public Response<?> updatePassword(@Validated @RequestBody UpdatePasswordReqVO updatePasswordReqVO){
        return authService.updatePassword(updatePasswordReqVO);
    }
}
