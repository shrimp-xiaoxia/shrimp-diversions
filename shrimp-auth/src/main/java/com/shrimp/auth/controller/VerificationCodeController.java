package com.shrimp.auth.controller;

import com.shrimp.auth.model.vo.verificationcode.SendVerificationCodeReqVO;
import com.shrimp.auth.service.VerificationCodeService;
import com.shrimp.framework.biz.operationlog.aspect.ApiOperationLog;
import com.shrimp.framework.common.response.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author ccb
 */
@RestController
@RequestMapping("/verification/code")
@Slf4j
public class VerificationCodeController {
    @Resource
    private VerificationCodeService verificationCodeService;

    @PostMapping("/send")
    @ApiOperationLog(description = "发送短信验证码")
    public Response<?> send(@RequestBody @Validated SendVerificationCodeReqVO vo){
        return verificationCodeService.generateVerificationCode(vo);
    }

}
