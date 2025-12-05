package com.shrimp.note.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author ccb
 */

@AllArgsConstructor
@Getter
public enum NoteUnCollectLuaResultEnum {
    NOT_EXISTS(-1L),
    NOTE_COLLECT(1L),
    NOTE_NOT_COLLECT(0L),
    ;
    private final Long code;

    public static NoteUnCollectLuaResultEnum valueOf(Long code) {
        for (NoteUnCollectLuaResultEnum type : NoteUnCollectLuaResultEnum.values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
