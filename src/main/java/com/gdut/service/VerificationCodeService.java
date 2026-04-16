package com.gdut.service;

public interface VerificationCodeService {
    /**
     * 发送验证码到指定邮箱
     * @param email 邮箱地址
     */
    void sendVerificationCode(String email);

    /**
     * 验证验证码是否正确
     * @param email 邮箱地址
     * @param code 用户输入的验证码
     * @return 验证码是否有效
     */
    boolean isValidCode(String email, String code);

    /**
     * 删除已使用的验证码
     * @param email 邮箱地址
     */
    void removeCode(String email);
}
