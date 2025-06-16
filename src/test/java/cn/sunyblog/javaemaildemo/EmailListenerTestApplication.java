package cn.sunyblog.javaemaildemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 邮件监听器测试应用
 * 用于测试email.listener配置格式
 */
@SpringBootApplication
public class EmailListenerTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmailListenerTestApplication.class, args);
    }
}