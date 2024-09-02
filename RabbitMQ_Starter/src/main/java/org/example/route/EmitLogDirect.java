package org.example.route;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Random;
import java.util.concurrent.TimeoutException;


/**
 * @auther Kindow
 * @date 2024/8/30
 * @project RabbitMQ_Starter
 * 路由篇 -- 队列如何通过键获取感兴趣的信息
 * 消息发送器 -- 消息发送到路由器，但是每个消息有自己的键标识
 */
public class EmitLogDirect {
    private static final String EXCHANGE_NAME = "direct_logs";

    public static void main(String[] args) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            // 创建一个队列，名字是EXCHANGE_NAME，类型是direct
            channel.exchangeDeclare(EXCHANGE_NAME, "direct");
            emitLogs(channel);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 自定义一个日志发送器，用于模拟发送
    public static void emitLogs(Channel channel) throws IOException {
        for (int i = 0; i < 50; i++) {
            Random random = new Random();
            double v = random.nextDouble() * 0.9;
            // “info”、“warning”、“error”之一
            String severity = "info";
            String message = "info Message" + "【" + i + "条日志】";
            if (v < 0.3) {
                severity = "error";
                message = " error message" + "【" + i + "条日志】";
            } else if (v > 0.3 && v < 0.6) {
                severity = "warning";
                message = " warning message" + "【" + i + "条日志】";
            }

            // 发布消息，severity：严重程度 message:实际消息内容
            // channel.basicPublish(exchangeName, routingKey, null, messageBodyBytes);
            channel.basicPublish(EXCHANGE_NAME, severity, null, message.getBytes("UTF-8"));
            System.out.println(" [x] 发送 '" + severity + "':'" + message + "'");
        }
    }
}
