package cn.sunyblog.easymail.exception;

/**
 * 邮件处理基础异常类
 * 所有邮件相关异常的父类
 *
 * @author suny
 * @version 1.0
 * @date 2025/06/16
 */
public class EmailException extends RuntimeException {

    /**
     * 错误代码
     */
    private final String errorCode;

    /**
     * 错误详细信息
     */
    private final String errorDetail;

    /**
     * 构造函数
     *
     * @param message 错误消息
     */
    public EmailException(String message) {
        super(message);
        this.errorCode = "EMAIL_ERROR";
        this.errorDetail = null;
    }

    /**
     * 构造函数
     *
     * @param message 错误消息
     * @param cause   原因异常
     */
    public EmailException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "EMAIL_ERROR";
        this.errorDetail = null;
    }

    /**
     * 构造函数
     *
     * @param errorCode 错误代码
     * @param message   错误消息
     */
    public EmailException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.errorDetail = null;
    }

    /**
     * 构造函数
     *
     * @param errorCode 错误代码
     * @param message   错误消息
     * @param cause     原因异常
     */
    public EmailException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.errorDetail = null;
    }

    /**
     * 构造函数
     *
     * @param errorCode   错误代码
     * @param message     错误消息
     * @param errorDetail 错误详细信息
     */
    public EmailException(String errorCode, String message, String errorDetail) {
        super(message);
        this.errorCode = errorCode;
        this.errorDetail = errorDetail;
    }

    /**
     * 构造函数
     *
     * @param errorCode   错误代码
     * @param message     错误消息
     * @param errorDetail 错误详细信息
     * @param cause       原因异常
     */
    public EmailException(String errorCode, String message, String errorDetail, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.errorDetail = errorDetail;
    }

    /**
     * 获取错误代码
     *
     * @return 错误代码
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * 获取错误详细信息
     *
     * @return 错误详细信息
     */
    public String getErrorDetail() {
        return errorDetail;
    }

    /**
     * 获取完整的错误信息
     *
     * @return 完整的错误信息
     */
    public String getFullErrorMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("[错误代码: ").append(errorCode).append("] ");
        sb.append(getMessage());
        if (errorDetail != null && !errorDetail.trim().isEmpty()) {
            sb.append(" - 详细信息: ").append(errorDetail);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return getFullErrorMessage();
    }
}