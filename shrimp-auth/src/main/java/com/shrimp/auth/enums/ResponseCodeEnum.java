package com.shrimp.auth.enums;

import com.shrimp.framework.common.exception.BaseExceptionInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseCodeEnum implements BaseExceptionInterface {
    // ----------- 通用异常状态码 -----------
    // ----------- 通用异常状态码 -----------
    SYSTEM_ERROR("AUTH-10000", "出错啦，后台小哥正在努力修复中..."),
    PARAM_NOT_VALID("AUTH-10001", "参数错误"),

        // ----------- 业务异常状态码 -----------
    VERIFICATION_CODE_SEND_FREQUENTLY("AUTH-20001","请求太频繁，请在3分钟后再尝试"),
    VERIFICATION_CODE_ERROR("AUTH-20002","验证码错误"),
    AUTHORIZATION_ERROR("AUTH-20003","未登录异常"),
    LOGIN_TYPE_ERROR("AUTH-20004","登录类型错误"),
    USER_NOT_FOUND("AUTH-20005","该用户不存在"),
    PHONE_OR_PASSWORD_ERROR("AUTH-20006","手机号或者密码错误"),
    LOGIN_FAIL("AUTH-20007", "登录失败"),
    VERIFICATION_CODE_USED("AUTH-20101", "验证码已使用，请在发送间隔结束后重新获取"),
    ;

    // 异常码
    private final String errorCode;
    // 错误信息
    private final String errorMessage;
}
