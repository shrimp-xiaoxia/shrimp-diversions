package com.shrimp.user.relation.biz.controller;
import com.shrimp.framework.common.response.Response;
import com.shrimp.user.relation.biz.domain.mapper.FollowingDOMapper;
import com.shrimp.user.relation.dto.FollowStateQueryDTO;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
@RestController
public class FollowStateController {
    @Resource private FollowingDOMapper following;
    @PostMapping("/internal/relation/batch-following")
    public Response<List<Long>> find(@Validated @RequestBody FollowStateQueryDTO request) {
        return Response.success(following.selectFollowingIds(request.getUserId(), request.getIds()));
    }
}
