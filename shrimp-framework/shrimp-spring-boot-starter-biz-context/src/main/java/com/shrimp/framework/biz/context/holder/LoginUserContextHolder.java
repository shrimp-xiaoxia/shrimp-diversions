package com.shrimp.framework.biz.context.holder;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.ttl.TransmittableThreadLocal;
import com.shrimp.framework.common.constants.GlobalConstants;
import java.util.HashMap;
import java.util.Map;

public class LoginUserContextHolder {
    private static final ThreadLocal<Map<String,Object>> LOGIN_USER_CONTEXT_THREAD_LOCAL = TransmittableThreadLocal.withInitial(HashMap::new);

    public static void setLoginUserId(Long userId){
        LOGIN_USER_CONTEXT_THREAD_LOCAL.get().put(GlobalConstants.USER_ID,userId);
    }

    public static Long getLoginUserId(){
        Object obj = LOGIN_USER_CONTEXT_THREAD_LOCAL.get().get(GlobalConstants.USER_ID);
        if(ObjectUtil.isNull(obj)){
            return null;
        }
        return Long.parseLong(obj.toString());
    }

    public static void removeLoginUserContext(){
        LOGIN_USER_CONTEXT_THREAD_LOCAL.remove();
    }
}
