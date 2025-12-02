package com.shrimp.note.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author ccb
 */

@AllArgsConstructor
@Getter
public enum NoteUnlikeLuaResultEnum {
    NOT_EXISTS(-1L),
    NOTE_LIKED(1L),
    NOTE_NOT_LIKE(0L),
    ;
    private final Long code;

    public static NoteUnlikeLuaResultEnum valueOf(Long code) {
        for (NoteUnlikeLuaResultEnum noteLikeLuaResultEnum : NoteUnlikeLuaResultEnum.values()) {
            if (noteLikeLuaResultEnum.getCode().equals(code)) {
                return noteLikeLuaResultEnum;
            }
        }

        return null;
    }
}
