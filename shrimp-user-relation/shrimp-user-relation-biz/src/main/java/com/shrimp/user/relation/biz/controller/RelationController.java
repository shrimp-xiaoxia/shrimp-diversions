package com.shrimp.user.relation.biz.controller;

import cn.hutool.db.Page;
import com.shrimp.framework.biz.operationlog.aspect.ApiOperationLog;
import com.shrimp.framework.common.response.PageResponse;
import com.shrimp.framework.common.response.Response;
import com.shrimp.user.relation.biz.model.vo.*;
import com.shrimp.user.relation.biz.service.RelationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/relation")
@Slf4j
public class RelationController {

    @Resource
    private RelationService relationService;

    @PostMapping("/follow")
    @ApiOperationLog(description = "关注用户接口")
    public Response<?> follow(@Validated @RequestBody FollowUserReqVO followUserReqVO) {
        return relationService.followUser(followUserReqVO);
    }

    @PostMapping("/unfollow")
    @ApiOperationLog(description = "取关用户接口")
    public Response<?> unfollow(@Validated @RequestBody UnFollowUserReqVO unFollowUserReqVO) {
        return relationService.unfollowUser(unFollowUserReqVO);
    }

    @PostMapping("/following/list")
    @ApiOperationLog(description = "查询用户关注列表")
    public PageResponse<FindFollowingUserRspVO> findFollowingUser(@Validated @RequestBody FindFollowingListReqVO findFollowingListReqVO) {
        return relationService.findFollowingList(findFollowingListReqVO);
    }

    @PostMapping("/fans/list")
    @ApiOperationLog(description = "查询用户粉丝接口")
    public PageResponse<FindFansUserRspVO> findFansUser(@Validated @RequestBody FindFansListReqVO findFansListReqVO) {
        return relationService.findFansList(findFansListReqVO);
    }
}
