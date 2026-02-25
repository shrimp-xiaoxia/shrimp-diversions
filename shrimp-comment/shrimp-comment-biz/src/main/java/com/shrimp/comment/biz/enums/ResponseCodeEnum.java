package com.shrimp.comment.biz.enums;

import com.shrimp.framework.common.exception.BaseExceptionInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author: 犬小哈
 * @url: www.quanxiaoha.com
 * @date: 2023-08-15 10:33
 * @description: 响应异常码
 **/
@Getter
@AllArgsConstructor
public enum ResponseCodeEnum implements BaseExceptionInterface {

    // ----------- 通用异常状态码 -----------
    SYSTEM_ERROR("COMMENT-10000", "出错啦，后台小哥正在努力修复中..."),
    PARAM_VALID_FAIL("COMMENT-10001", "参数错误"),

    // ----------- 业务异常状态码 -----------
    RPC_KEY_VALUE_GET_COMMENT_CONTENT_FAIL("COMMENT-10010", "远程获取评论内容失败"),
    RPC_KEY_VALUE_GET_USER_FAIL("COMMENT-10011", "远程获取用户信息失败"),
    COMMENT_NOT_FOUND("COMMENT-20001", "此评论不存在"),
    ;

    // 异常码
    private final String errorCode;
    // 错误信息
    private final String errorMessage;

}

