package com.shrimp.note.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

/**
 * @author: 犬小哈
 * @url: www.quanxiaoha.com
 * @date: 2023-08-15 10:33
 * @description: 笔记可见性
 **/
@Getter
@AllArgsConstructor
public enum NoteVisibleEnum {
    // 公开，所有人可见
    PUBLIC(0),
    // 仅自己可见
    PRIVATE(1);

    private final Integer code;

    public static NoteVisibleEnum valueOf(Integer code) {
        for (NoteVisibleEnum noteVisibleEnum : NoteVisibleEnum.values()) {
            if (Objects.equals(code, noteVisibleEnum.getCode())) {
                return noteVisibleEnum;
            }
        }
        return null;
    }
}

