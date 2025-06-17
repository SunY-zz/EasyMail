package cn.sunyblog.easymail.exception;


/**
 * 邮件模板异常类
 * 用于处理邮件模板相关的异常情况
 *
 * @author suny
 * @version 1.0
 * @date 2025/06/16
 */
public class EasyMailTemplateException extends EasyMailException {

    /**
     * 模板ID
     */
    private final String templateId;

    /**
     * 构造函数
     *
     * @param message 错误消息
     */
    public EasyMailTemplateException(String message) {
        super("EMAIL_TEMPLATE_ERROR", message);
        this.templateId = null;
    }

    /**
     * 构造函数
     *
     * @param message 错误消息
     * @param cause   原因异常
     */
    public EasyMailTemplateException(String message, Throwable cause) {
        super("EMAIL_TEMPLATE_ERROR", message, cause);
        this.templateId = null;
    }

    /**
     * 构造函数
     *
     * @param message    错误消息
     * @param templateId 模板ID
     */
    public EasyMailTemplateException(String message, String templateId) {
        super("EMAIL_TEMPLATE_ERROR", message, buildErrorDetail(templateId));
        this.templateId = templateId;
    }

    /**
     * 构造函数
     *
     * @param message    错误消息
     * @param templateId 模板ID
     * @param cause      原因异常
     */
    public EasyMailTemplateException(String message, String templateId, Throwable cause) {
        super("EMAIL_TEMPLATE_ERROR", message, buildErrorDetail(templateId), cause);
        this.templateId = templateId;
    }

    /**
     * 构造函数
     *
     * @param errorCode  错误代码
     * @param message    错误消息
     * @param templateId 模板ID
     */
    public EasyMailTemplateException(String errorCode, String message, String templateId) {
        super(errorCode, message, buildErrorDetail(templateId));
        this.templateId = templateId;
    }

    /**
     * 构造函数
     *
     * @param errorCode  错误代码
     * @param message    错误消息
     * @param templateId 模板ID
     * @param cause      原因异常
     */
    public EasyMailTemplateException(String errorCode, String message, String templateId, Throwable cause) {
        super(errorCode, message, buildErrorDetail(templateId), cause);
        this.templateId = templateId;
    }

    /**
     * 构建错误详细信息
     *
     * @param templateId 模板ID
     * @return 错误详细信息
     */
    private static String buildErrorDetail(String templateId) {
        if (templateId != null && !templateId.trim().isEmpty()) {
            return "模板ID: " + templateId;
        }
        return null;
    }

    public static Exception invalidEmailTemplate() {
        return new EasyMailTemplateException("EMAIL_TEMPLATE_INVALID",
                "邮件模板不能为空");
    }

    public static Exception invalidBathEmailTemplate() {
        return new EasyMailTemplateException("EMAIL_TEMPLATE_INVALID",
                "批量邮件模板不能为空");
    }

    /**
     * 获取模板ID
     *
     * @return 模板ID
     */
    public String getTemplateId() {
        return templateId;
    }

    /**
     * 创建模板不存在异常
     *
     * @param templateId 模板ID
     * @return EmailTemplateException实例
     */
    public static EasyMailTemplateException templateNotFound(String templateId) {
        return new EasyMailTemplateException("EMAIL_TEMPLATE_NOT_FOUND",
                "邮件模板不存在", templateId);
    }

    /**
     * 创建模板解析异常
     *
     * @param templateId 模板ID
     * @param cause      原因异常
     * @return EmailTemplateException实例
     */
    public static EasyMailTemplateException templateParseError(String templateId, Throwable cause) {
        return new EasyMailTemplateException("EMAIL_TEMPLATE_PARSE_ERROR",
                "邮件模板解析失败", templateId, cause);
    }

    /**
     * 创建模板变量缺失异常
     *
     * @param templateId      模板ID
     * @param missingVariable 缺失的变量名
     * @return EmailTemplateException实例
     */
    public static EasyMailTemplateException missingVariable(String templateId, String missingVariable) {
        return new EasyMailTemplateException("EMAIL_TEMPLATE_MISSING_VARIABLE",
                "模板变量缺失: " + missingVariable, templateId);
    }

    /**
     * 创建模板渲染异常
     *
     * @param templateId 模板ID
     * @param cause      原因异常
     * @return EmailTemplateException实例
     */
    public static EasyMailTemplateException renderError(String templateId, Throwable cause) {
        return new EasyMailTemplateException("EMAIL_TEMPLATE_RENDER_ERROR",
                "邮件模板渲染失败", templateId, cause);
    }
}