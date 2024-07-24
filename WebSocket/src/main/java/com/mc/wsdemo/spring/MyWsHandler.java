package com.mc.wsdemo.spring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @auther Kindow
 * @date 2024/7/24
 * @project ws-demo
 * web socket 主程序
 */

@Component
@Slf4j
public class MyWsHandler extends AbstractWebSocketHandler {

    private static Map<String, SessionBean> sessionBeanMap = new ConcurrentHashMap();
    private static AtomicInteger clientIdMaker = new AtomicInteger(0);

    private static StringBuffer stringBuffer = new StringBuffer();

    // 连接建立成功后的操作
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        SessionBean sessionBean = new SessionBean(session, clientIdMaker.getAndIncrement());
        sessionBeanMap.put(session.getId(), sessionBean);
        log.info(sessionBeanMap.get(session.getId()).getClientId() + ":" + "建立了连接");
        stringBuffer.append(sessionBeanMap.get(session.getId()).getClientId() + "进入了群聊<br/>");
        sendMessage(sessionBeanMap);

    }

    // 接受消息后的操作
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        super.handleTextMessage(session, message);
        log.info(String.valueOf(sessionBeanMap.get(session.getId()).getClientId() + ":" + message.getPayload()));
        stringBuffer.append(sessionBeanMap.get(session.getId()).getClientId() + ":" + message.getPayload() + "<br/>");
        sendMessage(sessionBeanMap);
    }


    // 传输异常
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        super.handleTransportError(session, exception);
        if (session.isOpen()) {
            session.close();
        }
        sessionBeanMap.remove(session.getId());
    }


    // 连接关闭
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        stringBuffer.append(sessionBeanMap.get(session.getId()).getClientId() + "退出了群聊<br/>");
        sessionBeanMap.remove(session.getId());

        sendMessage(sessionBeanMap);
    }

    // 服务每隔两秒发送心跳
   /* @Scheduled(fixedRate = 2000)
    public void sendMessage() {
        for (String key : sessionBeanMap.keySet()) {
            try {
                WebSocketSession session = sessionBeanMap.get(key).getWebSocketSession();
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage("心跳"));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }*/


    // 群发消息
    public void sendMessage(Map<String, SessionBean> sessionBeanMap) {
        for (String key : sessionBeanMap.keySet()) {
            try {
                sessionBeanMap.get(key).getWebSocketSession().sendMessage(new TextMessage(stringBuffer.toString()));
            } catch (IOException e) {
                e.printStackTrace();
                log.error(e.getMessage());
            }
        }
    }


}
