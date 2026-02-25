package com.shrimp.comment.biz.rpc;

import cn.hutool.core.collection.CollUtil;
import com.shrimp.framework.common.response.Response;
import com.shrimp.user.api.UserFeignApi;
import com.shrimp.user.dto.req.FindUserByIdsReqDTO;
import com.shrimp.user.dto.resp.FindUserByIdRspDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author ccb
 */
@Component
public class UserRpcService {
    @Resource
    private UserFeignApi userFeignApi;

    public List<FindUserByIdRspDTO> findUserByIds(List<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return null;
        }
        FindUserByIdsReqDTO findUserByIdsReqDTO = FindUserByIdsReqDTO.builder().ids(
                userIds.stream().distinct().collect(Collectors.toList())
        ).build();

        Response<List<FindUserByIdRspDTO>> response = userFeignApi.findUserByIds(findUserByIdsReqDTO);

        if (!response.isSuccess() || CollUtil.isEmpty(response.getData())) {
            return null;
        }
        return response.getData();

    }
}
