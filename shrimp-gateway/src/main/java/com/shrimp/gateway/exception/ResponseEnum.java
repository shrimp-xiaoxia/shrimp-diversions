package com.shrimp.gateway.exception;

import com.shrimp.framework.common.exception.BaseExceptionInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ResponseEnum implements BaseExceptionInterface {
    SYSTEM_ERROR("500", "系统繁忙，请稍后再试"),
    UNAUTHORIZED("401", "权限不足"),
    ;
    private final String errorCode;
    private final String errorMessage;
}
