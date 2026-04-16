package com.gdut.domain.entity.common;

import com.gdut.common.enums.ResultCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "统一响应结果")
public class Result<T> {
    @Schema(description = "响应码", example = "200")
    private Integer code;
    @Schema(description = "响应消息", example = "操作成功")
    private String msg;
    @Schema(description = "响应数据")
    private T data;

    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> success(String msg, T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), msg, data);
    }

    public static <T> Result<T> error(String msg) {
        return new Result<>(ResultCode.INTERNAL_SERVER_ERROR.getCode(), msg, null);
    }

    public static <T> Result<T> error(Integer code, String msg) {
        return new Result<>(code, msg, null);
    }
    
    /**
     * 使用预定义的错误码
     */
    public static <T> Result<T> error(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }
    
    /**
     * 使用预定义的错误码 + 自定义消息
     */
    public static <T> Result<T> error(ResultCode resultCode, String customMsg) {
        return new Result<>(resultCode.getCode(), customMsg, null);
    }
}