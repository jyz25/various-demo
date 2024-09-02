package org.example.route;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * @auther Kindow
 * @date 2024/8/30
 * @project RabbitMQ_Starter
 * 路由篇 -- 队列如何通过键获取感兴趣的信息
 * 消息接受器 -- 在服务器和客户端中创建一个匿名队列，用于接受指定键的消息
 */
public class ReceiveLogsDirect {

    private static final String EXCHANGE_NAME = "direct_logs";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.exchangeDeclare(EXCHANGE_NAME, "direct");
        // 在交换机内创建一个临时队列，【非持久，排他，自动销毁】
        String queueName = channel.queueDeclare().getQueue();
        if (argv.length < 1) {
            // 未设置启动参数，即该接受者应该接收哪些消息
            System.err.println("Usage: ReceiveLogsDirect [info] [warning] [error]");
            System.exit(1);
        }

        for (String severity : argv) {
            // 获取该队列可以接受的消息类型
            channel.queueBind(queueName, EXCHANGE_NAME, severity);
            System.out.println(" [*] 正在接受消息. 退出请按 CTRL+C 当前实例可接收的类型" + severity);
        }

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [x] 已接受到 '" +
                    delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
        };

        // basicConsume
        channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {
        });
    }
}
