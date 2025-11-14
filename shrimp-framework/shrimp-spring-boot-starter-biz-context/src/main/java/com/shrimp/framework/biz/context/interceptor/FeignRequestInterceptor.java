package com.shrimp.framework.biz.context.interceptor;

import cn.hutool.core.util.ObjUtil;
import com.shrimp.framework.biz.context.holder.LoginUserContextHolder;
import com.shrimp.framework.common.constants.GlobalConstants;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FeignRequestInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate requestTemplate) {
        Long userId = LoginUserContextHolder.getLoginUserId();
        if(ObjUtil.isNotNull(userId)){
            requestTemplate.header(GlobalConstants.USER_ID,String.valueOf(userId));
            log.info("feign设置请求头");
        }
    }
}
