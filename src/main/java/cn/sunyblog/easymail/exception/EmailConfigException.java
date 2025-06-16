package cn.sunyblog.easymail.exception;

/**
 * 邮件配置异常类
 * 用于处理邮件配置相关的异常情况
 *
 * @author suny
 * @version 1.0
 * @date 2025/06/16
 */
public class EmailConfigException extends EmailException {

    /**
     * 配置项名称
     */
    private final String configKey;

    /**
     * 配置项值
     */
    private final String configValue;

    /**
     * 构造函数
     *
     * @param message 错误消息
     */
    public EmailConfigException(String message) {
        super("EMAIL_CONFIG_ERROR", message);
        this.configKey = null;
        this.configValue = null;
    }

    /**
     * 构造函数
     *
     * @param message 错误消息
     * @param cause   原因异常
     */
    public EmailConfigException(String message, Throwable cause) {
        super("EMAIL_CONFIG_ERROR", message, cause);
        this.configKey = null;
        this.configValue = null;
    }

    /**
     * 构造函数
     *
     * @param message   错误消息
     * @param configKey 配置项名称
     */
    public EmailConfigException(String message, String configKey) {
        super("EMAIL_CONFIG_ERROR", message, buildErrorDetail(configKey, null));
        this.configKey = configKey;
        this.configValue = null;
    }

    /**
     * 构造函数
     *
     * @param message     错误消息
     * @param configKey   配置项名称
     * @param configValue 配置项值
     */
    public EmailConfigException(String message, String configKey, String configValue) {
        super("EMAIL_CONFIG_ERROR", message, buildErrorDetail(configKey, configValue));
        this.configKey = configKey;
        this.configValue = configValue;
    }


    /**
     * 构造函数
     *
     * @param errorCode   错误代码
     * @param message     错误消息
     * @param configKey   配置项名称
     * @param configValue 配置项值
     */
    public EmailConfigException(String errorCode, String message, String configKey, String configValue) {
        super(errorCode, message, buildErrorDetail(configKey, configValue));
        this.configKey = configKey;
        this.configValue = configValue;
    }

    /**
     * 构建错误详细信息
     *
     * @param configKey   配置项名称
     * @param configValue 配置项值
     * @return 错误详细信息
     */
    private static String buildErrorDetail(String configKey, String configValue) {
        StringBuilder sb = new StringBuilder();
        if (configKey != null && !configKey.trim().isEmpty()) {
            sb.append("配置项: ").append(configKey);
        }
        if (configValue != null && !configValue.trim().isEmpty()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("配置值: ").append(configValue);
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    /**
     * 获取配置项名称
     *
     * @return 配置项名称
     */
    public String getConfigKey() {
        return configKey;
    }

    /**
     * 获取配置项值
     *
     * @return 配置项值
     */
    public String getConfigValue() {
        return configValue;
    }

    /**
     * 创建配置缺失异常
     *
     * @param configKey 配置项名称
     * @return EmailConfigException实例
     */
    public static EmailConfigException missingConfig(String configKey) {
        return new EmailConfigException("EMAIL_CONFIG_MISSING",
                "邮件配置项缺失", configKey);
    }

    /**
     * 创建配置无效异常
     *
     * @param configKey   配置项名称
     * @param configValue 配置项值
     * @return EmailConfigException实例
     */
    public static EmailConfigException invalidConfig(String configKey, String configValue) {
        return new EmailConfigException("EMAIL_CONFIG_INVALID",
                "邮件配置项无效", configKey, configValue);
    }

    /**
     * 创建SMTP服务器配置异常
     *
     * @param server 服务器地址
     * @param port   端口号
     * @return EmailConfigException实例
     */
    public static EmailConfigException invalidSmtpConfig(String server, Integer port) {
        String detail = String.format("SMTP服务器: %s, 端口: %d", server, port);
        return new EmailConfigException("EMAIL_SMTP_CONFIG_INVALID",
                "SMTP服务器配置无效", "smtp.server", detail);
    }

    /**
     * 创建认证配置异常
     *
     * @param username 用户名
     * @return EmailConfigException实例
     */
    public static EmailConfigException invalidAuthConfig(String username) {
        return new EmailConfigException("EMAIL_AUTH_CONFIG_INVALID",
                "邮件认证配置无效", "smtp.username", username);
    }
}