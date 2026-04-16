package com.gdut.domain.entity.user;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static com.gdut.constant.AppConstants.DEFAULT_AVATAR_URL;

@Data
@TableName("user")
@AllArgsConstructor
@NoArgsConstructor
public class User {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    // 用户名
    private String username;
    // 邮箱
    private String email;
    // 手机号
    private String phone;
    // 加密后的密码
    private String password;
    // 头像URL
    private String avatar = DEFAULT_AVATAR_URL;
    // 创建时间
    private LocalDateTime createTime;
    // 更新时间
    private LocalDateTime updateTime;
    // 座右铭
    private String motto;
}