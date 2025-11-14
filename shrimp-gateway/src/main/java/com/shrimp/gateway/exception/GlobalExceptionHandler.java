package com.shrimp.gateway.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.SaTokenException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shrimp.framework.common.response.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {
    @Resource
    private ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
//        获取响应对象
        ServerHttpResponse response = exchange.getResponse();
        log.error("==>全局异常捕捉", ex);
//        判断异常
        Response<?> result;
//        SaTokenException
        if (ex instanceof NotLoginException) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            result = Response.fail(ResponseEnum.UNAUTHORIZED.getErrorCode(),ex.getMessage());
        } else if (ex instanceof NotPermissionException) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            result = Response.fail(ResponseEnum.UNAUTHORIZED);
        } else {
//        其他异常
            result = Response.fail(ResponseEnum.SYSTEM_ERROR);
        }
//        设置响应头的内容类型为 application/json;charset=UTF-8，表示响应体为 JSON 格式
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON_UTF8);
//        设置body 响应体
        return response.writeWith(Mono.fromSupplier(() ->
                {
                    DataBufferFactory dataBufferFactory = response.bufferFactory();
                    try {
                        return dataBufferFactory.wrap(objectMapper.writeValueAsBytes(result));
                    } catch (JsonProcessingException e) {
                        // 如果转换过程中出现异常，则返回空字节数组
                        return dataBufferFactory.wrap(new byte[0]);
                    }
                }
        ));
    }
}
