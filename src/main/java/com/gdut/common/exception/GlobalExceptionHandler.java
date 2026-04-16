package com.gdut.common.exception;

import com.gdut.common.enums.ResultCode;
import com.gdut.domain.entity.common.Result;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.error("业务异常：{}", e.getMessage());
        return Result.error(e.getResultCode(), e.getMessage());
    }

    /**
     * 处理其他运行时异常（兜底）
     */
    @ExceptionHandler(RuntimeException.class)
    public Result<Void> handleRuntimeException(RuntimeException e) {
        log.error("系统异常：{}", e.getMessage(), e);
        return Result.error(ResultCode.INTERNAL_SERVER_ERROR, "系统异常，请稍后重试");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldError().getDefaultMessage();
        log.error("参数校验异常：{}", msg);
        return Result.error(ResultCode.BAD_REQUEST, msg);
    }

    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String msg = e.getBindingResult().getFieldError().getDefaultMessage();
        log.error("参数绑定异常：{}", msg);
        return Result.error(ResultCode.BAD_REQUEST, msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException e) {
        String msg = e.getMessage().split(":")[1].trim();
        log.error("参数校验异常：{}", msg);
        return Result.error(ResultCode.BAD_REQUEST, msg);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> handleDuplicateKeyException(DuplicateKeyException e) {
        log.error("数据冲突异常：{}", e.getMessage());
        return Result.error(ResultCode.BAD_REQUEST, "操作过于频繁，请稍后重试");
    }
}
