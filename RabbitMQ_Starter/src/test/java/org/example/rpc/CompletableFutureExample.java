package org.example.rpc;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


/**
 * @auther Kindow
 * @date 2024/9/1
 * @project RabbitMQ_Starter
 */


public class CompletableFutureExample {


    @Test
    public void testComplete() throws ExecutionException, InterruptedException {
        CompletableFuture<String> future = new CompletableFuture<>();

        System.out.println("主程序正在运行");

        future.complete("hello");
        Thread.sleep(1000);
        System.out.println(future.get());

    }


    public static void main(String[] args) {
        // 创建一个异步执行的CompletableFuture
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            // 模拟耗时的异步操作
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "Interrupted";
            }
            return "Hello, CompletableFuture!";
        });

        // 当异步操作完成时，打印结果
        future.thenAccept(System.out::println);

        // 如果是异步的，那么必然会先输出主线程的内容
        System.out.println("主线程正在运行");

        // 等待异步操作完成（这里只是为了示例，实际使用中通常不需要）
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


}
