package com.gdut.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    /**
     * 配置AI消息保存的异步线程池
     */
    @Bean("aiMessageSaveExecutor")
    public Executor aiMessageSaveExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数：保持活跃的最小线程数
        executor.setCorePoolSize(5);
        
        // 最大线程数：线程池允许的最大线程数
        executor.setMaxPoolSize(10);
        
        // 队列容量：当核心线程都在忙时，任务放入队列等待
        executor.setQueueCapacity(100);
        
        // 线程空闲时间：超过核心线程数的线程，空闲多久后销毁（秒）
        executor.setKeepAliveSeconds(60);
        
        // 线程名称前缀：方便排查问题
        executor.setThreadNamePrefix("ai-msg-save-");
        
        // 拒绝策略：队列满且达到最大线程数时的处理策略
        // CallerRunsPolicy：由调用线程（提交任务的线程）执行，起到降级保护作用
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // 等待时间（秒）
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        
        log.info("AI消息保存异步线程池初始化完成: core={}, max={}, queue={}", 
                executor.getCorePoolSize(), 
                executor.getMaxPoolSize(), 
                executor.getQueueCapacity());
        
        return executor;
    }

    /**
     * 配置默认的异步线程池（可选）
     */
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
