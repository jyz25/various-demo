package com.wdbyte.weixin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

/**
 * @auther Kindow
 * @date 2024/7/10
 * @project springboot-weixin-qrcode-login
 */

@SpringBootApplication
@ServletComponentScan(basePackages = "com.wdbyte.weixin.config")
public class SpringBootApp {
    public static void main(String[] args) {
        SpringApplication.run(SpringBootApp.class,args);
    }
}
