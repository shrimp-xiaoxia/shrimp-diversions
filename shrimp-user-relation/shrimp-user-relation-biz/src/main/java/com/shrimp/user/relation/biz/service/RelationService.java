package com.shrimp.user.relation.biz.service;

import com.shrimp.framework.common.response.PageResponse;
import com.shrimp.framework.common.response.Response;
import com.shrimp.user.relation.biz.model.vo.*;

/**
 * @author ccb
 */
public interface RelationService {
    Response<?> followUser(FollowUserReqVO followUserReqVO);

    Response<?> unfollowUser(UnFollowUserReqVO unFollowUserReqVO);

    PageResponse<FindFollowingUserRspVO> findFollowingList(FindFollowingListReqVO findFollowingListReqVO);

    PageResponse<FindFansUserRspVO> findFansList(FindFansListReqVO findFansListReqVO);
}
