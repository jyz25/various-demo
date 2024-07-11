package org.example.util;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * @auther Kindow
 * @date 2024/7/11
 * @project springboot-weixin-qrcode-login
 */
public class Base64Test {


    public static void main(String[] args) throws UnsupportedEncodingException {
        String s1 = "Hello";
        byte[] bytes = s1.getBytes(StandardCharsets.US_ASCII);
        System.out.println("Hello字符串的ASCII表示：");
        char[] chars = s1.toCharArray();
        for (byte c : bytes) {
            System.out.print(c + " ");
        }
        String s = Base64.getEncoder().encodeToString(bytes);
        System.out.println();
        System.out.println("二进制数以Base64编码后的字符串是：");
        System.out.println(s);

    }
}
