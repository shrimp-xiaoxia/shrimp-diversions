package com.shrimp.auth.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum LoginTypeEnum {
    VERIFICATION_CODE(1),
    PASSWORD(0),

    ;
    private final Integer value;

    public static LoginTypeEnum valueOf(Integer value) {
        for(LoginTypeEnum loginTypeEnum : LoginTypeEnum.values()){
            if(loginTypeEnum.getValue().equals(value)){
                return loginTypeEnum;
            }
        }
        return null;
    }
}
