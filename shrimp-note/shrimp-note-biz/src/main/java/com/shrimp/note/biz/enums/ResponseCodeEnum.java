package com.shrimp.note.biz.enums;

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
    SYSTEM_ERROR("NOTE-20001","后台小哥正在全力抢修"),
    PARAM_VALID_FAIL("NOTE-20002","参数不合法"),
//            业务错误类型
    SAVE_NOTE_CONTENT_FAIL("NOTE-20005","保存笔记内容失败"),
    NOTE_NOT_EXITS("NOTE-20006","该笔记不存在"),
    NOTE_DELETE("NOTE-20007","该笔记已经被删除"),
    NOTE_PRIVATE("note-20008","该笔记不公开"),
    CREATOR_FIND_FAIL("NOTE-20009","该笔记的创造者不存在"),
    NOTE_CONTENT_FIND_FAIL("20010","查询笔记内容发生异常错误"),
    NOTE_TAKE_OFF("note-20011","该笔记已经下架"),
    NOTE_STATUS_FAIL("note-200012","该笔记状态错误"),
    NOTE_UPDATE_PERMISSION_FAIL("note-20013","你没有更新此笔记的权限"),
    NOTE_TOPIC_FIND_FAIL("note-20014","更新主题异常"),
    NOTE_CANT_VISIBLE_ONLY_ME("NOTE-20015", "此笔记无法修改为仅自己可见"),
    NOTE_UPDATE_IS_TOP_FAIL("NOTE-20016", "此笔记更新置顶情况失败"),
    NOTE_CANT_FAIL("NOTE-10017","你没有该权限"),
    NOTE_ALREADY_LIKED("NOTE-10018","你已经点赞过这篇笔记"),
    NOTE_UNLIKE("NOTE-10019","你没有点赞过这篇笔记"),
    ;
    final String errorCode;
    final String errorMessage;
}
