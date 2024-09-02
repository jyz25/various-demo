# RabbitMQ学习

[参考文档](https://rabbitmq.org.cn/tutorials/tutorial-one-java)

## 1、Hello World

基本配置：

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>org.example</groupId>
  <artifactId>RabbitMQ_Starter</artifactId>
  <version>1.0-SNAPSHOT</version>
  <packaging>jar</packaging>

  <name>RabbitMQ_Starter</name>
  <url>http://maven.apache.org</url>

  <properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  </properties>

  <dependencies>
    <dependency>
      <groupId>com.rabbitmq</groupId>
      <artifactId>amqp-client</artifactId>
      <version>5.14.2</version>
    </dependency>

    <dependency>
      <groupId>org.slf4j</groupId>
      <artifactId>slf4j-api</artifactId>
      <version>2.0.13</version>
    </dependency>

    <dependency>
      <groupId>org.slf4j</groupId>
      <artifactId>slf4j-simple</artifactId>
      <version>2.0.13</version>
    </dependency>

    <dependency>
      <groupId>junit</groupId>
      <artifactId>junit</artifactId>
      <version>3.8.1</version>
      <scope>test</scope>
    </dependency>
  </dependencies>
</project>
```

生产者消费者：

```java
// send
package org.example;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.nio.charset.StandardCharsets;

public class Send {

    private final static String QUEUE_NAME = "hello";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
            String message = "Hello World!";
            channel.basicPublish("", QUEUE_NAME, null, message.getBytes(StandardCharsets.UTF_8));
            System.out.println(" [x] Sent '" + message + "'");
        }
    }
}

// recv
package org.example;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import java.nio.charset.StandardCharsets;

public class Recv {

    private final static String QUEUE_NAME = "hello";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println(" [x] Received '" + message + "'");
        };
        channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> { });
    }
}
```



## 2、工作队列

综合代码：

```java
// NewTask
package org.example.queuelean;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;

public class NewTask {

    private static final String TASK_QUEUE_NAME = "task_queue";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.queueDeclare(TASK_QUEUE_NAME, true, false, false, null);

            String message = String.join(" ", argv);

            // 标明消息需要进行持久化
            channel.basicPublish("", TASK_QUEUE_NAME,
                                 MessageProperties.PERSISTENT_TEXT_PLAIN,
                                 message.getBytes("UTF-8"));
            System.out.println(" [x] Sent '" + message + "'");
        }
    }

}

// Worker
package org.example.queuelean;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

public class Worker {

    private static final String TASK_QUEUE_NAME = "task_queue";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        final Connection connection = factory.newConnection();
        final Channel channel = connection.createChannel();

        // 持久化类型的队列，避免崩溃时消息丢失
        channel.queueDeclare(TASK_QUEUE_NAME, true, false, false, null);
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        // 消费者消费一条信息并确认后，才可以发送第二条信息
        channel.basicQos(1);

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");

            System.out.println(" [x] Received '" + message + "'");
            try {
                doWork(message);
            } finally {
                System.out.println(" [x] Done");
                // 消息处理结束后，需要手动发送确认信息
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            }
        };
        // 消息消费结束后需要手动消息确认
        channel.basicConsume(TASK_QUEUE_NAME, false, deliverCallback, consumerTag -> { });
    }

    private static void doWork(String task) {
        for (char ch : task.toCharArray()) {
            if (ch == '.') {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException _ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
```



## 3、发布/订阅

```java
public class EmitLog {

    private static final String EXCHANGE_NAME = "logs";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            // 声明一个交换机，没有会创建，名字叫做logs，类型是fanout
            channel.exchangeDeclare(EXCHANGE_NAME, "fanout");

            String message = argv.length < 1 ? "info: Hello World!" :
            String.join(" ", argv);

            // 在logs交换机的无名队列中添加消息
            channel.basicPublish(EXCHANGE_NAME, "", null, message.getBytes("UTF-8"));
            System.out.println(" [x] Sent '" + message + "'");
        }
    }
}
```



```java
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

public class ReceiveLogs {
    private static final String EXCHANGE_NAME = "logs";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
        String queueName = channel.queueDeclare().getQueue();
        // 将创建的临时队列和交换机绑定
        // 消费者连接之前的所有消息，会自动丢失【交换机的消息没有队列可传】
        channel.queueBind(queueName, EXCHANGE_NAME, "");

        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [x] Received '" + message + "'");
        };
        channel.basicConsume(queueName, true, deliverCallback, consumerTag -> { });
    }
}
```



## 4、路由

这里我定义了一个消息发送器，发送50条消息，以三分之一的概率分别发送`info`、`warning`、`error`日志

我定义了一个消息接收器，通过启动实例参数，可以创建四条临时队列用于接受消息【全部接受，接受info，接受warning、接受error】

代码：

```java
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
```



```java
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
```



**一些雷点：**

我们不能使用@Test方法来执行消费端的代码，当一个消费者被启动起来后，它只能接收启动之后生产者发送来的消息，这意味着，如果我们在两个@Test的代码中，一个发送消息，一个接受消息，那么消费者能在短暂的执行期间接收到发送端发送的消息是不可能的【在发送消息的过程中，很有可能因为@Test修饰的代码强制结束而导致消费者等待消息线程自动被终止，而导致无法收到消息】PS：这个BUG卡了我很久，哪里知道使用main方法执行的代码不会自动终止，而执行test的方法会被强制终止，真是一个大坑。

```java
public static void main(String[] args) throws IOException, TimeoutException {
    try {
        //创建连接
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        //创建消息信道
        final Channel channel = connection.createChannel();
        //消息队列
        channel.queueDeclare(CommonConstants.QUERE_KEY_PRAISE, true, false, false, null);
        //绑定队列到交换机
        channel.queueBind(CommonConstants.QUERE_KEY_PRAISE, CommonConstants.EXCHANGE_NAME_DIRECT,  CommonConstants.QUERE_KEY_PRAISE);
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                log.info("Consumer msg: {}", message);
                System.out.println("Received message: " + message);
                // 发送ack以确认消息已被处理
                channel.basicAck(envelope.getDeliveryTag(), false);
            }
        };
        // 取消自动ack
        channel.basicConsume(CommonConstants.QUERE_KEY_PRAISE, false, consumer);
        // 通常不会在这里关闭channel，除非在特定的清理逻辑中
        // 例如，你可以将channel.close()放在一个finally块中，或者在应用程序关闭时调用
    } catch (Exception e) {
        e.printStackTrace();
        // 如果有必要，可以在这里关闭channel，但通常最好是在finally块中处理
    }
    // 取消自动ack
}


@Test
public void testConsume() throws IOException, TimeoutException {
    try {
        //创建连接
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        //创建消息信道
        final Channel channel = connection.createChannel();
        //消息队列
        channel.queueDeclare(CommonConstants.QUERE_KEY_PRAISE, true, false, false, null);
        //绑定队列到交换机
        channel.queueBind(CommonConstants.QUERE_KEY_PRAISE, CommonConstants.EXCHANGE_NAME_DIRECT,  CommonConstants.QUERE_KEY_PRAISE);
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                log.info("Consumer msg: {}", message);
                System.out.println("Received message: " + message);
                // 发送ack以确认消息已被处理
                channel.basicAck(envelope.getDeliveryTag(), false);
            }
        };
        // 取消自动ack
        channel.basicConsume(CommonConstants.QUERE_KEY_PRAISE, false, consumer);
        // 通常不会在这里关闭channel，除非在特定的清理逻辑中
        // 例如，你可以将channel.close()放在一个finally块中，或者在应用程序关闭时调用
    } catch (Exception e) {
        e.printStackTrace();
        // 如果有必要，可以在这里关闭channel，但通常最好是在finally块中处理
    }
    // 取消自动ack
}

```

比如上面这两段代码，如果运行Test的话是无法接受到消息的，而运行main就可以，不要问为什么，以后测试用main吧，简单省事

> ps:这和临时队列是不同的，临时队列是无法接受到创建队列之前的消息，而此处的代码队列是已经创建好的，这意味着队列中可以临时积攒消息，哪怕消费者后续连接，也依旧可以消费队列中已经存放的消息。所以此处要把临时队列和这个BUG区分开。



## 5、主题

启动参数

- #
- kern.*
- *.crit
- kern.* *.crit



日志发射器：

```java
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
```



日志接收器：

```java
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
```



## 6、RPC

```java
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
```



```java
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
```





## 7、发布者确认

一种确保生产者成功将消息发送到RabbitMQ服务器的一种机制

```java
package org.example.publishercomfirm;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConfirmCallback;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.BooleanSupplier;
/**
 * @auther Kindow
 * @date 2024/9/2
 * @project RabbitMQ_Starter
 */


public class PublisherConfirms {

    static final int MESSAGE_COUNT = 50_000;

    static Connection createConnection() throws Exception {
        ConnectionFactory cf = new ConnectionFactory();
        cf.setHost("localhost");
        cf.setUsername("guest");
        cf.setPassword("guest");
        return cf.newConnection();
    }

    public static void main(String[] args) throws Exception {
        publishMessagesIndividually();
        publishMessagesInBatch();
        handlePublishConfirmsAsynchronously();
    }

    static void publishMessagesIndividually() throws Exception {
        try (Connection connection = createConnection()) {
            Channel ch = connection.createChannel();

            String queue = UUID.randomUUID().toString();
            ch.queueDeclare(queue, false, false, true, null);

            ch.confirmSelect();
            long start = System.nanoTime();
            for (int i = 0; i < MESSAGE_COUNT; i++) {
                String body = String.valueOf(i);
                ch.basicPublish("", queue, null, body.getBytes());
                ch.waitForConfirmsOrDie(5_000);
            }
            long end = System.nanoTime();
            System.out.format("Published %,d messages individually in %,d ms%n", MESSAGE_COUNT, Duration.ofNanos(end - start).toMillis());
        }
    }

    static void publishMessagesInBatch() throws Exception {
        try (Connection connection = createConnection()) {
            Channel ch = connection.createChannel();

            String queue = UUID.randomUUID().toString();
            ch.queueDeclare(queue, false, false, true, null);

            ch.confirmSelect();

            int batchSize = 100;
            int outstandingMessageCount = 0;

            long start = System.nanoTime();
            for (int i = 0; i < MESSAGE_COUNT; i++) {
                String body = String.valueOf(i);
                ch.basicPublish("", queue, null, body.getBytes());
                outstandingMessageCount++;

                if (outstandingMessageCount == batchSize) {
                    ch.waitForConfirmsOrDie(5_000);
                    outstandingMessageCount = 0;
                }
            }

            if (outstandingMessageCount > 0) {
                ch.waitForConfirmsOrDie(5_000);
            }
            long end = System.nanoTime();
            System.out.format("Published %,d messages in batch in %,d ms%n", MESSAGE_COUNT, Duration.ofNanos(end - start).toMillis());
        }
    }

    static void handlePublishConfirmsAsynchronously() throws Exception {
        try (Connection connection = createConnection()) {
            Channel ch = connection.createChannel();

            String queue = UUID.randomUUID().toString();
            ch.queueDeclare(queue, false, false, true, null);

            ch.confirmSelect();

            // 这里，outstandingConfirms 是一个 ConcurrentSkipListMap 的实例，用于存储尚未确认的消息。
            // 键是消息的序列号（Long），值是消息的内容或其他相关信息（String）。
            ConcurrentNavigableMap<Long, String> outstandingConfirms = new ConcurrentSkipListMap<>();

            // 回调接口，函数式接口 用于处理消息确认
            ConfirmCallback cleanOutstandingConfirms = (sequenceNumber, multiple) -> {
                if (multiple) {
                    // multiple为ture，表名RabbitMQ已经成功接受到一批完整的信息了
                    // sequenceNumber 此时为 最后一条消息的序列号
                    // confirmed 用于存储已经确认的消息【即最后一条序列号即之前的消息】
                    ConcurrentNavigableMap<Long, String> confirmed = outstandingConfirms.headMap(
                            sequenceNumber, true
                    );
                    confirmed.clear();
                } else {
                    // 只有一条消息被确认，移除这条消息
                    outstandingConfirms.remove(sequenceNumber);
                }
            };

            ch.addConfirmListener(cleanOutstandingConfirms, (sequenceNumber, multiple) -> {
                // 消息没被确认时该做的事情
                String body = outstandingConfirms.get(sequenceNumber);
                System.err.format(
                        "Message with body %s has been nack-ed. Sequence number: %d, multiple: %b%n",
                        body, sequenceNumber, multiple
                );
                // 该handle方法会调用回调接口中的代码
                cleanOutstandingConfirms.handle(sequenceNumber, multiple);
            });

            long start = System.nanoTime();
            for (int i = 0; i < MESSAGE_COUNT; i++) {
                String body = String.valueOf(i);
                outstandingConfirms.put(ch.getNextPublishSeqNo(), body);
                ch.basicPublish("", queue, null, body.getBytes());
            }

            if (!waitUntil(Duration.ofSeconds(60), () -> outstandingConfirms.isEmpty())) {
                throw new IllegalStateException("All messages could not be confirmed in 60 seconds");
            }

            long end = System.nanoTime();
            System.out.format("Published %,d messages and handled confirms asynchronously in %,d ms%n", MESSAGE_COUNT, Duration.ofNanos(end - start).toMillis());
        }
    }

    static boolean waitUntil(Duration timeout, BooleanSupplier condition) throws InterruptedException {
        int waited = 0;
        while (!condition.getAsBoolean() && waited < timeout.toMillis()) {
            Thread.sleep(100L);
            waited += 100;
        }
        return condition.getAsBoolean();
    }

}
```



对于RabbitMQ的学习可以告一段落了。基础入门应该没问题了



# 其他

一些好的学习网站

[官网](https://www.rabbitmq.com/tutorials/tutorial-seven-java#putting-it-all-together)

[本地测试地址](http://localhost:15672/#/queues)

可惜中文文档看不了了，我记得是有一个中文网的

[中文官网](https://rabbitmq.org.cn/tutorials/tutorial-five-java#topic-exchange)
