package com.shrimp.user.relation.biz.service;

import com.shrimp.framework.common.response.Response;
import com.shrimp.user.relation.biz.model.vo.FollowUserReqVO;

/**
 * @author ccb
 */
public interface RelationService {
    Response<?> followUser(FollowUserReqVO followUserReqVO);
}
