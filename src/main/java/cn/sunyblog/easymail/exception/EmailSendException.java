package cn.sunyblog.easymail.exception;

/**
 * 邮件发送异常类
 * 用于处理邮件发送过程中的各种异常情况
 *
 * @author suny
 * @version 1.0
 * @date 2025/06/16
 */
public class EmailSendException extends EmailException {

    /**
     * 构造函数
     *
     * @param message 错误消息
     */
    public EmailSendException(String message) {
        super("EMAIL_SEND_ERROR", message);
    }

    /**
     * 构造函数
     *
     * @param message 错误消息
     * @param cause   原因异常
     */
    public EmailSendException(String message, Throwable cause) {
        super("EMAIL_SEND_ERROR", message, cause);
    }

    /**
     * 构造函数
     *
     * @param errorCode 错误代码
     * @param message   错误消息
     */
    public EmailSendException(String errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * 构造函数
     *
     * @param errorCode 错误代码
     * @param message   错误消息
     * @param cause     原因异常
     */
    public EmailSendException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    /**
     * 构造函数
     *
     * @param errorCode   错误代码
     * @param message     错误消息
     * @param errorDetail 错误详细信息
     */
    public EmailSendException(String errorCode, String message, String errorDetail) {
        super(errorCode, message, errorDetail);
    }

    /**
     * 构造函数
     *
     * @param errorCode   错误代码
     * @param message     错误消息
     * @param errorDetail 错误详细信息
     * @param cause       原因异常
     */
    public EmailSendException(String errorCode, String message, String errorDetail, Throwable cause) {
        super(errorCode, message, errorDetail, cause);
    }
}