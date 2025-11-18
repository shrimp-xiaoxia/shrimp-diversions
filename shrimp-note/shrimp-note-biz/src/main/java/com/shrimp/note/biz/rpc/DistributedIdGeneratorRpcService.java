package com.shrimp.note.biz.rpc;

import com.shrimp.distrituted.id.generator.api.DistributedIdGeneratorFeignApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class DistributedIdGeneratorRpcService {

    @Resource
    private DistributedIdGeneratorFeignApi distributedIdGeneratorFeignApi;

    public static final String BIZ_TAG_NOTE_ID = "leaf-segment-note-id";

    public String getNoteId() {
        return distributedIdGeneratorFeignApi.getSegmentId(BIZ_TAG_NOTE_ID);
    }

}
