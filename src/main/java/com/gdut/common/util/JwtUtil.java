package com.gdut.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Date;
import java.util.Map;


/**
 * JWT 令牌工具类
 * 用于生成和解析 JWT 令牌
 */
@Slf4j
@Component
public class JwtUtil {
    
    @Value("${jwt.secret}")
    private String secretKeyConfig;
    
    @Value("${jwt.expiration:7200}")
    private long expirationSeconds;
    
    private static String SECRET_KEY;
    private static long EXPIRATION_TIME;
    
    @PostConstruct
    public void init() {
        SECRET_KEY = Base64.getEncoder().encodeToString(secretKeyConfig.getBytes());
        EXPIRATION_TIME = expirationSeconds * 1000;
        log.info("JWT配置初始化完成，过期时间: {}秒", expirationSeconds);
    }

    
    /**
     * 生成 JWT 令牌
     * @param claims 自定义声明数据
     * @return JWT 令牌字符串
     */
    public static String generateToken(Map<String, Object> claims) {
        Date now = new Date();
        Date expirationDate = new Date(System.currentTimeMillis() + EXPIRATION_TIME);
        
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expirationDate)
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }
    
    /**
     * 解析 JWT 令牌
     * @param token JWT 令牌字符串
     * @return Claims 对象，包含令牌中的所有声明
     */
    public static Claims parseToken(String token) {
        return Jwts.parser()
                .setSigningKey(SECRET_KEY)
                .parseClaimsJws(token)
                .getBody();
    }
    
    /**
     * 从令牌中获取指定声明的值
     * @param token JWT 令牌字符串
     * @param key 声明的键
     * @return 声明的值
     */
    public static Object getClaim(String token, String key) {
        Claims claims = parseToken(token);
        return claims.get(key);
    }
    
    /**
     * 检查令牌是否过期
     * @param token JWT 令牌字符串
     * @return true-未过期，false-已过期
     */
    public static boolean isTokenValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    /**
     * 获取当前用户ID
     * @param req HttpServletRequest 对象
     * @return 当前用户ID
     */
    public static String getCurrentUserId(HttpServletRequest req) {
        String token = req.getHeader("token");
        
        if (token == null || token.isEmpty()) {
            throw new RuntimeException("Token 不能为空");
        }
        
        return getClaim(token, "id").toString();
    }


    /**
     * 判断当前令牌是否需要刷新
     */
    public static boolean needRefresh(String token) {
        try {
            Claims claims = parseToken(token);
            Date expiration = claims.getExpiration();
            long remainingTime = expiration.getTime() - System.currentTimeMillis();
            log.info("剩余时间: {}", remainingTime);
            
            // 剩余时间少于 30 分钟，需要刷新
            return remainingTime < 30 * 60 * 1000 && remainingTime > 0;
        } catch (Exception e) {
            // 解析失败说明令牌已过期或无效，不需要刷新，直接返回 false
            return false;
        }
    }

    /**
     * 刷新令牌
     */
    public static String refreshToken(String token) {
        try {
            Claims claims = parseToken(token);
            return generateToken(claims);
        } catch (Exception e) {
            //解析不了说明令牌已经过期了
            return null;
        }
    }



}
