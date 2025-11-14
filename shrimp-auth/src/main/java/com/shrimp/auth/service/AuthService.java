package com.shrimp.auth.service;

import com.shrimp.auth.model.vo.user.UpdatePasswordReqVO;
import com.shrimp.auth.model.vo.user.UserLoginReqVO;
import com.shrimp.framework.common.response.Response;

public interface AuthService {
    Response<String> loginAndRegister(UserLoginReqVO userLoginReqVO);
//    Long registerUser(String phone);

    Response<?> logout();

    Response<?> updatePassword(UpdatePasswordReqVO  updatePasswordReqVO);
}
