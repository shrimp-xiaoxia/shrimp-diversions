package com.shrimp.count.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LikeUnlikeCommentTypeEnum {
    LIKE(1),
    UNLIKE(0),
    ;
    private final Integer code;
}
