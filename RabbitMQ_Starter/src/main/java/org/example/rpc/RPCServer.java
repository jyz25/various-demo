package org.example.rpc;

import com.rabbitmq.client.*;

public class RPCServer {

    private static final String RPC_QUEUE_NAME = "rpc_queue";

    // 返回斐波那契数列第n个的数值
    private static int fib(int n) {
        if (n == 0) return 0;
        if (n == 1) return 1;
        return fib(n - 1) + fib(n - 2);
    }

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        // 普通的、不持久化的、非排他、不会自动删除的队列
        channel.queueDeclare(RPC_QUEUE_NAME, false, false, false, null);
        // 清空名为 RPC_QUEUE_NAME 的队列中的所有消息
        channel.queuePurge(RPC_QUEUE_NAME);

        // 一次只发送一条消息，消费者确认后才能发送第二条
        channel.basicQos(1);

        System.out.println(" [x] 正在等待RPC请求");

        // 当收到来自消费者的请求时，应该执行的操作
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            // 封装消息属性
            AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                    .Builder()
                    // correlationId : 用于将消费者的请求和生产者的响应一一对应起来
                    // 使得每个消息能够得到正确的响应
                    .correlationId(delivery.getProperties().getCorrelationId())
                    .build();

            String response = "";
            try {
                String message = new String(delivery.getBody(), "UTF-8");
                int n = Integer.parseInt(message);

                System.out.println(" [.] fib(" + message + ")");
                response += fib(n);
            } catch (RuntimeException e) {
                System.out.println(" [.] " + e);
            } finally {
                // 将结果发送给客户端
                // delivery.getProperties().getReplyTo() 获取客户端发送消息时指定的回复队列。
                channel.basicPublish("", delivery.getProperties().getReplyTo(), replyProps, response.getBytes("UTF-8"));
                // 用于确认消息已经成功处理
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            }
        };

        channel.basicConsume(RPC_QUEUE_NAME, false, deliverCallback, (consumerTag -> {}));
    }
}
