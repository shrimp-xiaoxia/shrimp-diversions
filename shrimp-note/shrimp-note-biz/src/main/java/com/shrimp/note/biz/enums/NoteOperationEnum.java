package com.shrimp.note.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author ccb
 */

@AllArgsConstructor
@Getter
public enum NoteOperationEnum {
    PUBLISH(1),
    DELETE(0),
    ;
    private final Integer code;

    public static NoteOperationEnum valueOf(Integer code) {
        for (NoteOperationEnum enumType : NoteOperationEnum.values()) {
            if (enumType.code.equals(code)) {
                return enumType;
            }
        }
        return null;
    }
}
