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
public enum LuaFollowResultEnum {
    // ZSET 不存在
    ZSET_NOT_EXIST(-1L),
    // 关注已达到上限
    FOLLOW_LIMIT(-2L),
    // 已经关注了该用户
    ALREADY_FOLLOWED(-3L),
    // 关注成功
    FOLLOW_SUCCESS(0L),
    ;

    private final Long code;

    /**
     * 根据类型 code 获取对应的枚举
     *
     * @param code
     * @return
     */
    public static LuaFollowResultEnum valueOf(Long code) {
        for (LuaFollowResultEnum luaFollowResultEnum : LuaFollowResultEnum.values()) {
            if (Objects.equals(code, luaFollowResultEnum.getCode())) {
                return luaFollowResultEnum;
            }
        }
        return null;
    }
}

