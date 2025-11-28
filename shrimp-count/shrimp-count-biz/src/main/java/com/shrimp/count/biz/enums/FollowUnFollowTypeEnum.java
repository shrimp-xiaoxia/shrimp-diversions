package com.shrimp.count.biz.enums;

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

    public static FollowUnFollowTypeEnum valueOf(Integer code) {
        for (FollowUnFollowTypeEnum followUnFollowTypeEnum : FollowUnFollowTypeEnum.values()) {
            if (followUnFollowTypeEnum.getCode().equals(code)) {
                return followUnFollowTypeEnum;
            }
        }
        return null;
    }
}
