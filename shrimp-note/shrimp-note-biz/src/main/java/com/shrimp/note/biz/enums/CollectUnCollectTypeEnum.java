package com.shrimp.note.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author ccb
 */

@AllArgsConstructor
@Getter
public enum CollectUnCollectTypeEnum {
    COLLECT(1),
    UNCOLLECTED(0),
    ;
    private final Integer code;

    public static CollectUnCollectTypeEnum valueOf(Integer code) {
        for (CollectUnCollectTypeEnum type : CollectUnCollectTypeEnum.values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
