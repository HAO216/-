package com.hjg.reggie;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/*
* @Slf4j：开启日志
* @SpringBootApplication：它会自动为我们扫描配置，最终成功启动项目
* @ServletComponentScan：扫描组件，如WebFilter登录过滤器等
* */
@Slf4j
@SpringBootApplication
@ServletComponentScan
@EnableTransactionManagement //开启对事物管理的支持
@EnableCaching//开启Spring Cache注解方式的缓存功能
public class ReggieApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReggieApplication.class,args);
        log.info("项目启动成功...");
    }
}
