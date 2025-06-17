package cn.sunyblog.easymail.exception;

/**
 * 邮件处理异常类
 * 用于处理邮件接收、解析、处理过程中的异常
 *
 * @author suny
 * @version 1.0
 * @date 2025/06/16
 */
public class EasyMailProcessException extends EasyMailException {

    /**
     * 邮件ID
     */
    private final String messageId;

    /**
     * 处理器名称
     */
    private final String handlerName;

    /**
     * 构造函数
     *
     * @param message 错误消息
     */
    public EasyMailProcessException(String message) {
        super("EMAIL_PROCESS_ERROR", message);
        this.messageId = null;
        this.handlerName = null;
    }

    /**
     * 构造函数
     *
     * @param message 错误消息
     * @param cause   原因异常
     */
    public EasyMailProcessException(String message, Throwable cause) {
        super("EMAIL_PROCESS_ERROR", message, cause);
        this.messageId = null;
        this.handlerName = null;
    }

    /**
     * 构造函数
     *
     * @param message     错误消息
     * @param messageId   邮件ID
     * @param handlerName 处理器名称
     */
    public EasyMailProcessException(String message, String messageId, String handlerName) {
        super("EMAIL_PROCESS_ERROR", message, buildErrorDetail(messageId, handlerName));
        this.messageId = messageId;
        this.handlerName = handlerName;
    }

    /**
     * 构造函数
     *
     * @param message     错误消息
     * @param messageId   邮件ID
     * @param handlerName 处理器名称
     * @param cause       原因异常
     */
    public EasyMailProcessException(String message, String messageId, String handlerName, Throwable cause) {
        super("EMAIL_PROCESS_ERROR", message, buildErrorDetail(messageId, handlerName), cause);
        this.messageId = messageId;
        this.handlerName = handlerName;
    }

    /**
     * 构造函数
     *
     * @param errorCode   错误代码
     * @param message     错误消息
     * @param messageId   邮件ID
     * @param handlerName 处理器名称
     */
    public EasyMailProcessException(String errorCode, String message, String messageId, String handlerName) {
        super(errorCode, message, buildErrorDetail(messageId, handlerName));
        this.messageId = messageId;
        this.handlerName = handlerName;
    }

    /**
     * 构造函数
     *
     * @param errorCode   错误代码
     * @param message     错误消息
     * @param messageId   邮件ID
     * @param handlerName 处理器名称
     * @param cause       原因异常
     */
    public EasyMailProcessException(String errorCode, String message, String messageId, String handlerName, Throwable cause) {
        super(errorCode, message, buildErrorDetail(messageId, handlerName), cause);
        this.messageId = messageId;
        this.handlerName = handlerName;
    }

    /**
     * 构建错误详细信息
     *
     * @param messageId   邮件ID
     * @param handlerName 处理器名称
     * @return 错误详细信息
     */
    private static String buildErrorDetail(String messageId, String handlerName) {
        StringBuilder sb = new StringBuilder();
        if (messageId != null && !messageId.trim().isEmpty()) {
            sb.append("邮件ID: ").append(messageId);
        }
        if (handlerName != null && !handlerName.trim().isEmpty()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("处理器: ").append(handlerName);
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    /**
     * 获取邮件ID
     *
     * @return 邮件ID
     */
    public String getMessageId() {
        return messageId;
    }

    /**
     * 获取处理器名称
     *
     * @return 处理器名称
     */
    public String getHandlerName() {
        return handlerName;
    }

    /**
     * 创建邮件解析异常
     *
     * @param messageId 邮件ID
     * @param cause     原因异常
     * @return EmailProcessException实例
     */
    public static EasyMailProcessException parseError(String messageId, Throwable cause) {
        return new EasyMailProcessException("EMAIL_PARSE_ERROR",
                "邮件解析失败", messageId, null, cause);
    }

    /**
     * 创建处理器执行异常
     *
     * @param handlerName 处理器名称
     * @param messageId   邮件ID
     * @param cause       原因异常
     * @return EmailProcessException实例
     */
    public static EasyMailProcessException handlerExecutionError(String handlerName, String messageId, Throwable cause) {
        return new EasyMailProcessException("EMAIL_HANDLER_EXECUTION_ERROR",
                "邮件处理器执行失败", messageId, handlerName, cause);
    }

    /**
     * 创建处理器不存在异常
     *
     * @param handlerName 处理器名称
     * @return EmailProcessException实例
     */
    public static EasyMailProcessException handlerNotFound(String handlerName) {
        return new EasyMailProcessException("EMAIL_HANDLER_NOT_FOUND",
                "邮件处理器不存在", null, handlerName);
    }

    /**
     * 创建通用处理异常
     *
     * @param message 错误消息
     * @param cause   原因异常
     * @return EmailProcessException实例
     */
    public static EasyMailProcessException processingError(String message, Throwable cause) {
        return new EasyMailProcessException(message, cause);
    }

    /**
     * 创建解析异常
     *
     * @param message 错误消息
     * @param cause   原因异常
     * @return EmailProcessException实例
     */
    public static EasyMailProcessException parsingError(String message, Throwable cause) {
        return new EasyMailProcessException(message, cause);
    }
}