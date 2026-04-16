package com.gdut.common.config;

import com.gdut.common.interceptor.TokenInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final TokenInterceptor tokenInterceptor;

    /**
     * 注册拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tokenInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                    "/user/login",
                    "/user/login-by-code",
                    "/user/send-code",
                    "/user/register-by-code",
                    "/user/reset-password"
                );
        
        // 添加安全头拦截器
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    Object handler) {
                // CSP: 内容安全策略
                response.setHeader("Content-Security-Policy", 
                    "default-src 'self'; " +
                    "script-src 'self'; " +
                    "style-src 'self' 'unsafe-inline'; " +
                    "img-src 'self' data: https: blob:; " +
                    "font-src 'self' data:; " +
                    "connect-src 'self' ws://localhost:8080 wss://*; " +
                    "frame-ancestors 'none';"
                );
                // 防止点击劫持
                response.setHeader("X-Frame-Options", "DENY");
                // 禁用 MIME 类型嗅探
                response.setHeader("X-Content-Type-Options", "nosniff");
                // 启用 XSS 过滤器（旧浏览器）
                response.setHeader("X-XSS-Protection", "1; mode=block");
                return true;
            }
        }).addPathPatterns("/**");
    }

    /**
     * CORS 跨域配置
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
