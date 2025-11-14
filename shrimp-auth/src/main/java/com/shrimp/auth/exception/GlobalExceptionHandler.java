package com.shrimp.auth.exception;

import com.shrimp.auth.enums.ResponseCodeEnum;
import com.shrimp.framework.common.exception.BizException;
import com.shrimp.framework.common.response.Response;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Optional;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler({BizException.class})
    @ResponseBody
    public Response<Object> handleBizException(HttpServletRequest request, BizException e) {
        log.warn("{} request fail, errorCode: {}, errorMessage: {}", request.getRequestURI(), e.getErrorCode(), e.getErrorMessage());
        return Response.fail(e);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class})
    @ResponseBody
    public Response<Object> handleMethodArgumentNotValidException(HttpServletRequest request, MethodArgumentNotValidException e) {
        String errorCode = ResponseCodeEnum.PARAM_NOT_VALID.getErrorCode();

        BindingResult bindingResult = e.getBindingResult();
        StringBuilder stringBuilder = new StringBuilder();
        Optional.ofNullable(bindingResult.getFieldErrors()).ifPresent(
                fieldErrors -> {
                    fieldErrors.forEach(error -> {
                        stringBuilder.append(error.getField())
                                .append(" ")
                                .append(error.getDefaultMessage())
                                .append("，当前值：")
                                .append(error.getRejectedValue())
                                .append(";");
                    });
                }
        );
        String errorMess = stringBuilder.toString();
        log.warn("{} request fail, errorCode: {}，errorMess: {}", request.getRequestURI(), errorCode, errorMess);
        return Response.fail(errorCode, errorMess);
    }

    @ExceptionHandler({IllegalArgumentException.class})
    @ResponseBody
    public Response<Object> handIllegalArgumentException(HttpServletRequest request, IllegalArgumentException e) {
        String errorCode = ResponseCodeEnum.PARAM_NOT_VALID.getErrorCode();
        String errorMessage = e.getMessage();
        log.warn("{} request error, errorCode: {}, errorMessage: {}", request.getRequestURI(), errorCode, errorMessage);
        return Response.fail(errorCode, errorMessage);
    }

    @ExceptionHandler({Exception.class})
    @ResponseBody
    public Response<Object> handleException(HttpServletRequest request, Exception e) {
        log.error("{} request error, ", request.getRequestURI(), e);
        return Response.fail(ResponseCodeEnum.SYSTEM_ERROR);
    }


}
