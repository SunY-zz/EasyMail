package cn.sunyblog.easymail.exception;

import java.util.List;
import java.util.ArrayList;

/**
 * 邮件验证异常类
 * 用于处理邮件内容、格式等验证相关异常
 *
 * @author suny
 * @version 1.0
 * @date 2025/06/16
 */
public class EmailValidationException extends EmailException {

    /**
     * 验证错误列表
     */
    private final List<String> validationErrors;

    /**
     * 构造函数
     *
     * @param message 错误消息
     */
    public EmailValidationException(String message) {
        super("EMAIL_VALIDATION_ERROR", message);
        this.validationErrors = new ArrayList<>();
    }

    /**
     * 构造函数
     *
     * @param message          错误消息
     * @param validationErrors 验证错误列表
     */
    public EmailValidationException(String message, List<String> validationErrors) {
        super("EMAIL_VALIDATION_ERROR", message, buildErrorDetail(validationErrors));
        this.validationErrors = validationErrors != null ? new ArrayList<>(validationErrors) : new ArrayList<>();
    }

    /**
     * 构造函数
     *
     * @param errorCode        错误代码
     * @param message          错误消息
     * @param validationErrors 验证错误列表
     */
    public EmailValidationException(String errorCode, String message, List<String> validationErrors) {
        super(errorCode, message, buildErrorDetail(validationErrors));
        this.validationErrors = validationErrors != null ? new ArrayList<>(validationErrors) : new ArrayList<>();
    }

    /**
     * 构建错误详细信息
     *
     * @param validationErrors 验证错误列表
     * @return 错误详细信息
     */
    private static String buildErrorDetail(List<String> validationErrors) {
        if (validationErrors == null || validationErrors.isEmpty()) {
            return null;
        }
        return "验证错误: " + String.join("; ", validationErrors);
    }

    /**
     * 获取验证错误列表
     *
     * @return 验证错误列表
     */
    public List<String> getValidationErrors() {
        return new ArrayList<>(validationErrors);
    }

    /**
     * 添加验证错误
     *
     * @param error 验证错误
     */
    public void addValidationError(String error) {
        if (error != null && !error.trim().isEmpty()) {
            this.validationErrors.add(error.trim());
        }
    }

    /**
     * 检查是否有验证错误
     *
     * @return 如果有验证错误返回true
     */
    public boolean hasValidationErrors() {
        return !validationErrors.isEmpty();
    }

    /**
     * 获取验证错误数量
     *
     * @return 验证错误数量
     */
    public int getValidationErrorCount() {
        return validationErrors.size();
    }

    /**
     * 创建空内容异常
     *
     * @return EmailValidationException实例
     */
    public static EmailValidationException emptyContent() {
        return new EmailValidationException("EMAIL_EMPTY_CONTENT", "邮件内容不能为空", null);
    }

    /**
     * 创建空主题异常
     *
     * @return EmailValidationException实例
     */
    public static EmailValidationException emptySubject() {
        return new EmailValidationException("EMAIL_EMPTY_SUBJECT", "邮件主题不能为空", null);
    }

    /**
     * 创建空收件人异常
     *
     * @return EmailValidationException实例
     */
    public static EmailValidationException emptyRecipients() {
        return new EmailValidationException("EMAIL_EMPTY_RECIPIENTS", "收件人列表不能为空", null);
    }

    /**
     * 创建无效邮箱地址异常
     *
     * @param invalidEmails 无效的邮箱地址列表
     * @return EmailValidationException实例
     */
    public static EmailValidationException invalidEmailAddresses(List<String> invalidEmails) {
        List<String> errors = new ArrayList<>();
        if (invalidEmails != null) {
            for (String email : invalidEmails) {
                errors.add("无效的邮箱地址: " + email);
            }
        }
        return new EmailValidationException("EMAIL_INVALID_ADDRESS", "邮箱地址格式不正确", errors);
    }

    /**
     * 创建附件不存在异常
     *
     * @param missingFiles 不存在的文件列表
     * @return EmailValidationException实例
     */
    public static EmailValidationException missingAttachments(List<String> missingFiles) {
        List<String> errors = new ArrayList<>();
        if (missingFiles != null) {
            for (String file : missingFiles) {
                errors.add("附件文件不存在: " + file);
            }
        }
        return new EmailValidationException("EMAIL_MISSING_ATTACHMENT", "附件文件不存在", errors);
    }
}