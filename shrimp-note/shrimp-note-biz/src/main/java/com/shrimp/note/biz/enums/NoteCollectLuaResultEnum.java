package com.shrimp.note.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author ccb
 */

@AllArgsConstructor
@Getter
public enum NoteCollectLuaResultEnum {
    NOT_EXISTS(-1L),
    NOTE_COLLECT(1L),
    NOTE_NOT_COLLECT(0L),
    ;
    private final Long code;

    public static NoteCollectLuaResultEnum valueOf(Long code) {
        for (NoteCollectLuaResultEnum noteCollectLuaResultEnum : NoteCollectLuaResultEnum.values()) {
            if (noteCollectLuaResultEnum.getCode().equals(code)) {
                return noteCollectLuaResultEnum;
            }
        }
        return null;
    }
}
