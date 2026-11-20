package com.shrimp.comment.biz.rpc;

import cn.hutool.core.collection.CollUtil;
import com.shrimp.framework.common.response.Response;
import com.shrimp.user.api.UserFeignApi;
import com.shrimp.user.dto.req.FindUserByIdsReqDTO;
import com.shrimp.user.dto.resp.FindUserByIdRspDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
        List<Long> distinctIds = userIds.stream().filter(Objects::nonNull).distinct().toList();
        List<FindUserByIdRspDTO> result = new ArrayList<>();
        for (int start = 0; start < distinctIds.size(); start += 10) {
            var request = FindUserByIdsReqDTO.builder()
                    .ids(distinctIds.subList(start, Math.min(start + 10, distinctIds.size())))
                    .build();
            Response<List<FindUserByIdRspDTO>> response = userFeignApi.findUserByIds(request);
            if (response == null || !response.isSuccess()) return null;
            if (CollUtil.isNotEmpty(response.getData())) result.addAll(response.getData());
        }
        return result.isEmpty() ? null : result;

    }
}
