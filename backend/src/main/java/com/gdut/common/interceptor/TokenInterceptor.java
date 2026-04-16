package com.gdut.common.interceptor;

import com.gdut.annotation.RequireRole;
import com.gdut.common.util.JwtUtil;
import com.gdut.common.util.UserContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;
import java.util.Map;

@Slf4j
@Component
public class TokenInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, 
                            @NonNull HttpServletResponse response,
                            @NonNull Object handler) {

        // 只拦截 Controller 方法请求
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        // 获取当前方法对象
        Method method = ((HandlerMethod) handler).getMethod();
        if (method.getAnnotation(RequireRole.class) == null) {
            return true;
        }

        log.info("当前方法需要登录校验");

        // 优先级1: 从 Cookie 获取 Token
        String token = getTokenFromCookie(request);
        
        // 优先级2: 从 Header 获取（兼容移动端或特殊场景）
        if (token == null || token.isEmpty()) {
            token = request.getHeader("token");
        }
        

        if (token == null || token.isEmpty()) {
            log.info("令牌为空，响应401");
            writeErrorResponse(response, 401, "未登录或登录已过期");
            return false;
        }

        Map<String, Object> claims;

        try {
            claims = JwtUtil.parseToken(token);
        } catch (Exception e) {
            log.info("令牌无效，响应401");
            writeErrorResponse(response, 401, "令牌无效或已过期");
            return false;
        }

        // 验证token中是否包含用户ID
        Object idObj = claims.get("id");
        if (idObj == null) {
            log.warn("Token中缺少用户ID");
            writeErrorResponse(response, 401, "令牌信息不完整");
            return false;
        }

        Long userId;
        try {
            userId = Long.valueOf(idObj.toString());
        } catch (NumberFormatException e) {
            log.warn("Token中的用户ID格式错误");
            writeErrorResponse(response, 401, "令牌格式错误");
            return false;
        }

        // 检查是否需要刷新token
        if (JwtUtil.needRefresh(token)) {
            String newToken = JwtUtil.refreshToken(token);
            if (newToken != null) {
                log.info("刷新令牌成功");
                
                // 更新 Cookie 中的 Token
                Cookie cookie = new Cookie("token", newToken);
                cookie.setHttpOnly(true);
                cookie.setSecure(false);
                cookie.setPath("/");
                cookie.setMaxAge(7200);
                response.addCookie(cookie);
            }
        }

        UserContext.setUserId(userId);
        log.info("用户已登录，ID：{}", userId);
        return true;
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

    /**
     * 写入统一的错误响应JSON
     */
    private void writeErrorResponse(HttpServletResponse response, int code, String message) {
        try {
            response.setStatus(code);
            response.setContentType("application/json;charset=UTF-8");
            String json = String.format("{\"code\":%d,\"msg\":\"%s\",\"data\":null}", code, message);
            response.getWriter().write(json);
        } catch (Exception e) {
            log.error("写入错误响应失败", e);
        }
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, 
                               @NonNull HttpServletResponse response, 
                               @NonNull Object handler, 
                               Exception ex) {
        UserContext.remove();
    }
}
