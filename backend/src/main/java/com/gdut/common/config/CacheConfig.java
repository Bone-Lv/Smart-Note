package com.gdut.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    /**
     * 配置Caffeine缓存管理器
     * - noteDetail: 缓存笔记详情，热点数据保护数据库，30分钟过期
     * - noteFrequency: 记录笔记访问频率，用于LFU算法，30分钟过期
     * - verificationCode: 存储邮箱验证码，5分钟过期
     * - verificationCodeTimestamp: 存储验证码发送时间戳，用于频率限制，5分钟过期
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "noteDetail", 
                "noteFrequency",
                "verificationCode",
                "verificationCodeTimestamp"
        );
        
        // 默认配置：30分钟过期
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .recordStats());
        
        // 为验证码相关缓存设置单独的5分钟过期时间
        cacheManager.registerCustomCache("verificationCode", 
                Caffeine.newBuilder()
                        .maximumSize(1000)
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .recordStats()
                        .build());
        
        cacheManager.registerCustomCache("verificationCodeTimestamp",
                Caffeine.newBuilder()
                        .maximumSize(1000)
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .recordStats()
                        .build());
        
        return cacheManager;
    }
}
