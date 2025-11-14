package com.shrimp.auth.service;

import com.shrimp.auth.model.vo.verificationcode.SendVerificationCodeReqVO;
import com.shrimp.framework.common.response.Response;

public interface VerificationCodeService {
    Response<?> generateVerificationCode(SendVerificationCodeReqVO phone);
}
