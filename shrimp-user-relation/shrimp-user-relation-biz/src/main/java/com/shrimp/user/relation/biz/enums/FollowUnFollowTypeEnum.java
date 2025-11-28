package com.shrimp.user.relation.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author ccb
 */

@AllArgsConstructor
@Getter
public enum FollowUnFollowTypeEnum {
    FOLLOW(1),
    UNFOLLOW(0),
    ;
    private final Integer code;
}
