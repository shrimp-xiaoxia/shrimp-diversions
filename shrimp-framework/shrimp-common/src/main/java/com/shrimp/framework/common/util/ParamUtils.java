package com.shrimp.framework.common.util;

import java.util.regex.Pattern;

public class ParamUtils {

    // ============================== 校验昵称 ==============================
    // 定义昵称长度范围
    private static final int NICK_NAME_MIN_LENGTH = 2;
    private static final int NICK_NAME_MAX_LENGTH = 24;
    private static final String NICK_NAME_REGEX = "[!@#$%^&*(),.?\":{}|<>]";

    public static boolean checkNickName(String nickName) {
//        检查长度
        if(nickName.length() < NICK_NAME_MIN_LENGTH || nickName.length() > NICK_NAME_MAX_LENGTH) {
            return false;
        }
//        检查是否有特殊字符
        Pattern pattern = Pattern.compile(NICK_NAME_REGEX);
        return !pattern.matcher(nickName).find();
    }

    // ============================== 校验小哈书号 ==============================
    // 定义 ID 长度范围
    private static final int ID_MIN_LENGTH = 6;
    private static final int ID_MAX_LENGTH = 15;
    private static final String ID_REGEX = "^[a-zA-Z0-9_]+$";

    public static boolean checkXiaohashuId(String xiaohashuId) {
        if(xiaohashuId.length() < ID_MIN_LENGTH || xiaohashuId.length() > ID_MAX_LENGTH) {
            return false;
        }
        Pattern pattern = Pattern.compile(ID_REGEX);
        return pattern.matcher(xiaohashuId).matches();
    }

    /**
     * 字符串长度校验
     *
     * @param str
     * @param length
     * @return
     */

    public static boolean checkStrLength(String str,int length) {
        if(str == null || str.length() > length){
            return false;
        }
        return true;
    }
}
