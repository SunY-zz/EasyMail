package cn.sunyblog.easymail.send.template;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 邮件模板管理器
 * 提供邮件模板的注册、获取、缓存等功能
 *
 * @author suny
 * @version 1.0.0
 */
@Slf4j
@Component
public class EasyMailSendTemplateManager {

    /**
     * 模板缓存
     */
    private static final Map<String, EasyMailSendTemplate> templateCache = new ConcurrentHashMap<>();

    /**
     * 初始化默认模板
     */
    public void initDefaultTemplates() {
        // 注册预定义模板
        registerTemplate(EasyMailSendTemplate.createVerificationCodeTemplate());
        registerTemplate(EasyMailSendTemplate.createWelcomeTemplate());
        registerTemplate(EasyMailSendTemplate.createPasswordResetTemplate());
        registerTemplate(EasyMailSendTemplate.createNotificationTemplate());

        log.info("已初始化 {} 个默认邮件模板", templateCache.size());
    }

    /**
     * 注册邮件模板
     *
     * @param template 邮件模板
     * @return 是否注册成功
     */
    public static EasyMailSendTemplate registerTemplate(EasyMailSendTemplate template) {
        if (template == null || !template.isValid()) {
            log.warn("尝试注册无效的邮件模板: {}", template);
            throw new IllegalArgumentException("无效的邮件模板");
        }

        template.setUpdateTime(System.currentTimeMillis());
        templateCache.put(template.getTemplateId(), template);

        log.info("已注册邮件模板: {} - {}", template.getTemplateId(), template.getTemplateName());
        return template;
    }

    /**
     * 获取邮件模板
     *
     * @param templateId 模板ID
     * @return 邮件模板，如果不存在则返回null
     */
    public EasyMailSendTemplate getTemplate(String templateId) {
        if (templateId == null || templateId.trim().isEmpty()) {
            return null;
        }

        return templateCache.get(templateId);
    }

    /**
     * 删除邮件模板
     *
     * @param templateId 模板ID
     * @return 是否删除成功
     */
    public boolean removeTemplate(String templateId) {
        if (templateId == null || templateId.trim().isEmpty()) {
            return false;
        }

        EasyMailSendTemplate removed = templateCache.remove(templateId);
        if (removed != null) {
            log.info("已删除邮件模板: {} - {}", removed.getTemplateId(), removed.getTemplateName());
            return true;
        }

        return false;
    }

    /**
     * 更新邮件模板
     *
     * @param template 邮件模板
     * @return 是否更新成功
     */
    public boolean updateTemplate(EasyMailSendTemplate template) {
        if (template == null || !template.isValid()) {
            return false;
        }

        if (!templateCache.containsKey(template.getTemplateId())) {
            log.warn("尝试更新不存在的模板: {}", template.getTemplateId());
            return false;
        }

        template.setUpdateTime(System.currentTimeMillis());
        templateCache.put(template.getTemplateId(), template);

        log.info("已更新邮件模板: {} - {}", template.getTemplateId(), template.getTemplateName());
        return true;
    }

    /**
     * 检查模板是否存在
     *
     * @param templateId 模板ID
     * @return 是否存在
     */
    public boolean hasTemplate(String templateId) {
        return templateId != null && templateCache.containsKey(templateId);
    }

    /**
     * 获取所有模板ID
     *
     * @return 模板ID集合
     */
    public java.util.Set<String> getAllTemplateIds() {
        return new java.util.HashSet<>(templateCache.keySet());
    }

    /**
     * 获取所有模板
     *
     * @return 模板映射的副本
     */
    public Map<String, EasyMailSendTemplate> getAllTemplates() {
        return new ConcurrentHashMap<>(templateCache);
    }

    /**
     * 清空所有模板
     */
    public void clearAllTemplates() {
        int count = templateCache.size();
        templateCache.clear();
        log.info("已清空所有邮件模板，共 {} 个", count);
    }

    /**
     * 获取模板统计信息
     *
     * @return 统计信息
     */
    public String getTemplateStats() {
        int totalTemplates = templateCache.size();
        long htmlTemplates = templateCache.values().stream().mapToLong(t -> t.isHtml() ? 1 : 0).sum();
        long textTemplates = totalTemplates - htmlTemplates;

        return String.format(
                "模板统计 - 总数: %d, HTML模板: %d, 文本模板: %d",
                totalTemplates, htmlTemplates, textTemplates
        );
    }

    /**
     * 验证模板中的变量
     *
     * @param templateId 模板ID
     * @param variables  变量映射
     * @return 验证结果和缺失的变量列表
     */
    public TemplateValidationResult validateTemplate(String templateId, Map<String, Object> variables) {
        EasyMailSendTemplate template = getTemplate(templateId);
        if (template == null) {
            return new TemplateValidationResult(false, "模板不存在: " + templateId, null);
        }

        // 提取模板中的变量
        java.util.Set<String> requiredVariables = extractVariables(template.getSubjectTemplate());
        requiredVariables.addAll(extractVariables(template.getContentTemplate()));

        // 检查缺失的变量
        java.util.Set<String> missingVariables = new java.util.HashSet<>();
        for (String variable : requiredVariables) {
            if (variables == null || !variables.containsKey(variable)) {
                missingVariables.add(variable);
            }
        }

        if (missingVariables.isEmpty()) {
            return new TemplateValidationResult(true, "模板验证通过", null);
        } else {
            return new TemplateValidationResult(false, "缺少必需的变量", missingVariables);
        }
    }

    /**
     * 从模板字符串中提取变量名
     *
     * @param template 模板字符串
     * @return 变量名集合
     */
    private java.util.Set<String> extractVariables(String template) {
        java.util.Set<String> variables = new java.util.HashSet<>();
        if (template == null) {
            return variables;
        }

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\$\\{([^}]+)\\}");
        java.util.regex.Matcher matcher = pattern.matcher(template);

        while (matcher.find()) {
            variables.add(matcher.group(1));
        }

        return variables;
    }

    /**
     * 模板验证结果
     */
    public static class TemplateValidationResult {
        private final boolean valid;
        private final String message;
        private final java.util.Set<String> missingVariables;

        public TemplateValidationResult(boolean valid, String message, java.util.Set<String> missingVariables) {
            this.valid = valid;
            this.message = message;
            this.missingVariables = missingVariables;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }

        public java.util.Set<String> getMissingVariables() {
            return missingVariables;
        }

        @Override
        public String toString() {
            if (valid) {
                return message;
            } else {
                return message + (missingVariables != null ? ", 缺失变量: " + missingVariables : "");
            }
        }
    }
}