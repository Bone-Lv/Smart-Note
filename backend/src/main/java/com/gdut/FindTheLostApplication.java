package com.gdut;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableScheduling

@MapperScan("com.gdut.mapper")
public class FindTheLostApplication {

    public static void main(String[] args) {
        SpringApplication.run(FindTheLostApplication.class, args);
    }

}
