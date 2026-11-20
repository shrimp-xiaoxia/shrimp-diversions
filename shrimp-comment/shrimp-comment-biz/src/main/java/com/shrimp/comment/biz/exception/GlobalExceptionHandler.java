package com.shrimp.comment.biz.exception;

import com.shrimp.comment.biz.enums.ResponseCodeEnum;
import com.shrimp.framework.common.exception.BizException;
import com.shrimp.framework.common.response.Response;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Comparator;
import java.util.stream.Collectors;

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
        String errorCode = ResponseCodeEnum.PARAM_VALID_FAIL.getErrorCode();

        BindingResult bindingResult = e.getBindingResult();
        String errorMess = bindingResult.getFieldErrors().stream()
                .sorted(Comparator.comparing(error -> error.getField()))
                .map(error -> error.getField() + " " + error.getDefaultMessage() + ";")
                .collect(Collectors.joining());
        log.warn("{} request fail, errorCode: {}，errorMess: {}", request.getRequestURI(), errorCode, errorMess);
        return Response.fail(errorCode, errorMess);
    }

    @ExceptionHandler({IllegalArgumentException.class})
    @ResponseBody
    public Response<Object> handIllegalArgumentException(HttpServletRequest request, IllegalArgumentException e) {
        String errorCode = ResponseCodeEnum.PARAM_VALID_FAIL.getErrorCode();
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
