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
import com.shrimp.user.relation.biz.domain.mapper.FollowingDOMapper;
import com.shrimp.user.relation.biz.domain.mapper.FansDOMapper;
import com.shrimp.user.relation.biz.rpc.UserRpcService;
import com.shrimp.user.dto.resp.FindUserByIdRspDTO;
import com.shrimp.framework.common.exception.BizException;
import com.shrimp.user.relation.biz.enums.ResponseCodeEnum;
import com.shrimp.framework.biz.context.holder.LoginUserContextHolder;
import com.shrimp.count.api.CountFeignApi;
import com.shrimp.count.dto.CountQueryReqDTO;
import com.shrimp.count.dto.CountResultDTO;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/relation")
@Slf4j
public class RelationController {

    @Resource
    private RelationService relationService;
    @Resource private FollowingDOMapper following;
    @Resource private FansDOMapper fans;
    @Resource private UserRpcService users;
    @Resource private CountFeignApi counts;

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
        Long targetId = findFollowingListReqVO.getUserId();
        int pageNo = findFollowingListReqVO.getPageNo();
        long total = following.countAvailable(targetId);
        List<Long> pageIds = following.pageAvailable(targetId, PageResponse.getOffset(pageNo, 10));
        List<FindFollowingUserRspVO> items = pageIds.isEmpty() ? null : orderedUsers(pageIds).stream().map(user ->
                FindFollowingUserRspVO.builder().userId(user.getId()).nickname(user.getNickName())
                        .avatar(user.getAvatar()).introduction(user.getIntroduction()).build()).toList();
        PageResponse<FindFollowingUserRspVO> page = PageResponse.success(items, pageNo, total);
        if (page.getData() != null && !page.getData().isEmpty()) {
            Set<Long> ids = following(page.getData().stream().map(FindFollowingUserRspVO::getUserId).toList());
            page.getData().forEach(item -> { item.setIsLiked(ids == null ? null : ids.contains(item.getUserId())); item.setFollowStateAvailable(ids != null); });
        }
        return page;
    }

    @PostMapping("/fans/list")
    @ApiOperationLog(description = "查询用户粉丝接口")
    public PageResponse<FindFansUserRspVO> findFansUser(@Validated @RequestBody FindFansListReqVO findFansListReqVO) {
        Long targetId = findFansListReqVO.getUserId();
        int pageNo = findFansListReqVO.getPageNo();
        long total = fans.countAvailable(targetId);
        List<Long> pageIds = fans.pageAvailable(targetId, PageResponse.getOffset(pageNo, 10));
        List<FindFansUserRspVO> items = pageIds.isEmpty() ? null : orderedUsers(pageIds).stream().map(user ->
                FindFansUserRspVO.builder().userId(user.getId()).nickName(user.getNickName()).avatar(user.getAvatar()).build()).toList();
        PageResponse<FindFansUserRspVO> page = PageResponse.success(items, pageNo, total);
        if (page.getData() != null && !page.getData().isEmpty()) {
            List<Long> userIds = page.getData().stream().map(FindFansUserRspVO::getUserId).toList();
            Set<Long> ids = following(userIds);
            Map<Long, CountResultDTO> values = userCounts(userIds);
            page.getData().forEach(item -> {
                item.setIsLiked(ids == null ? null : ids.contains(item.getUserId())); item.setFollowStateAvailable(ids != null);
                CountResultDTO count = values.get(item.getUserId());
                item.setFansTotal(count == null ? null : count.getFansTotal()); item.setFansTotalAvailable(count != null && count.getFansTotal() != null);
                item.setNoteTotal(count == null ? null : count.getNoteTotal()); item.setNoteTotalAvailable(count != null && count.getNoteTotal() != null);
            });
        }
        return page;
    }

    private Set<Long> following(List<Long> ids) {
        Long viewer = LoginUserContextHolder.getLoginUserId();
        if (viewer == null) return Set.of();
        try { return new HashSet<>(following.selectFollowingIds(viewer, ids)); }
        catch (RuntimeException e) { log.warn("关注状态查询失败", e); return null; }
    }

    private List<FindUserByIdRspDTO> orderedUsers(List<Long> ids) {
        List<FindUserByIdRspDTO> found = users.findUserByIds(ids);
        if (found == null) throw new BizException(ResponseCodeEnum.SYSTEM_ERROR);
        Map<Long, FindUserByIdRspDTO> byId = found.stream().collect(Collectors.toMap(FindUserByIdRspDTO::getId, Function.identity(), (a,b) -> a));
        if (ids.stream().anyMatch(id -> !byId.containsKey(id))) throw new BizException(ResponseCodeEnum.SYSTEM_ERROR);
        return ids.stream().map(byId::get).toList();
    }

    private Map<Long, CountResultDTO> userCounts(List<Long> ids) {
        try {
            CountQueryReqDTO query = new CountQueryReqDTO(); query.setIds(ids);
            Response<List<CountResultDTO>> response = counts.findUsers(query);
            if (response != null && response.isSuccess() && response.getData() != null)
                return response.getData().stream().collect(Collectors.toMap(CountResultDTO::getId, Function.identity(), (a,b) -> a));
        } catch (RuntimeException e) { log.warn("粉丝计数查询失败", e); }
        return Collections.emptyMap();
    }
}
