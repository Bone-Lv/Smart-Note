package com.gdut.controller;

import cn.hutool.core.bean.BeanUtil;
import com.gdut.annotation.RequireRole;
import com.gdut.domain.dto.user.LoginDTO;
import com.gdut.domain.dto.user.ResetPasswordDTO;
import com.gdut.domain.dto.user.SendVerificationCodeDTO;
import com.gdut.domain.dto.user.UpdatePasswordDTO;
import com.gdut.domain.dto.user.UserDTO;
import com.gdut.domain.dto.user.VerificationCodeLoginDTO;
import com.gdut.domain.dto.user.VerificationCodeRegisterDTO;
import com.gdut.domain.entity.common.Result;
import com.gdut.domain.entity.user.User;
import com.gdut.domain.vo.user.LoginVO;
import com.gdut.domain.vo.user.UserVO;
import com.gdut.service.UserService;
import com.gdut.service.VerificationCodeService;
import com.gdut.common.util.UserContext;
import com.gdut.common.util.ChatWebSocketHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Tag(name = "用户管理接口", description = "用户注册、登录、密码修改等接口")
public class UserController {

    private final UserService userService;
    private final VerificationCodeService verificationCodeService;
    private final ChatWebSocketHandler chatWebSocketHandler;

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "支持邮箱/手机号+密码登录，校验通过后将Token存入HttpOnly Cookie")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO, HttpServletResponse response) {
        return Result.success(userService.login(loginDTO, response));
    }

    @PostMapping("/logout")
    @RequireRole
    @Operation(summary = "退出登录", description = "清除认证Cookie")
    public Result<Void> logout(HttpServletResponse response) {
        userService.logout(response);
        return Result.success(null);
    }

    @PutMapping("/password")
    @RequireRole
    @Operation(summary = "修改密码", description = "校验旧密码，新密码长度8-20位，加密存储")
    public Result<Void> updatePassword(@Valid @RequestBody UpdatePasswordDTO updatePasswordDTO) {
        userService.updatePassword(UserContext.getUserId(), updatePasswordDTO);
        return Result.success(null);
    }

    @GetMapping("/check-auth")
    @RequireRole
    @Operation(summary = "检查认证状态", description = "验证用户是否已登录，用于前端路由守卫。如果Token有效返回200，无效返回401")
    public Result<Map<String, Object>> checkAuth() {
        Long userId = UserContext.getUserId();
        log.debug("用户认证状态检查成功：userId={}", userId);
        return Result.success(Map.of(
            "authenticated", true,
            "userId", userId
        ));
    }

    @GetMapping
    @RequireRole
    @Operation(summary = "查询当前用户信息", description = "根据 Token 自动获取当前登录用户ID并查询个人信息")
    public Result<UserVO> getUserInfo() {
        User user = userService.getById(UserContext.getUserId());
        UserVO userVO = BeanUtil.copyProperties(user, UserVO.class);
        
        // 设置在线状态（当前用户肯定在线）
        userVO.setOnlineStatus(1);
        
        return Result.success(userVO);
    }

    @PutMapping
    @RequireRole
    @Operation(summary = "修改当前用户信息", description = "支持修改用户名、头像、手机号、座右铭")
    public Result<Void> updateUserInfo(@Valid @RequestBody UserDTO userDTO) {
        userService.updateUserInfo(UserContext.getUserId(), userDTO);
        return Result.success(null);
    }

    @PostMapping("/avatar")
    @RequireRole
    @Operation(summary = "上传头像", description = "上传图片文件到OSS，自动更新用户头像，支持jpg/png/gif格式，最大5MB")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = UserContext.getUserId();
        String avatarUrl = userService.uploadAvatar(userId, file);
        return Result.success(avatarUrl);
    }

    @PostMapping("/send-code")
    @Operation(summary = "发送验证码", description = "向指定邮箱发送6位数字验证码，有效期5分钟")
    public Result<Void> sendVerificationCode(@Valid @RequestBody SendVerificationCodeDTO dto) {
        verificationCodeService.sendVerificationCode(dto.getEmail());
        return Result.success(null);
    }

    @PostMapping("/login-by-code")
    @Operation(summary = "验证码登录", description = "使用邮箱+验证码登录，无需密码")
    public Result<LoginVO> loginByVerificationCode(@Valid @RequestBody VerificationCodeLoginDTO loginDTO, HttpServletResponse response) {
        return Result.success(userService.loginByVerificationCode(loginDTO.getEmail(), loginDTO.getVerificationCode(), response));
    }

    @PostMapping("/register-by-code")
    @Operation(summary = "验证码注册", description = "使用邮箱验证码注册，需要先发送验证码")
    public Result<Void> registerByVerificationCode(@Valid @RequestBody VerificationCodeRegisterDTO registerDTO) {
        userService.registerByVerificationCode(registerDTO);
        return Result.success(null);
    }

    @PostMapping("/reset-password")
    @Operation(summary = "验证码重置密码", description = "忘记密码时，通过邮箱验证码重置密码")
    public Result<Void> resetPasswordByVerificationCode(@Valid @RequestBody ResetPasswordDTO resetPasswordDTO) {
        userService.resetPasswordByVerificationCode(resetPasswordDTO);
        return Result.success(null);
    }

}