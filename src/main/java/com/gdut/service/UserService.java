package com.gdut.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gdut.domain.dto.user.LoginDTO;
import com.gdut.domain.dto.user.ResetPasswordDTO;
import com.gdut.domain.dto.user.UpdatePasswordDTO;
import com.gdut.domain.dto.user.UserDTO;
import com.gdut.domain.dto.user.VerificationCodeRegisterDTO;
import com.gdut.domain.entity.user.User;
import com.gdut.domain.vo.user.LoginVO;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;


public interface UserService extends IService<User> {
    /**
     * 用户登录
     */
    LoginVO login(LoginDTO loginDTO, HttpServletResponse response);

    /**
     * 修改密码
     */
    void updatePassword(Long userId, UpdatePasswordDTO updatePasswordDTO);

    /**
     * 根据账号(邮箱/手机号)查询用户
     */
    User getByAccount(String account);

    void updateUserInfo(Long userId, @Valid UserDTO userDTO);

    /**
     * 上传头像到OSS并更新用户信息
     */
    String uploadAvatar(Long userId, MultipartFile file);

    /**
     * 退出登录,清除认证Cookie
     */
    void logout(HttpServletResponse response);

    /**
     * 验证码登录
     * @param email 邮箱
     * @param verificationCode 验证码
     * @param response HTTP响应
     * @return 登录结果
     */
    LoginVO loginByVerificationCode(String email, String verificationCode, HttpServletResponse response);

    /**
     * 验证码注册
     * @param registerDTO 注册信息
     */
    void registerByVerificationCode(VerificationCodeRegisterDTO registerDTO);

    /**
     * 验证码重置密码
     * @param resetPasswordDTO 重置密码信息
     */
    void resetPasswordByVerificationCode(ResetPasswordDTO resetPasswordDTO);
}
