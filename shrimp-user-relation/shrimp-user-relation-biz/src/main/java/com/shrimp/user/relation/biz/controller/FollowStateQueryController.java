package com.shrimp.user.relation.biz.controller;

import com.shrimp.framework.biz.context.holder.LoginUserContextHolder;
import com.shrimp.framework.common.response.Response;
import com.shrimp.user.relation.biz.model.vo.FindFollowStateReqVO;
import com.shrimp.user.relation.biz.model.vo.FindFollowStateRspVO;
import com.shrimp.user.relation.biz.service.FollowStateQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/relation")
@RequiredArgsConstructor
public class FollowStateQueryController {
    private final FollowStateQueryService service;

    @PostMapping("/find-follow-state")
    public Response<FindFollowStateRspVO> find(@Validated @RequestBody FindFollowStateReqVO request) {
        return Response.success(service.find(LoginUserContextHolder.getLoginUserId(), request.getUserId()));
    }
}
