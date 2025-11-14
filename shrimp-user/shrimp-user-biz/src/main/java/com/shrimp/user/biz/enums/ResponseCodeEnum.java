package com.shrimp.user.biz.enums;

import com.shrimp.framework.common.exception.BaseExceptionInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ResponseCodeEnum implements BaseExceptionInterface {
    SYSTEM_ERROR("USER-20001","后台小哥正在努力修复"),
//    业务性错误
    PARAM_VALID_FAIL("USER-20005","参数非法"),
    NICK_NAME_VALID_FAIL("USER-20006","昵称请设置2-24个字符，不能使用@《/等特殊字符"),
    XIAOHASHU_ID_VALID_FAIL("USER-20007","小哈书号请设置6-15个字符，仅可使用英文（必须）、数字、下划线"),
    SEX_VALID_FAIL("USER-20008","性别错误"),
    INTRODUCE_VALID_FAIL("user-20009","个人简介请设置1-100个字符"),
    UPLOAD_AVATAR_FAIL("USER-20010", "头像上传失败"),
    UPLOAD_BACKGROUND_IMG_FAIL("USER-20011", "背景图上传失败"),
    LOGIN_STATUS_ERROR("USER-200012","没登录异常"),
    USER_NOT_FOUND("AUTH-200013","该用户不存在"),
    ;
    private final String errorCode;
    private final String errorMessage;
}
