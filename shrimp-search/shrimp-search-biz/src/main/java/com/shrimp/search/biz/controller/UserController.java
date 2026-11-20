package com.shrimp.search.biz.controller;

import com.shrimp.framework.biz.operationlog.aspect.ApiOperationLog;
import com.shrimp.framework.common.response.PageResponse;
import com.shrimp.framework.common.response.Response;
import com.shrimp.search.biz.model.vo.SearchUserReqVO;
import com.shrimp.search.biz.model.vo.SearchUserRspVO;
import com.shrimp.search.biz.service.UserService;
import com.shrimp.search.dto.RebuildUserDocumentReqDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.shrimp.framework.biz.context.holder.LoginUserContextHolder;
import com.shrimp.user.relation.api.RelationFeignApi;
import com.shrimp.user.relation.dto.FollowStateQueryDTO;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * @author ccb
 */
@RestController
@RequestMapping("/search")
@Slf4j
public class UserController {
    @Resource
    private UserService userService;
    @Resource private RelationFeignApi relations;

    @PostMapping("/user")
    @ApiOperationLog(description = "搜索用户接口")
    public PageResponse<SearchUserRspVO> searchUser(@Validated @RequestBody SearchUserReqVO searchUserReqVO) {
        PageResponse<SearchUserRspVO> page = userService.searchUser(searchUserReqVO);
        if (page.getData() == null || page.getData().isEmpty()) return page;
        Long userId = LoginUserContextHolder.getLoginUserId();
        if (userId == null) {
            page.getData().forEach(item -> { item.setIsLiked(false); item.setFollowStateAvailable(true); });
            return page;
        }
        try {
            FollowStateQueryDTO query = new FollowStateQueryDTO(); query.setUserId(userId);
            query.setIds(page.getData().stream().map(SearchUserRspVO::getUserId).toList());
            Response<List<Long>> response = relations.findFollowing(query);
            if (response != null && response.isSuccess() && response.getData() != null) {
                Set<Long> followed = new HashSet<>(response.getData());
                page.getData().forEach(item -> { item.setIsLiked(followed.contains(item.getUserId())); item.setFollowStateAvailable(true); });
            }
        } catch (RuntimeException e) { log.warn("关注状态查询失败", e); }
        return page;
    }

    // ===================================== 对其他服务提供的接口 =====================================
    @PostMapping("/user/document/rebuild")
    @ApiOperationLog(description = "用户文档重建")
    public Response<Long> rebuildDocument(@Validated @RequestBody RebuildUserDocumentReqDTO rebuildUserDocumentReqDTO) {
        return userService.rebuildDocument(rebuildUserDocumentReqDTO);
    }
}
