package org.example.topic;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeoutException;

/**
 * @auther Kindow
 * @date 2024/8/31
 * @project RabbitMQ_Starter
 */
public class EmitLogTopic {

    private static final String EXCHANGE_NAME = "topic_logs";

    // 定义一个字符串数组来存储所有可能的组合
    private static final String[] combinations = {
            "auth.info",
            "auth.warn",
            "auth.crit",
            "cron.info",
            "cron.warn",
            "cron.crit",
            "kern.info",
            "kern.warn",
            "kern.crit"
    };

    /**
     * 随机返回日志级别和设备组合
     *
     * @return 一个随机选择的日志级别和设备组合字符串
     */
    public static String getRandomCombination() {
        // 随机选择一个索引
        int index = random.nextInt(combinations.length);
        // 返回对应的组合
        return combinations[index];
    }

    // 使用Random类来生成随机数
    private static final Random random = new Random();

    public static void main(String[] args) throws IOException, TimeoutException {

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            channel.exchangeDeclare(EXCHANGE_NAME, "topic");
            emitLogs(channel);
        }

    }

    // 自定义一个日志发送器，用于模拟发送
    public static void emitLogs(Channel channel) throws IOException {
        for (int i = 0; i < 50; i++) {
            // 严重性（info/warn/crit...）和设备（auth/cron/kern...）
            //"<设备>.<严重性>"
            String routingKey = getRandomCombination();
            String message = routingKey + " Message" + "【" + i + "条日志】";
            // 发布消息，routingKey：匹配秘钥 message:实际消息内容
            // channel.basicPublish(exchangeName, routingKey, null, messageBodyBytes);
            channel.basicPublish(EXCHANGE_NAME, routingKey, null, message.getBytes("UTF-8"));
            System.out.println(" [x] 发送 '" + routingKey + "':'" + message + "'");
        }
    }


}
