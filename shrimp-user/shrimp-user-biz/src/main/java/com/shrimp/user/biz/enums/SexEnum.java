package com.shrimp.user.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

@AllArgsConstructor
@Getter
public enum SexEnum {
    WOMAN(0),
    MAN(1),
    ;
    private final Integer value;

    public static boolean isValid(Integer value){
        for (SexEnum sexEnum : SexEnum.values()) {
            if(Objects.equals(sexEnum.value, value)){
                return true;
            }
        }
        return false;
    }

}
