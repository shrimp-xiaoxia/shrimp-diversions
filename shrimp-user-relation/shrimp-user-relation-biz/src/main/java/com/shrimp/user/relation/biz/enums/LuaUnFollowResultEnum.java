package com.shrimp.user.relation.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

/**
 * @author: 犬小哈
 * @url: www.quanxiaoha.com
 * @date: 2023-08-15 10:33
 * @description: 执行 Lua 脚本返回结果
 **/
@Getter
@AllArgsConstructor
public enum LuaUnFollowResultEnum {
    // ZSET 不存在
    ZSET_NOT_EXIST(-1L),
    // 该取关用户不存在
    USER_NOT_EXISTED(-2L),
    // 取关成功
    FOLLOW_SUCCESS(0L),
    ;

    private final Long code;

    /**
     * 根据类型 code 获取对应的枚举
     *
     * @param code
     * @return
     */
    public static LuaUnFollowResultEnum valueOf(Long code) {
        for (LuaUnFollowResultEnum luaUnFollowResultEnum : LuaUnFollowResultEnum.values()) {
            if (Objects.equals(code, luaUnFollowResultEnum.getCode())) {
                return luaUnFollowResultEnum;
            }
        }
        return null;
    }
}

