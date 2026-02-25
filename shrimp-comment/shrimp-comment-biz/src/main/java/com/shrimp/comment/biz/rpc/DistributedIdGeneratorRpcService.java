package com.shrimp.comment.biz.rpc;

import com.shrimp.distrituted.id.generator.api.DistributedIdGeneratorFeignApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * @author ccb
 */
@Component
public class DistributedIdGeneratorRpcService{
    @Resource
    private DistributedIdGeneratorFeignApi distributedIdGeneratorFeignApi;

    public String getCommentId(){
        return distributedIdGeneratorFeignApi.getSegmentId("leaf-segment-comment-id");
    }
}