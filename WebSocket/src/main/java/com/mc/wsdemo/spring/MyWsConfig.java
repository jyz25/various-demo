package com.mc.wsdemo.spring;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import javax.annotation.Resource;

/**
 * @auther Kindow
 * @date 2024/7/24
 * @project ws-demo
 */

@Configuration
@EnableWebSocket
public class MyWsConfig implements WebSocketConfigurer {

    @Resource
    private MyWsHandler myWsHandler;


    @Resource
    private MyWsInterceptor myWsInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(myWsHandler, "/myWs1")
                .addInterceptors(myWsInterceptor)
                .setAllowedOrigins("*");
    }
}
