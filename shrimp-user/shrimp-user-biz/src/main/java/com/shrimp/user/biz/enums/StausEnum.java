package com.shrimp.user.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum StausEnum {
    ENABLE( 0),
    DISABLE( 1);
    ;
    private final Integer value;
}
