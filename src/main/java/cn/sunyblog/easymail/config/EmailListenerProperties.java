package cn.sunyblog.easymail.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 邮件监听器配置属性
 */
@Data
@ConfigurationProperties(prefix = "email.listener")
public class EmailListenerProperties {

    /**
     * 是否启用邮件监听器
     */
    private boolean enabled = true;

    /**
     * 邮件服务器配置
     */
    private Server server = new Server();

    /**
     * 连接配置
     */
    private Connection connection = new Connection();

    /**
     * 监控配置
     */
    private Monitor monitor = new Monitor();

    /**
     * 监听配置
     */
    private Listener listener = new Listener();

    /**
     * 日志配置
     */
    private Log log = new Log();

    /**
     * 附件配置
     */
    private Attachment attachment = new Attachment();

    /**
     * 邮件服务器配置
     */
    @Data
    public static class Server {
        /**
         * 邮件服务器地址
         */
        private String host;

        /**
         * 邮件服务器端口
         */
        private String port;

        /**
         * 邮件协议，例如imaps
         */
        private String protocol = "imaps";

        /**
         * 邮件账户用户名
         */
        private String username;

        /**
         * 邮件账户授权码
         */
        private String password;

        /**
         * 邮件文件夹，默认为收件箱
         */
        private String folder = "INBOX";
    }

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

        /**
         * 是否信任所有SSL证书
         */
        private boolean trustAllCerts = true;
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

        /**
         * 是否自动启动
         */
        private boolean autoStart = true;

        /**
         * 是否处理现有未读邮件
         */
        private boolean processExistingUnread = true;

        /**
         * 线程池核心线程数
         */
        private int corePoolSize = 16;

        /**
         * 线程池最大线程数
         */
        private int maxPoolSize = 50;

        /**
         * 线程池队列容量
         */
        private int queueCapacity = 100;
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

    /**
     * 附件配置类
     */
    @Data
    public static class Attachment {
        /**
         * 附件存储目录
         */
        private String saveDir;

        /**
         * 是否保存附件
         */
        private boolean saveAttachments = true;

        /**
         * 是否使用唯一文件名
         */
        private boolean useUniqueFilename = true;
    }
}