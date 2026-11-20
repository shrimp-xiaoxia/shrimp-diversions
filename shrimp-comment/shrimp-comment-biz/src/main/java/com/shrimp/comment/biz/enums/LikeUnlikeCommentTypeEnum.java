package com.shrimp.comment.biz.enums;

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
