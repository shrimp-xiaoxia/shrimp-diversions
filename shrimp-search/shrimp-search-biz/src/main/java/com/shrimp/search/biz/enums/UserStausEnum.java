package com.shrimp.search.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum UserStausEnum {
    ENABLE( 0),
    DISABLE( 1);
    ;
    private final Integer value;
}
