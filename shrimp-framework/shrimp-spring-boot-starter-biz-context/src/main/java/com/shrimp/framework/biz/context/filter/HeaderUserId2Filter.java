package com.shrimp.framework.biz.context.filter;

import com.shrimp.framework.biz.context.holder.LoginUserContextHolder;
import com.shrimp.framework.common.constants.GlobalConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
public class HeaderUserId2Filter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        log.info("开始执行doFilterInternal过滤器方法");
        String userIdStr = request.getHeader(GlobalConstants.USER_ID);
        if(StringUtils.isBlank(userIdStr)){
            filterChain.doFilter(request,response);
            return;
        }

        long userId = Long.parseLong(userIdStr);
        log.info("该用户ID：{}",userId);
        try {
            LoginUserContextHolder.setLoginUserId(userId);
            filterChain.doFilter(request,response);
        } finally {
            LoginUserContextHolder.removeLoginUserContext();
        }
    }
}
