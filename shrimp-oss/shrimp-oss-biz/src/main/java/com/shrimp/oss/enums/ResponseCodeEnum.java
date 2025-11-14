package com.shrimp.oss.enums;

import com.shrimp.framework.common.exception.BaseExceptionInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ResponseCodeEnum implements BaseExceptionInterface {
    SYSTEM_ERROR("oss-20001","后台小哥正在努力修复"),
//    业务性错误
    INVALID_PARAM("oss-20005","参数非法"),
    ;
    private final String errorCode;
    private final String errorMessage;
}
