package com.gdut.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一响应状态码枚举
 * 
 * 编码规则：
 * - 2xx: 成功
 * - 4xx: 客户端错误（参数错误、权限不足等）
 * - 5xx: 服务器错误
 * - 1xxx: 用户模块错误
 * - 2xxx: 笔记模块错误
 * - 3xxx: 好友模块错误
 * - 4xxx: 消息模块错误
 * - 5xxx: AI 聊天模块错误
 * - 6xxx: 文件夹模块错误
 */
@Getter
@AllArgsConstructor
public enum ResultCode {
    
    // ==================== 成功 (2xx) ====================
    SUCCESS(200, "操作成功"),
    
    // ==================== 客户端错误 (4xx) ====================
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "没有权限访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    
    // ==================== 服务器错误 (5xx) ====================
    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务暂时不可用"),
    
    // ==================== 用户模块错误 (1xxx) ====================
    USER_NOT_EXIST(1001, "用户不存在"),
    EMAIL_ALREADY_EXISTS(1002, "邮箱已被注册"),
    PHONE_ALREADY_EXISTS(1003, "手机号已被注册"),
    USERNAME_ALREADY_EXISTS(1004, "用户名已被使用"),
    VERIFICATION_CODE_EXPIRED(1005, "验证码已过期"),
    VERIFICATION_CODE_ERROR(1006, "验证码错误"),
    PASSWORD_ERROR(1007, "密码错误"),
    OLD_PASSWORD_ERROR(1008, "旧密码错误"),
    PASSWORD_NOT_MATCH(1009, "两次密码不一致"),
    PASSWORD_LENGTH_INVALID(1010, "密码长度需在8-20位之间"),
    ACCOUNT_NOT_EXIST(1011, "账号不存在"),
    AVATAR_UPLOAD_FAILED(1012, "头像上传失败"),
    FILE_TYPE_INVALID(1013, "文件格式不支持"),
    FILE_SIZE_EXCEEDED(1014, "文件大小超过限制"),
    
    // ==================== 笔记模块错误 (2xxx) ====================
    NOTE_NOT_EXIST(2001, "笔记不存在"),
    NOTE_NO_PERMISSION(2002, "没有权限操作此笔记"),
    NOTE_LOCKED(2003, "笔记正在被其他人编辑"),
    NOTE_VERSION_CONFLICT(2004, "版本冲突，请刷新后重试"),
    NOTE_IMPORT_FAILED(2005, "笔记导入失败"),
    NOTE_EXPORT_FAILED(2006, "笔记导出失败"),
    NOTE_ANALYSIS_FAILED(2007, "AI分析失败"),
    SHARE_CODE_INVALID(2008, "分享码无效或已过期"),
    ANNOTATION_NOT_EXIST(2009, "批注不存在"),
    ANNOTATION_NO_PERMISSION(2010, "没有权限操作此批注"),
    
    // ==================== 好友模块错误 (3xxx) ====================
    FRIEND_NOT_EXIST(3001, "好友不存在"),
    FRIEND_ALREADY_EXISTS(3002, "已是好友关系"),
    FRIEND_REQUEST_PENDING(3003, "好友申请待处理"),
    FRIEND_REQUEST_NOT_FOUND(3004, "好友申请不存在"),
    NOT_FRIENDS(3005, "不是好友关系"),
    FRIEND_GROUP_NOT_EXIST(3006, "好友分组不存在"),
    SELF_FRIEND_FORBIDDEN(3007, "不能添加自己为好友"),
    
    // ==================== 消息模块错误 (4xxx) ====================
    MESSAGE_NOT_EXIST(4001, "消息不存在"),
    GROUP_NOT_EXIST(4002, "群聊不存在"),
    NOT_GROUP_MEMBER(4003, "不是群聊成员"),
    GROUP_FULL(4004, "群聊人数已达上限"),
    MESSAGE_SEND_FAILED(4005, "消息发送失败"),
    PRIVATE_CHAT_NOT_EXIST(4006, "私聊会话不存在"),
    
    // ==================== AI 聊天模块错误 (5xxx) ====================
    AI_SERVICE_UNAVAILABLE(5001, "AI 服务暂时不可用"),
    AI_QUOTA_EXCEEDED(5002, "AI 使用次数已达上限"),
    AI_REQUEST_TIMEOUT(5003, "AI 请求超时"),
    AI_CONTENT_FILTERED(5004, "内容包含敏感信息，无法处理"),
    CONVERSATION_NOT_EXIST(5005, "对话不存在"),
    
    // ==================== 文件夹模块错误 (6xxx) ====================
    FOLDER_NOT_EXIST(6001, "文件夹不存在"),
    FOLDER_HAS_CHILDREN(6002, "文件夹下还有子文件夹或笔记"),
    FOLDER_NAME_DUPLICATE(6003, "同级目录下已存在同名文件夹"),
    FOLDER_MOVE_INVALID(6004, "不能将文件夹移动到其子文件夹下"),
    
    // ==================== 回收站模块错误 (7xxx) ====================
    RECYCLE_BIN_EMPTY(7001, "回收站为空"),
    RESTORE_FAILED(7002, "恢复失败，原文件夹可能已被删除");
    
    private final Integer code;
    private final String message;
}
