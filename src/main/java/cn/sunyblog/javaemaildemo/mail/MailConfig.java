package cn.sunyblog.javaemaildemo.mail;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;
@Data
@Configuration
@ConfigurationProperties(prefix = "mail.imap")
public class MailConfig {
    /**
     * 邮件服务器地址
     */
    private String server;
    /**
     * 邮件服务器端口
     */
    private String port;
    /**
     * 邮件协议，例如IMAP
     */
    private String protocol;
    /**
     * 邮件账户用户名
     */
    private String username;
    /**
     * 邮件账户授权码
     */
    private String password;
    /**
     * 附件存储目录，暂时没有使用
     */
    private String attachmentDir;
    /**
     * 连接配置
     */
    private Connection connection = new Connection();
    /**
     * 监控配置
     */
    private Monitor monitor = new Monitor();
    /**
     * 日志配置
     */
    private Log log = new Log();
    /**
     * 监听配置
     */
    private Listener listener = new Listener();

    /**
     * 连接配置类，用于配置邮件连接的超时参数
     */
    @Data
    public static class Connection {
        /**
         * 连接超时时间，默认15秒
         */
        private int timeout = 15000;
        /**
         * 读取超时时间，默认30秒
         */
        private int readTimeout = 30000;
        /**
         * 写入超时时间，默认30秒
         */
        private int writeTimeout = 30000;
    }

    /**
     * 监控配置类，用于配置邮件连接的监控参数
     */
    @Data
    public static class Monitor {
        /**
         * Idle状态超时时间，默认10秒
         */
        private int idleTimeout = 10000;
        /**
         * 保持连接的间隔时间，默认300秒
         */
        private int keepAliveInterval = 300;
        /**
         * 重新连接的延迟时间，默认15秒
         */
        private int reconnectDelay = 15;
        /**
         * 短延迟时间，默认5秒
         */
        private int shortDelay = 5;
        /**
         * 长延迟时间，默认30秒
         */
        private int longDelay = 30;
        /**
         * 任务超时时间，默认300秒
         */
        private int taskTimeout = 300;
    }

    /**
     * 监听配置类，用于配置邮件监听的参数
     */
    @Data
    public static class Listener {
        /**
         * 最大重试次数，默认20次
         */
        private int maxRetries = 20;
    }

    /**
     * 日志配置类，用于配置邮件服务的日志参数
     */
    @Data
    public static class Log {
        /**
         * 是否启用debug日志，默认不启用
         */
        private boolean debugEnabled = false;
    }
}