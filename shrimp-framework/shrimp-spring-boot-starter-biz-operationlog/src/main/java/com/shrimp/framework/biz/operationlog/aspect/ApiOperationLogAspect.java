package com.shrimp.framework.biz.operationlog.aspect;

import com.shrimp.framework.common.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;


@Aspect
@Slf4j
public class ApiOperationLogAspect {
    @Pointcut("@annotation(com.shrimp.framework.biz.operationlog.aspect.ApiOperationLog)")
    public void apiOperationLogPointCut() {}

    @Around("apiOperationLogPointCut()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        Object[] args = joinPoint.getArgs();
        String argsJsonStr = Arrays.stream(args).map(toJsonStr()).collect(Collectors.joining(","));

        String description = getAnnotationDescription(joinPoint);

        // 打印请求相关参数
        log.info("====== 请求开始: [{}], 入参: {}, 请求类: {}, 请求方法: {} =================================== ",
                description, argsJsonStr, className, methodName);
        // 方法执行的结果
        Object result = joinPoint.proceed();

        long executionTime = System.currentTimeMillis() - start;

        // 打印出参等相关信息
        log.info("====== 请求结束: [{}], 耗时: {}ms, 出参: {} =================================== ",
                description, executionTime, JsonUtils.toJsonString(result));
        return result;
    }


    private String getAnnotationDescription(ProceedingJoinPoint joinPoint) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();

        ApiOperationLog apiOperationLog = method.getAnnotation(ApiOperationLog.class);
        return apiOperationLog.description();
    }

    private Function<Object,String> toJsonStr(){
//        等同于 obj->sonUtils.toJsonString(obj);
        return JsonUtils::toJsonString;
    }
}
