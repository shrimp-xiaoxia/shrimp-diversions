package com.shrimp.note.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author ccb
 */

@AllArgsConstructor
@Getter
public enum NoteLikeLuaResultEnum {
    NOT_EXISTS(-1L),
    NOTE_LIKED(1L),
    NOTE_NOT_LIKE(0L),
    ;
    private final Long code;

    public static NoteLikeLuaResultEnum valueOf(Long code) {
        for (NoteLikeLuaResultEnum noteLikeLuaResultEnum : NoteLikeLuaResultEnum.values()) {
            if (noteLikeLuaResultEnum.getCode().equals(code)) {
                return noteLikeLuaResultEnum;
            }
        }

        return null;
    }
}
