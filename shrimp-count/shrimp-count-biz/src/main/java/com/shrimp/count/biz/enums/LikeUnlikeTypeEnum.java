package com.shrimp.count.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author ccb
 */

@AllArgsConstructor
@Getter
public enum LikeUnlikeTypeEnum {
    LIKE(1),
    UNLIKE(0),
    ;
    private final Integer code;

    public static LikeUnlikeTypeEnum valueOf(Integer code) {
        for (LikeUnlikeTypeEnum type : LikeUnlikeTypeEnum.values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
