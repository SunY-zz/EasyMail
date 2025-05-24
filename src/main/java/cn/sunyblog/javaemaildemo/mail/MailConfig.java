package cn.sunyblog.javaemaildemo.mail;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;
@Data
@Configuration
@ConfigurationProperties(prefix = "mail.imap")
public class MailConfig {
    private String server;
    private String port;
    private String protocol;
    private String username;
    private String password;
    private String attachmentDir;

    // 连接配置
    private Connection connection = new Connection();
    // 监控配置
    private Monitor monitor = new Monitor();
    // 日志配置
    private Log log = new Log();

    @Data
    public static class Connection {
        private int timeout = 30000;
        private int readTimeout = 30000;
        private int writeTimeout = 30000;
    }

    @Data
    public static class Monitor {
        private int idleTimeout = 10000;
        private int keepAliveInterval = 300;
        private int reconnectDelay = 15;
        private int shortDelay = 5;
        private int longDelay = 30;
    }

    @Data
    public static class Log {
        private boolean debugEnabled = false;
    }
}