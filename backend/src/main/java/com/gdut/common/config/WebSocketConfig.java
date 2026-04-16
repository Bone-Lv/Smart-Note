package com.gdut.common.config;

import com.gdut.common.util.ChatWebSocketHandler;
import com.gdut.common.interceptor.WebSocketInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final WebSocketInterceptor webSocketInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 添加 WebSocket 处理器
        registry.addHandler(chatWebSocketHandler, "/ws")
                .addInterceptors(webSocketInterceptor)  // 添加拦截器
                .setAllowedOrigins("*");                // 允许跨域
    }
}
