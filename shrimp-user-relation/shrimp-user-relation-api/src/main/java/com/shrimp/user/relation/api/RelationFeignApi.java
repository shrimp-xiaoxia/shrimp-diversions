package com.shrimp.user.relation.api;
import com.shrimp.framework.common.response.Response;
import com.shrimp.user.relation.dto.FollowStateQueryDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.List;
@FeignClient(name = "shrimp-user-relation-biz")
public interface RelationFeignApi {
    @PostMapping("/internal/relation/batch-following")
    Response<List<Long>> findFollowing(@RequestBody FollowStateQueryDTO request);
}
