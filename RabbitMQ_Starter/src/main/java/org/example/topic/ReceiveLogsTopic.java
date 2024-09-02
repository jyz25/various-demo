package org.example.topic;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

/**
 * @auther Kindow
 * @date 2024/8/31
 * @project RabbitMQ_Starter
 */
public class ReceiveLogsTopic {
    private static final String EXCHANGE_NAME = "topic_logs";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE_NAME, "topic");
        // 又是一个临时队列
        String queueName = channel.queueDeclare().getQueue();

        if (argv.length < 1) {
            System.err.println("需要参数: 日志主题 [binding_key]...");
            System.exit(1);
        }

        for (String bindingKey : argv) {
            channel.queueBind(queueName, EXCHANGE_NAME, bindingKey);
            System.out.println(" [*] 正在接受消息. 退出请按 CTRL+C 当前实例可接收的类型" + bindingKey);
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
