package cn.sunyblog.easymail.exception;

/**
 * 邮件连接异常类
 * 用于处理SMTP连接、认证等网络相关异常
 *
 * @author suny
 * @version 1.0
 * @date 2025/06/16
 */
public class EasyMailConnectionException extends EasyMailException {

    /**
     * 服务器地址
     */
    private final String server;

    /**
     * 端口号
     */
    private final Integer port;

    /**
     * 构造函数
     *
     * @param message 错误消息
     */
    public EasyMailConnectionException(String message) {
        super("EMAIL_CONNECTION_ERROR", message);
        this.server = null;
        this.port = null;
    }

    /**
     * 构造函数
     *
     * @param message 错误消息
     * @param cause   原因异常
     */
    public EasyMailConnectionException(String message, Throwable cause) {
        super("EMAIL_CONNECTION_ERROR", message, cause);
        this.server = null;
        this.port = null;
    }

    /**
     * 构造函数
     *
     * @param message 错误消息
     * @param server  服务器地址
     * @param port    端口号
     */
    public EasyMailConnectionException(String message, String server, Integer port) {
        super("EMAIL_CONNECTION_ERROR", message, buildErrorDetail(server, port));
        this.server = server;
        this.port = port;
    }

    /**
     * 构造函数
     *
     * @param message 错误消息
     * @param server  服务器地址
     * @param port    端口号
     * @param cause   原因异常
     */
    public EasyMailConnectionException(String message, String server, Integer port, Throwable cause) {
        super("EMAIL_CONNECTION_ERROR", message, buildErrorDetail(server, port), cause);
        this.server = server;
        this.port = port;
    }

    /**
     * 构造函数
     *
     * @param errorCode 错误代码
     * @param message   错误消息
     * @param server    服务器地址
     * @param port      端口号
     */
    public EasyMailConnectionException(String errorCode, String message, String server, Integer port) {
        super(errorCode, message, buildErrorDetail(server, port));
        this.server = server;
        this.port = port;
    }

    /**
     * 构造函数
     *
     * @param errorCode 错误代码
     * @param message   错误消息
     * @param server    服务器地址
     * @param port      端口号
     * @param cause     原因异常
     */
    public EasyMailConnectionException(String errorCode, String message, String server, Integer port, Throwable cause) {
        super(errorCode, message, buildErrorDetail(server, port), cause);
        this.server = server;
        this.port = port;
    }

    /**
     * 构建错误详细信息
     *
     * @param server 服务器地址
     * @param port   端口号
     * @return 错误详细信息
     */
    private static String buildErrorDetail(String server, Integer port) {
        if (server != null && port != null) {
            return String.format("服务器: %s, 端口: %d", server, port);
        } else if (server != null) {
            return String.format("服务器: %s", server);
        } else if (port != null) {
            return String.format("端口: %d", port);
        }
        return null;
    }

    /**
     * 获取服务器地址
     *
     * @return 服务器地址
     */
    public String getServer() {
        return server;
    }

    /**
     * 获取端口号
     *
     * @return 端口号
     */
    public Integer getPort() {
        return port;
    }
}