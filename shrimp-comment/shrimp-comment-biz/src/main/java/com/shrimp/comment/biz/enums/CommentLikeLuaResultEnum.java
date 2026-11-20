package com.shrimp.comment.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CommentLikeLuaResultEnum {
    NOT_EXISTS(-1L),
    COMMENT_LIKE(1L),
    COMMENT_NOT_LIKE(0L),
    ;
    private final Long code;

    /**
     * 根据code获取枚举类
     * @param code
     * @return
     */
    public static CommentLikeLuaResultEnum valueOf(Long code) {
        for (CommentLikeLuaResultEnum value : CommentLikeLuaResultEnum.values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
}
