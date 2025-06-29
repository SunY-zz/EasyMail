package cn.sunyblog.easymail.api;

import cn.sunyblog.easymail.exception.EasyMailValidationException;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 邮件请求构建器
 * 提供链式调用的友好API，简化邮件发送的使用方式
 * <p>
 * 使用示例：
 * <pre>
 * EmailRequest request = EmailRequest.builder()
 *     .to("recipient@example.com")
 *     .subject("测试邮件")
 *     .text("这是邮件内容")
 *     .build();
 *
 * SendResult result = emailSenderService.send(request);
 * </pre>
 *
 * @author suny
 * @version 1.0.0
 */
@Data
@Builder
public class EasyMailRequest {

    /**
     * 收件人列表（TO）
     */
    @Singular("toList")
    private List<String> toList;

    /**
     * 单个收件人（便捷字段，会自动转换为toList）
     */
    private String to;

    /**
     * 抄送人列表（CC）
     */
    @Singular("cc")
    private List<String> ccList;

    /**
     * 密送人列表（BCC）
     */
    @Singular("bcc")
    private List<String> bccList;

    /**
     * 邮件主题
     */
    private String subject;

    /**
     * 文本内容（纯文本格式）
     */
    private String text;

    /**
     * HTML内容（HTML格式）
     */
    private String html;

    /**
     * 附件列表
     */
    @Singular("attachment")
    private List<File> attachments;

    /**
     * 模板ID（用于模板邮件）
     */
    private String templateId;

    /**
     * 模板变量（用于模板邮件）
     */
    @Singular("variable")
    private Map<String, Object> templateVariables;

    /**
     * 邮件优先级（1-5，1为最高优先级）
     */
    @Builder.Default
    private Integer priority = 3;

    /**
     * 是否异步发送
     */
    @Builder.Default
    private boolean async = false;

    /**
     * 发送策略（可选：default, batch, high_priority）
     */
    private String strategy;

    /**
     * 自定义邮件头
     */
    @Singular("header")
    private Map<String, String> customHeaders;

    /**
     * 验证邮件请求的有效性
     *
     * @return 验证结果
     */
    public ValidationResult validate() {
        ValidationResult result = new ValidationResult();

        // 处理单个收件人字段
        List<String> actualToList = getActualToList();

        // 检查收件人
        if (actualToList == null || actualToList.isEmpty()) {
            result.addError("收件人列表不能为空");
        } else {
            // 验证收件人邮箱格式
            for (String email : actualToList) {
                if (!isValidEmail(email)) {
                    result.addError("收件人邮箱格式无效: " + email);
                }
            }
        }

        // 检查抄送人邮箱格式
        if (ccList != null) {
            for (String email : ccList) {
                if (!isValidEmail(email)) {
                    result.addError("抄送人邮箱格式无效: " + email);
                }
            }
        }

        // 检查密送人邮箱格式
        if (bccList != null) {
            for (String email : bccList) {
                if (!isValidEmail(email)) {
                    result.addError("密送人邮箱格式无效: " + email);
                }
            }
        }

        // 检查主题
        if (subject == null || subject.trim().isEmpty()) {
            result.addError("邮件主题不能为空");
        }

        // 检查内容
        if ((text == null || text.trim().isEmpty()) &&
                (html == null || html.trim().isEmpty()) &&
                (templateId == null || templateId.trim().isEmpty())) {
            result.addError("邮件内容、HTML内容或模板ID至少需要指定一个");
        }

        // 检查模板相关
        if (templateId != null && !templateId.trim().isEmpty()) {
            if (templateVariables == null || templateVariables.isEmpty()) {
                result.addError("使用模板时必须提供模板变量");
            }
        }

        // 检查附件
        if (attachments != null) {
            for (File attachment : attachments) {
                if (attachment == null) {
                    result.addError("附件不能为null");
                } else if (!attachment.exists()) {
                    result.addError("附件文件不存在: " + attachment.getPath());
                } else if (!attachment.isFile()) {
                    result.addError("附件必须是文件: " + attachment.getPath());
                } else if (!attachment.canRead()) {
                    result.addError("附件文件无法读取: " + attachment.getPath());
                }
            }
        }

        // 检查优先级
        if (priority != null && (priority < 1 || priority > 5)) {
            result.addError("邮件优先级必须在1-5之间");
        }

        return result;
    }

    /**
     * 验证邮件请求并抛出异常
     *
     * @throws EasyMailValidationException 验证失败时抛出
     */
    public void validateAndThrow() throws EasyMailValidationException {
        ValidationResult result = validate();
        if (!result.isValid()) {
            throw new EasyMailValidationException(
                    "邮件请求验证失败", result.getErrors());
        }
    }

    /**
     * 验证邮箱地址格式
     *
     * @param email 邮箱地址
     * @return 是否有效
     */
    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return email.matches(emailRegex);
    }

    /**
     * 判断是否为HTML邮件
     *
     * @return 如果有HTML内容则返回true
     */
    public boolean isHtml() {
        return html != null && !html.trim().isEmpty();
    }

    /**
     * 判断是否为模板邮件
     *
     * @return 如果指定了模板ID则返回true
     */
    public boolean isTemplate() {
        return templateId != null && !templateId.trim().isEmpty();
    }

    /**
     * 获取邮件内容（优先返回HTML，其次返回文本）
     *
     * @return 邮件内容
     */
    public String getContent() {
        if (isHtml()) {
            return html;
        }
        return text;
    }

    /**
     * 验证结果类
     */
    @Data
    public static class ValidationResult {
        private boolean valid = true;
        private List<String> errors = new java.util.ArrayList<>();

        public void addError(String error) {
            this.valid = false;
            this.errors.add(error);
        }

        public String getErrorMessage() {
            return String.join("; ", errors);
        }
    }

    /**
     * 便捷方法：创建简单文本邮件
     *
     * @param to      收件人
     * @param subject 主题
     * @param text    文本内容
     * @return EmailRequest实例
     */
    public static EasyMailRequest simpleText(String to, String subject, String text) {
        return EasyMailRequest.builder()
                .to(to)
                .subject(subject)
                .text(text)
                .build();
    }

    /**
     * 便捷方法：创建HTML邮件
     *
     * @param to      收件人
     * @param subject 主题
     * @param html    HTML内容
     * @return EmailRequest实例
     */
    public static EasyMailRequest htmlEmail(String to, String subject, String html) {
        return EasyMailRequest.builder()
                .to(to)
                .subject(subject)
                .html(html)
                .build();
    }

    /**
     * 便捷方法：创建HTML模板邮件（从文件路径）
     * 注意：此方法创建的是文件路径模板，需要特殊处理
     *
     * @param to           收件人
     * @param subject      主题
     * @param templatePath HTML模板文件路径
     * @param variables    模板变量
     * @return EmailRequest实例
     */
    public static EasyMailRequest htmlTemplate(String to, String subject, String templatePath, Map<String, Object> variables) {
        // 使用特殊前缀标识这是文件路径模板
        String fileTemplateId = "file:" + templatePath;
        return EasyMailRequest.builder()
                .to(to)
                .subject(subject)
                .templateId(fileTemplateId)
                .templateVariables(variables)
                .build();
    }

    /**
     * 便捷方法：创建模板邮件
     *
     * @param to         收件人
     * @param templateId 模板ID
     * @param variables  模板变量
     * @return EmailRequest实例
     */
    public static EasyMailRequest templateEmail(String to, String templateId, Map<String, Object> variables) {
        return EasyMailRequest.builder()
                .to(to)
                .templateId(templateId)
                .templateVariables(variables)
                .build();
    }

    /**
     * 获取实际的收件人列表（处理单个收件人字段）
     *
     * @return 实际的收件人列表
     */
    public List<String> getActualToList() {
        if (toList != null && !toList.isEmpty()) {
            return toList;
        }
        if (to != null && !to.trim().isEmpty()) {
            return Collections.singletonList(to);
        }
        return toList;
    }

    /**
     * 便捷方法：创建带附件的邮件
     *
     * @param to          收件人
     * @param subject     主题
     * @param content     内容
     * @param isHtml      是否为HTML
     * @param attachments 附件列表
     * @return EmailRequest实例
     */
    public static EasyMailRequest withAttachments(String to, String subject, String content, boolean isHtml, List<File> attachments) {
        EasyMailRequestBuilder builder = EasyMailRequest.builder()
                .to(to)
                .subject(subject);

        if (isHtml) {
            builder.html(content);
        } else {
            builder.text(content);
        }

        if (attachments != null) {
            attachments.forEach(builder::attachment);
        }

        return builder.build();
    }

    /**
     * 便捷方法：创建多收件人邮件
     *
     * @param toList  收件人列表
     * @param subject 主题
     * @param content 内容
     * @param isHtml  是否为HTML
     * @return EmailRequest实例
     */
    public static EasyMailRequest toMultiple(List<String> toList, String subject, String content, boolean isHtml) {
        EasyMailRequestBuilder builder = EasyMailRequest.builder()
                .subject(subject);

        if (toList != null) {
            toList.forEach(builder::to);
        }

        if (isHtml) {
            builder.html(content);
        } else {
            builder.text(content);
        }

        return builder.build();
    }

    /**
     * 便捷方法：创建完整邮件（支持TO、CC、BCC）
     *
     * @param toList      收件人列表
     * @param ccList      抄送人列表
     * @param bccList     密送人列表
     * @param subject     主题
     * @param content     内容
     * @param isHtml      是否为HTML
     * @param attachments 附件列表
     * @return EmailRequest实例
     */
    public static EasyMailRequest fullEmail(List<String> toList, List<String> ccList, List<String> bccList,
                                            String subject, String content, boolean isHtml, List<File> attachments) {
        EasyMailRequestBuilder builder = EasyMailRequest.builder()
                .subject(subject);

        if (toList != null) {
            toList.forEach(builder::to);
        }
        if (ccList != null) {
            ccList.forEach(builder::cc);
        }
        if (bccList != null) {
            bccList.forEach(builder::bcc);
        }

        if (isHtml) {
            builder.html(content);
        } else {
            builder.text(content);
        }

        if (attachments != null) {
            attachments.forEach(builder::attachment);
        }

        return builder.build();
    }
}