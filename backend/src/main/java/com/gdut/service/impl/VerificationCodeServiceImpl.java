package com.gdut.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.gdut.common.exception.BusinessException;
import com.gdut.common.enums.ResultCode;
import com.gdut.service.EmailService;
import com.gdut.service.VerificationCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class VerificationCodeServiceImpl implements VerificationCodeService {

    private final CacheManager cacheManager;
    private final EmailService emailService;
    
    private static final String CACHE_NAME = "verificationCode";
    private static final String TIMESTAMP_CACHE_NAME = "verificationCodeTimestamp";
    private static final int CODE_LENGTH = 6;
    private static final long SEND_INTERVAL_MS = 60 * 1000; // 1分钟

    @Override
    public void sendVerificationCode(String email) {
        // 1. 校验邮箱格式
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "邮箱格式不正确");
        }

        // 2. 检查发送频率
        checkSendFrequency(email);

        // 3. 生成6位随机验证码
        String code = RandomUtil.randomNumbers(CODE_LENGTH);

        // 4. 存储到缓存中
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache != null) {
            cache.put(email, code);
        }

        // 5. 记录发送时间戳
        Cache timestampCache = cacheManager.getCache(TIMESTAMP_CACHE_NAME);
        if (timestampCache != null) {
            timestampCache.put(email, System.currentTimeMillis());
        }

        // 6. 发送邮件
        try {
            emailService.sendVerificationCodeEmail(email, code);
            log.info("验证码邮件发送成功: {}", email);
        } catch (Exception e) {
            log.error("验证码邮件发送失败: {}", email, e);
            // 开发环境:如果邮件发送失败,打印验证码到控制台
            log.warn("========== 开发模式：验证码输出到控制台 ==========");
            log.warn("邮箱: {}", email);
            log.warn("验证码: {}", code);
            log.warn("================================================");
        }
    }

    @Override
    public boolean isValidCode(String email, String code) {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache == null) {
            log.error("验证码缓存不存在");
            return false;
        }
        
        Cache.ValueWrapper valueWrapper = cache.get(email);
        if (valueWrapper == null) {
            log.warn("邮箱 {} 的验证码不存在或已过期", email);
            return false;
        }
        
        String storedCode = (String) valueWrapper.get();
        boolean isValid = code.equals(storedCode);
        
        if (!isValid) {
            log.warn("邮箱 {} 的验证码错误", email);
        }
        
        return isValid;
    }

    @Override
    public void removeCode(String email) {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache != null) {
            cache.evict(email);
            log.debug("已删除邮箱 {} 的验证码", email);
        }
    }

    /**
     * 检查发送频率
     */
    private void checkSendFrequency(String email) {
        Cache timestampCache = cacheManager.getCache(TIMESTAMP_CACHE_NAME);
        if (timestampCache == null) {
            return;
        }

        Cache.ValueWrapper timestampWrapper = timestampCache.get(email);
        if (timestampWrapper != null) {
            Long lastSendTime = (Long) timestampWrapper.get();
            long currentTime = System.currentTimeMillis();
            long timeDiff = currentTime - lastSendTime;

            if (timeDiff < SEND_INTERVAL_MS) {
                long remainingSeconds = (SEND_INTERVAL_MS - timeDiff) / 1000;
                log.warn("邮箱 {} 的验证码发送过于频繁，还需等待 {} 秒", email, remainingSeconds);
                throw new BusinessException(ResultCode.BAD_REQUEST, "验证码发送过于频繁，请" + remainingSeconds + "秒后再试");
            }
        }
    }
}
