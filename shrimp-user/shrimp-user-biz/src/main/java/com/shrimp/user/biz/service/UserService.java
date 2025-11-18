package com.shrimp.user.biz.service;

import com.shrimp.framework.common.response.Response;
import com.shrimp.user.biz.model.vo.UpdateUserInfoReqVO;
import com.shrimp.user.dto.req.FindUserByIdReqDTO;
import com.shrimp.user.dto.req.FindUserByPhoneReqDTO;
import com.shrimp.user.dto.req.RegisterUserReqDTO;
import com.shrimp.user.dto.req.UpdateUserPasswordReqDTO;
import com.shrimp.user.dto.resp.FindUserByIdRspDTO;
import com.shrimp.user.dto.resp.FindUserByPhoneRspDTO;

public interface UserService {
    Response<?> updateUserInfo(UpdateUserInfoReqVO updateUserInfoReqVO);
    Response<Long> registerUser(RegisterUserReqDTO registerUserReqDTO);

    Response<FindUserByPhoneRspDTO> findUserByPhone(FindUserByPhoneReqDTO findUserByPhoneReqDTO);
    Response<?> updatePassword(UpdateUserPasswordReqDTO updateUserPasswordReqDTO);

    Response<FindUserByIdRspDTO> findUserById(FindUserByIdReqDTO findUserByIdReqDTO);
}
