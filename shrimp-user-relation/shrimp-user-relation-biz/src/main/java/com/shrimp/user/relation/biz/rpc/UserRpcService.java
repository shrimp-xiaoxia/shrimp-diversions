package com.shrimp.user.relation.biz.rpc;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import com.shrimp.framework.common.response.Response;
import com.shrimp.user.api.UserFeignApi;
import com.shrimp.user.dto.req.FindUserByIdReqDTO;
import com.shrimp.user.dto.req.FindUserByIdsReqDTO;
import com.shrimp.user.dto.resp.FindUserByIdRspDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * @author ccb
 */
@Service
@Slf4j
public class UserRpcService {
    @Resource
    private UserFeignApi userFeignApi;

    public boolean checkUserIsExist(Long followUserId) {
        FindUserByIdReqDTO findUserByIdReqDTO = FindUserByIdReqDTO.builder()
                .id(followUserId).build();
        Response<FindUserByIdRspDTO> response = userFeignApi.findUserById(findUserByIdReqDTO);
        if (ObjUtil.isNull(response) || !response.isSuccess() || ObjUtil.isNull(response.getData())) {
            return false;
        }
        return true;
    }

    public List<FindUserByIdRspDTO> findUserByIds(List<Long> userIds) {
        FindUserByIdsReqDTO findUserByIdsReqDTO = FindUserByIdsReqDTO.builder()
                .ids(userIds)
                .build();

        Response<List<FindUserByIdRspDTO>> response = userFeignApi.findUserByIds(findUserByIdsReqDTO);
        if (ObjUtil.isNull(response) || !response.isSuccess() || CollUtil.isEmpty(response.getData())) {
            return null;
        }
        return response.getData();
    }
}

