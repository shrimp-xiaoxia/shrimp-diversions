package com.shrimp.user.biz.rpc;

import com.shrimp.distrituted.id.generator.api.DistributedIdGeneratorFeignApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class DistributedIdGeneratorRpcService {

    @Resource
    private DistributedIdGeneratorFeignApi distributedIdGeneratorFeignApi;

    public static final String BIZ_TAG_XIAOHASHU_ID = "leaf-segment-xiaohashu-id";

    public String getXiaohashuId(){
        return distributedIdGeneratorFeignApi.getSegmentId(BIZ_TAG_XIAOHASHU_ID);
    }

    public static final String BIZ_TAG_USER_ID  = "leaf-segment-user-id";

    public String getUserId(){
        return distributedIdGeneratorFeignApi.getSegmentId(BIZ_TAG_USER_ID);
    }
}
