package cn.sunyblog.easymail.send.template;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * 邮件模板类
 * 支持模板化邮件发送，提供变量替换功能
 *
 * @author suny
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailTemplate {

    /**
     * 模板ID
     */
    private String templateId;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 邮件主题模板
     * 支持变量替换，使用 ${variableName} 格式
     */
    private String subjectTemplate;

    /**
     * 邮件内容模板
     * 支持变量替换，使用 ${variableName} 格式
     */
    private String contentTemplate;

    /**
     * 是否为HTML格式
     */
    private boolean isHtml;

    /**
     * 默认附件列表
     */
    private List<File> defaultAttachments;

    /**
     * 模板描述
     */
    private String description;

    /**
     * 模板版本
     */
    private String version;

    /**
     * 创建时间
     */
    private long createTime;

    /**
     * 更新时间
     */
    private long updateTime;

    /**
     * 替换模板中的变量
     *
     * @param template  模板字符串
     * @param variables 变量映射
     * @return 替换后的字符串
     */
    public static String replaceVariables(String template, Map<String, Object> variables) {
        if (template == null || variables == null || variables.isEmpty()) {
            return template;
        }

        String result = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            result = result.replace(placeholder, value);
        }

        return result;
    }

    /**
     * 根据变量生成邮件主题
     *
     * @param variables 变量映射
     * @return 生成的邮件主题
     */
    public String generateSubject(Map<String, Object> variables) {
        return replaceVariables(subjectTemplate, variables);
    }

    /**
     * 根据变量生成邮件内容
     *
     * @param variables 变量映射
     * @return 生成的邮件内容
     */
    public String generateContent(Map<String, Object> variables) {
        return replaceVariables(contentTemplate, variables);
    }

    /**
     * 验证模板是否有效
     *
     * @return 验证结果
     */
    public boolean isValid() {
        return templateId != null && !templateId.trim().isEmpty() &&
                subjectTemplate != null && !subjectTemplate.trim().isEmpty() &&
                contentTemplate != null && !contentTemplate.trim().isEmpty();
    }

    // ==================== 预定义模板工厂方法 ====================

    /**
     * 创建验证码邮件模板
     *
     * @return 验证码邮件模板
     */
    public static EmailTemplate createVerificationCodeTemplate() {
        return EmailTemplate.builder()
                .templateId("verification_code")
                .templateName("验证码邮件")
                .subjectTemplate("【${appName}】验证码")
                .contentTemplate("您的验证码是：${code}，有效期${expireMinutes}分钟，请勿泄露给他人。")
                .isHtml(false)
                .description("用于发送验证码的邮件模板")
                .version("1.0")
                .createTime(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建欢迎邮件模板
     *
     * @return 欢迎邮件模板
     */
    public static EmailTemplate createWelcomeTemplate() {
        return EmailTemplate.builder()
                .templateId("welcome")
                .templateName("欢迎邮件")
                .subjectTemplate("欢迎加入${appName}")
                .contentTemplate("<h2>欢迎您，${userName}！</h2><p>感谢您注册${appName}，我们很高兴为您服务。</p><p>如有任何问题，请联系我们的客服团队。</p>")
                .isHtml(true)
                .description("用户注册成功后的欢迎邮件")
                .version("1.0")
                .createTime(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建密码重置邮件模板
     *
     * @return 密码重置邮件模板
     */
    public static EmailTemplate createPasswordResetTemplate() {
        return EmailTemplate.builder()
                .templateId("password_reset")
                .templateName("密码重置邮件")
                .subjectTemplate("【${appName}】密码重置")
                .contentTemplate("<h3>密码重置请求</h3><p>您好，${userName}！</p><p>我们收到了您的密码重置请求。请点击以下链接重置您的密码：</p><p><a href='${resetLink}'>重置密码</a></p><p>此链接将在${expireHours}小时后失效。</p><p>如果您没有请求重置密码，请忽略此邮件。</p>")
                .isHtml(true)
                .description("用户密码重置邮件")
                .version("1.0")
                .createTime(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建通知邮件模板
     *
     * @return 通知邮件模板
     */
    public static EmailTemplate createNotificationTemplate() {
        return EmailTemplate.builder()
                .templateId("notification")
                .templateName("通知邮件")
                .subjectTemplate("【${appName}】${notificationType}")
                .contentTemplate("<h3>${title}</h3><p>${content}</p><p>发送时间：${sendTime}</p>")
                .isHtml(true)
                .description("通用通知邮件模板")
                .version("1.0")
                .createTime(System.currentTimeMillis())
                .build();
    }
}