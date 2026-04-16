package com.gdut.aspect;

import com.gdut.common.util.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Aspect
@Component
public class LogAspect {

    @Around("execution(* com.gdut.controller..*(..))")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        Long userId = UserContext.getUserId();

        log.info(">>> 开始执行: {}.{} | 用户: {} | 参数: {}",
                className, methodName, userId, Arrays.toString(args));

        long startTime = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long endTime = System.currentTimeMillis();
            log.info("<<< 执行成功: {}.{} | 耗时: {}ms", className, methodName, (endTime - startTime));
            return result;
        } catch (Throwable e) {
            long endTime = System.currentTimeMillis();
            log.error("<<< 执行异常: {}.{} | 耗时: {}ms | 异常: {}",
                    className, methodName, (endTime - startTime), e.getMessage());
            throw e;
        }
    }
}
