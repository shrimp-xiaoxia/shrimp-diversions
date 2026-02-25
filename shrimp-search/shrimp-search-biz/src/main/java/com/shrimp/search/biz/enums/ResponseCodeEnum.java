package com.shrimp.search.biz.enums;

import com.shrimp.framework.common.exception.BaseExceptionInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author ccb
 */
@Getter
@AllArgsConstructor
public enum ResponseCodeEnum implements BaseExceptionInterface {
    // ----------- 通用异常状态码 -----------
    SYSTEM_ERROR("SEARCH-10000", "出错啦，后台小哥正在努力修复中..."),
    PARAM_VALID_FAIL("SEARCH-10001", "参数错误"),
    //
    SORT_TYPE_FAIL("SEARCH_10002", "指定排序类型错误"),
    PUBLISH_TIME_TYPE_FAIL("SEARCH_10003", "指定时间范围类型错误"),
    ;

    final String errorCode;
    final String errorMessage;
}
