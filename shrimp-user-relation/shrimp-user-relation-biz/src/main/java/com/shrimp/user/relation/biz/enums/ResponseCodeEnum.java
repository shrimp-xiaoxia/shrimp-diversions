package com.shrimp.user.relation.biz.enums;

import com.shrimp.framework.common.exception.BaseExceptionInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author ccb
 */
@Getter
@AllArgsConstructor
public enum ResponseCodeEnum implements BaseExceptionInterface {
    //    通用错误类型
    SYSTEM_ERROR("NOTE-20001", "后台小哥正在全力抢修"),
    PARAM_VALID_FAIL("NOTE-20002", "参数不合法"),
    //            业务错误类型
// ----------- 业务异常状态码 -----------
    CANT_FOLLOW_YOUR_SELF("RELATION-20005", "无法关注自己"),
    FOLLOW_USER_NOT_EXISTED("RELATION-20006", "关注的用户不存在"),
    FOLLOWING_COUNT_LIMIT("RELATION-20007", "您关注的用户已达上限，请先取关部分用户"),
    ALREADY_FOLLOWED("RELATION-20008", "您已经关注了该用户"),
    CANT_UNFOLLOW_YOUR_SELF("RELATION-20009","无法取关自己"),
    UNFOLLOW_USER_NOT_EXISTED("RELATION-20010","取关的用户不存在"),
    USER_NOT_FOLLOW_FAIL("RELATION-20011","没有关注该用户,无法取关")
    ;
    final String errorCode;
    final String errorMessage;
}
