package com.shrimp.count.biz.enums;

import com.shrimp.framework.common.exception.BaseExceptionInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public enum CountResponseCodeEnum implements BaseExceptionInterface {
    COUNT_QUERY_FAILED("COUNT-20100", "计数查询失败");
    private final String errorCode;
    private final String errorMessage;
}
