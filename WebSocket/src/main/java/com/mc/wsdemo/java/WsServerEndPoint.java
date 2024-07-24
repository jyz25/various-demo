package com.mc.wsdemo.java;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @auther Kindow
 * @date 2024/7/23
 * @project ws-demo
 * <p>
 * 监听地址 /myWs
 */


@ServerEndpoint("/myWs")
@Component
@Slf4j
public class WsServerEndPoint {

    static Map<String, Session> sessionMap = new ConcurrentHashMap<>();

    // 连接建立的操作
    @OnOpen
    public void onOpen(Session session) {
        sessionMap.put(session.getId(), session);
        log.info("WebSocket is open");
    }


    // 收到了客户端消息的操作
    @OnMessage
    public String onMessage(String test) {
        log.info(test);
        return "hello";
    }

    // 连接关闭时的操作
    @OnClose
    public void onClose(Session session) {
        sessionMap.remove(session.getId());
        log.info("WebSocket is close");
    }


    // 服务每隔两秒发送心跳
    @Scheduled(fixedRate = 2000)
    public void sendMessage() {
        for (String key : sessionMap.keySet()) {
            try {
                sessionMap.get(key).getBasicRemote().sendText("心跳");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


}
