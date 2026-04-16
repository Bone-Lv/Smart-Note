package com.gdut.common.interceptor;

import com.gdut.common.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket握手拦截器，用于验证Token并提取用户ID
 */
@Slf4j
@Component
public class WebSocketInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(@NonNull ServerHttpRequest request, 
                                   @NonNull ServerHttpResponse response,
                                   @NonNull WebSocketHandler wsHandler, 
                                   @NonNull Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpRequest = servletRequest.getServletRequest();
            
            // 优先级1: 从 Cookie 获取 Token
            String token = getTokenFromCookie(httpRequest);
            
            // 优先级2: 从 URL 参数获取（浏览器原生 WebSocket）
            if (token == null || token.isEmpty()) {
                token = httpRequest.getParameter("token");
            }
            
            // 优先级3: 从 Header 获取（非浏览器客户端）
            if (token == null || token.isEmpty()) {
                token = httpRequest.getHeader("token");
            }
            
            if (token == null || token.isEmpty()) {
                log.warn("WebSocket握手失败：缺少token");
                return false;
            }
            
            try {
                // 验证token并提取用户ID
                Object userIdObj = JwtUtil.getClaim(token, "id");

                if (userIdObj == null) {
                    log.warn("WebSocket握手失败：token中缺少用户ID");
                    return false;
                }
                
                Long userId;
                try {
                    userId = Long.valueOf(userIdObj.toString());
                } catch (NumberFormatException e) {
                    log.error("WebSocket握手失败：用户ID格式错误", e);
                    return false;
                }
                
                // 将用户ID存入attributes，供WebSocketHandler使用
                attributes.put("userId", userId);
                log.info("WebSocket握手成功：userId={}", userId);
                return true;
            } catch (Exception e) {
                log.error("WebSocket握手失败：token验证异常", e);
                return false;
            }
        }
        return false;
    }
    
    /**
     * 从 Cookie 中获取 Token
     */
    private String getTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    @Override
    public void afterHandshake(@NonNull ServerHttpRequest request, 
                              @NonNull ServerHttpResponse response,
                              @NonNull WebSocketHandler wsHandler, 
                              Exception exception) {
        // 握手后处理，可以留空
    }
}
