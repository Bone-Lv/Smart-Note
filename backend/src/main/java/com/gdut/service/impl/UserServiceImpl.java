package com.gdut.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gdut.common.exception.BusinessException;
import com.gdut.common.enums.ResultCode;
import com.gdut.domain.dto.user.LoginDTO;
import com.gdut.domain.dto.user.ResetPasswordDTO;
import com.gdut.domain.dto.user.UpdatePasswordDTO;
import com.gdut.domain.dto.user.UserDTO;
import com.gdut.domain.dto.user.VerificationCodeRegisterDTO;
import com.gdut.domain.entity.user.User;
import com.gdut.domain.vo.user.LoginVO;
import com.gdut.domain.vo.user.UserVO;
import com.gdut.mapper.UserMapper;
import com.gdut.service.UserService;
import com.gdut.common.util.AliyunOSSOperator;
import com.gdut.common.util.JwtUtil;
import com.gdut.service.VerificationCodeService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final PasswordEncoder passwordEncoder;
    private final AliyunOSSOperator aliyunOSSOperator;
    private final VerificationCodeService verificationCodeService;

    @Value("${user.password.min-length}")
    private Integer minLength;
    @Value("${user.password.max-length}")
    private Integer maxLength;

    @Override
    public LoginVO login(LoginDTO loginDTO, HttpServletResponse response) {
        // 1. 根据账号（邮箱/手机号）查询用户
        User user = getByAccount(loginDTO.getAccount());
        if (user == null) {
            throw new BusinessException(ResultCode.ACCOUNT_NOT_EXIST);
        }

        // 2. 密码校验
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }

        // 3. 生成token
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId());
        
        String token = JwtUtil.generateToken(claims);

        // 4. 设置 HttpOnly Cookie
        Cookie cookie = new Cookie("token", token);
        cookie.setHttpOnly(true);      // JavaScript 无法访问
        cookie.setSecure(false);       // 开发环境 false，生产环境改为 true
        cookie.setPath("/");           // 全站有效
        cookie.setMaxAge(7200);        // 2小时过期
        cookie.setAttribute("SameSite", "Lax");  // 防止 CSRF
        response.addCookie(cookie);
        
        log.info("用户 {} 登录成功，Cookie已设置", user.getId());

        // 5. 封装VO（不再返回 token）
        UserVO userVO = BeanUtil.copyProperties(user, UserVO.class);
        LoginVO loginVO = new LoginVO();
        loginVO.setUser(userVO);

        return loginVO;
    }

    @Override
    public void updatePassword(Long userId, UpdatePasswordDTO updatePasswordDTO) {
        // 1. 密码一致性校验
        if (!StrUtil.equals(updatePasswordDTO.getNewPassword(), updatePasswordDTO.getConfirmPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_NOT_MATCH);
        }

        // 2. 密码长度校验
        String newPassword = updatePasswordDTO.getNewPassword();
        if (StrUtil.length(newPassword) < minLength || StrUtil.length(newPassword) > maxLength) {
            throw new BusinessException(ResultCode.PASSWORD_LENGTH_INVALID);
        }

        // 3. 查询用户
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }

        // 4. 旧密码校验
        if (!passwordEncoder.matches(updatePasswordDTO.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.OLD_PASSWORD_ERROR);
        }

        // 5. 更新加密后的新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        updateById(user);
    }

    @Override
    public void updateUserInfo(Long userId, UserDTO userDTO) {
        // 1. 查询当前用户
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }

        // 2. 只更新非空字段
        if (StrUtil.isNotBlank(userDTO.getUsername())) {
            // 校验用户名是否已被其他用户使用
            if (!StrUtil.equals(user.getUsername(), userDTO.getUsername())) {
                LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(User::getUsername, userDTO.getUsername());
                if (getOne(wrapper) != null) {
                    throw new BusinessException(ResultCode.USERNAME_ALREADY_EXISTS);
                }
            }
            user.setUsername(userDTO.getUsername());
        }

        if (userDTO.getAvatar() != null) {
            user.setAvatar(userDTO.getAvatar());
        }

        if (StrUtil.isNotBlank(userDTO.getPhone())) {
            // 校验手机号是否已被其他用户使用
            if (!StrUtil.equals(user.getPhone(), userDTO.getPhone())) {
                LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(User::getPhone, userDTO.getPhone());
                if (getOne(wrapper) != null) {
                    throw new BusinessException(ResultCode.PHONE_ALREADY_EXISTS);
                }
            }
            user.setPhone(userDTO.getPhone());
        }

        if (userDTO.getMotto() != null) {
            user.setMotto(userDTO.getMotto());
        }

        // 更新用户信息
        updateById(user);
    }



    @Override
    public User getByAccount(String account) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, account)
                .or().eq(User::getPhone, account);
        return getOne(wrapper);
    }

    @Override
    public String uploadAvatar(Long userId, MultipartFile file) {
        // 1. 校验文件是否为空
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请选择要上传的头像文件");
        }

        // 2. 校验文件大小（最大5MB）
        long maxSize = 5 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new BusinessException(ResultCode.FILE_SIZE_EXCEEDED, "头像文件大小不能超过5MB");
        }

        // 3. 校验文件类型（只允许图片格式）
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(ResultCode.FILE_TYPE_INVALID, "只支持上传图片格式文件（jpg/png/gif等）");
        }

        // 4. 校验文件扩展名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.matches(".*\\.(jpg|jpeg|png|gif|bmp|webp)$")) {
            throw new BusinessException(ResultCode.FILE_TYPE_INVALID, "只支持 jpg、jpeg、png、gif、bmp、webp 格式的图片");
        }

        try {
            // 5. 查询当前用户，获取旧头像URL
            User user = getById(userId);
            if (user == null) {
                throw new BusinessException(ResultCode.USER_NOT_EXIST);
            }
            String oldAvatarUrl = user.getAvatar();

            // 6. 上传新头像到OSS
            String newAvatarUrl = aliyunOSSOperator.upload(file, originalFilename);

            // 7. 更新数据库中的头像URL
            user.setAvatar(newAvatarUrl);
            updateById(user);

            // 8. 如果用户之前有头像，尝试删除旧头像（避免OSS存储浪费）
            if (oldAvatarUrl != null && oldAvatarUrl.contains("aliyuncs.com")) {
                try {
                    String oldObjectKey = aliyunOSSOperator.extractObjectKeyFromUrl(oldAvatarUrl);
                    aliyunOSSOperator.deleteFile(oldObjectKey);
                    log.info("已删除旧头像: {}", oldObjectKey);
                } catch (Exception e) {
                    // 删除旧头像失败不影响主流程，只记录日志
                    log.warn("删除旧头像失败: {}", oldAvatarUrl, e);
                }
            }

            log.info("用户 {} 上传头像成功: {}", userId, newAvatarUrl);
            return newAvatarUrl;

        } catch (BusinessException e) {
            // 业务异常直接抛出
            throw e;
        } catch (Exception e) {
            log.error("上传头像失败", e);
            throw new BusinessException(ResultCode.AVATAR_UPLOAD_FAILED, "头像上传失败");
        }
    }

    @Override
    public void logout(HttpServletResponse response) {
        // 清除 Cookie
        Cookie cookie = new Cookie("token", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);  // 生产环境改为 true
        cookie.setPath("/");
        cookie.setMaxAge(0);  // 立即过期
        response.addCookie(cookie);
        
        log.info("用户退出登录，Cookie已清除");
    }

    @Override
    public LoginVO loginByVerificationCode(String email, String verificationCode, HttpServletResponse response) {
        // 1. 验证验证码
        if (!verificationCodeService.isValidCode(email, verificationCode)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "验证码错误或已过期");
        }

        // 2. 根据邮箱查询用户
        User user = getByAccount(email);
        if (user == null) {
            throw new BusinessException(ResultCode.ACCOUNT_NOT_EXIST, "该邮箱未注册，请先注册");
        }

        // 3. 生成token
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId());
        String token = JwtUtil.generateToken(claims);

        // 4. 设置 HttpOnly Cookie
        Cookie cookie = new Cookie("token", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(7200);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
        
        log.info("用户 {} 通过验证码登录成功", user.getId());

        // 5. 删除已使用的验证码(防止重用)
        verificationCodeService.removeCode(email);

        // 6. 封装返回结果
        UserVO userVO = BeanUtil.copyProperties(user, UserVO.class);
        LoginVO loginVO = new LoginVO();
        loginVO.setUser(userVO);

        return loginVO;
    }

    @Override
    public void registerByVerificationCode(VerificationCodeRegisterDTO registerDTO) {
        // 1. 密码一致性校验
        if (!StrUtil.equals(registerDTO.getPassword(), registerDTO.getConfirmPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "两次密码不一致");
        }

        // 2. 验证验证码
        if (!verificationCodeService.isValidCode(registerDTO.getEmail(), registerDTO.getVerificationCode())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "验证码错误或已过期");
        }

        // 3. 校验用户名/邮箱/手机号是否已存在
        if (getByAccount(registerDTO.getUsername()) != null) {
            throw new BusinessException(ResultCode.USERNAME_ALREADY_EXISTS, "用户名已存在");
        }
        if (getByAccount(registerDTO.getEmail()) != null) {
            throw new BusinessException(ResultCode.EMAIL_ALREADY_EXISTS, "邮箱已被注册");
        }
        if (getByAccount(registerDTO.getPhone()) != null) {
            throw new BusinessException(ResultCode.PHONE_ALREADY_EXISTS, "手机号已被注册");
        }

        // 4. 使用 BeanUtil 复制属性并设置加密密码
        User user = BeanUtil.copyProperties(registerDTO, User.class);
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));

        // 5. 保存用户
        save(user);

        // 6. 删除已使用的验证码
        verificationCodeService.removeCode(registerDTO.getEmail());

        log.info("用户通过验证码注册成功: {}", registerDTO.getEmail());
    }

    @Override
    public void resetPasswordByVerificationCode(ResetPasswordDTO resetPasswordDTO) {
        // 1. 密码一致性校验
        if (!StrUtil.equals(resetPasswordDTO.getNewPassword(), resetPasswordDTO.getConfirmPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "两次密码不一致");
        }

        // 2. 验证验证码
        if (!verificationCodeService.isValidCode(resetPasswordDTO.getEmail(), resetPasswordDTO.getVerificationCode())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "验证码错误或已过期");
        }

        // 3. 根据邮箱查询用户
        User user = getByAccount(resetPasswordDTO.getEmail());
        if (user == null) {
            throw new BusinessException(ResultCode.ACCOUNT_NOT_EXIST, "该邮箱未注册");
        }

        // 4. 更新密码
        user.setPassword(passwordEncoder.encode(resetPasswordDTO.getNewPassword()));
        updateById(user);

        // 5. 删除已使用的验证码
        verificationCodeService.removeCode(resetPasswordDTO.getEmail());

        log.info("用户通过验证码重置密码成功: {}", resetPasswordDTO.getEmail());
    }

}
