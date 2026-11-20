package com.shrimp.user.relation.biz.service;

import com.shrimp.user.relation.biz.domain.mapper.FollowingDOMapper;
import com.shrimp.user.relation.biz.model.vo.FindFollowStateRspVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FollowStateQueryService {
    private final FollowingDOMapper following;

    public FindFollowStateRspVO find(Long viewerId, Long targetId) {
        if (viewerId == null) return new FindFollowStateRspVO(null, false);
        try {
            List<Long> matches = following.selectFollowingIds(viewerId, List.of(targetId));
            if (matches == null) return new FindFollowStateRspVO(null, false);
            return new FindFollowStateRspVO(matches.contains(targetId), true);
        } catch (RuntimeException e) {
            log.warn("关注状态查询不可用", e);
            return new FindFollowStateRspVO(null, false);
        }
    }
}
