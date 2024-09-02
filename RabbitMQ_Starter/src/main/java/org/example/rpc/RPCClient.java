package org.example.rpc;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * @auther Kindow
 * @date 2024/9/1
 * @project RabbitMQ_Starter
 */


public class RPCClient implements AutoCloseable {

    private Connection connection;
    private Channel channel;
    private String requestQueueName = "rpc_queue";

    public RPCClient() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        connection = factory.newConnection();
        channel = connection.createChannel();
    }

    public static void main(String[] argv) {
        try (RPCClient fibonacciRpc = new RPCClient()) {
            for (int i = 0; i < 32; i++) {
                String i_str = Integer.toString(i);
                System.out.println(" [x] Requesting fib(" + i_str + ")");
                // 似乎是阻塞来远程获取需要的斐波那契数列
                String response = fibonacciRpc.call(i_str);
                System.out.println(" [.] Got '" + response + "'");
            }
        } catch (IOException | TimeoutException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }


    // 可以改进为并发执行的，即多个请求同时达到服务端
    // 其实质是通过匿名队列实现了消息的可达性。即请求和响应是一对一的对应关系
    public String call(String message) throws IOException, InterruptedException, ExecutionException {
        final String corrId = UUID.randomUUID().toString();

        System.out.println("生成的UUID是" + corrId);

        // 获取一个匿名队列，用于从RabbitMQ中获取消息
        String replyQueueName = channel.queueDeclare().getQueue();

        // 定义了消息属性
        AMQP.BasicProperties props = new AMQP.BasicProperties
                .Builder()
                // 相关性ID（correlationId）
                .correlationId(corrId)
                // 回复队列名称（replyTo）
                .replyTo(replyQueueName)
                .build();

        // String exchange, String routingKey, AMQP.BasicProperties props, byte[] body
        // 向消息队列发送了一个数字，表示求这个数字大小的斐波那契数列
        channel.basicPublish("", requestQueueName, props, message.getBytes("UTF-8"));

        final CompletableFuture<String> response = new CompletableFuture<>();


        // 自动处理消息，即ACK为Ture
        // 接受到消息后，完成Lambda表达式中的操作
        String ctag = channel.basicConsume(replyQueueName, true, (consumerTag, delivery) -> {
                    // 根据corrId来关联响应消息与请求消息
                    if (delivery.getProperties().getCorrelationId().equals(corrId)) {
                        // CompletableFuture 类中的 complete() 方法用于手动完成一个异步任务，并设置其结果
                        response.complete(new String(delivery.getBody(), "UTF-8"));
                    }
                },
                // 消费者主动取消或者被动取消消费【即不在等待队列传来的消息】，调用的处理后事的方法
                consumerTag -> {
                });

        // 阻塞等待任务完成
        String result = response.get();
        // 取消该消费者通道，即停止该消费者继续消费，解除消费者等待消息造成阻塞
        channel.basicCancel(ctag);
        return result;
    }

    public void close() throws IOException {
        connection.close();
    }
}
