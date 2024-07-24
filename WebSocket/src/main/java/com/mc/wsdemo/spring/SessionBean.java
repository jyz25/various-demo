package com.mc.wsdemo.spring;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.socket.WebSocketSession;

/**
 * @auther Kindow
 * @date 2024/7/24
 * @project ws-demo
 */

@NoArgsConstructor
@AllArgsConstructor
@Data
public class SessionBean {
    private WebSocketSession webSocketSession;
    private Integer clientId;
}
