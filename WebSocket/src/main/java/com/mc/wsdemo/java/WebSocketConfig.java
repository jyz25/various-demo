package com.mc.wsdemo.java;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * @auther Kindow
 * @date 2024/7/23
 * @project ws-demo
 */

@Configuration
public class WebSocketConfig {


    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}
