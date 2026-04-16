package com.gdut.common.exception;

import com.gdut.common.enums.ResultCode;
import lombok.Getter;

/**
 * 业务异常类
 * 用于封装业务逻辑中的异常信息，配合 ResultCode 枚举使用
 */
@Getter
public class BusinessException extends RuntimeException {
    
    private final ResultCode resultCode;
    
    /**
     * 使用预定义的错误码和默认消息
     */
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
    }
    
    /**
     * 使用预定义的错误码 + 自定义消息
     */
    public BusinessException(ResultCode resultCode, String customMessage) {
        super(customMessage);
        this.resultCode = resultCode;
    }
    
    /**
     * 使用预定义的错误码 + 原始异常
     */
    public BusinessException(ResultCode resultCode, Throwable cause) {
        super(resultCode.getMessage(), cause);
        this.resultCode = resultCode;
    }
    
    /**
     * 使用预定义的错误码 + 自定义消息 + 原始异常
     */
    public BusinessException(ResultCode resultCode, String customMessage, Throwable cause) {
        super(customMessage, cause);
        this.resultCode = resultCode;
    }
}
