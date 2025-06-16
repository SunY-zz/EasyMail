package cn.sunyblog.easymail.template;

import java.util.Map;
import java.util.List;
import java.util.Optional;

/**
 * 邮件模板引擎接口
 * 提供可扩展的模板处理功能，支持多种模板引擎实现
 * <p>
 * 支持的模板引擎：
 * - 简单变量替换（默认实现）
 * - Thymeleaf
 * - FreeMarker
 * - Velocity
 * - Mustache
 *
 * @author suny
 * @version 1.0.0
 */
public interface EmailTemplateEngine {

    /**
     * 获取模板引擎名称
     *
     * @return 引擎名称
     */
    String getEngineName();

    /**
     * 检查是否支持指定的模板格式
     *
     * @param templateFormat 模板格式（如：simple, thymeleaf, freemarker等）
     * @return 是否支持
     */
    boolean supports(String templateFormat);

    /**
     * 处理模板内容
     *
     * @param template  模板内容
     * @param variables 变量映射
     * @return 处理后的内容
     * @throws TemplateProcessingException 模板处理异常
     */
    String processTemplate(String template, Map<String, Object> variables) throws TemplateProcessingException;

    /**
     * 处理模板文件
     *
     * @param templatePath 模板文件路径
     * @param variables    变量映射
     * @return 处理后的内容
     * @throws TemplateProcessingException 模板处理异常
     */
    String processTemplateFile(String templatePath, Map<String, Object> variables) throws TemplateProcessingException;

    /**
     * 验证模板语法
     *
     * @param template 模板内容
     * @return 验证结果
     */
    TemplateValidationResult validateTemplate(String template);

    /**
     * 提取模板中的变量
     *
     * @param template 模板内容
     * @return 变量列表
     */
    List<String> extractVariables(String template);

    /**
     * 获取模板引擎配置
     *
     * @return 配置信息
     */
    TemplateEngineConfig getConfig();

    /**
     * 设置模板引擎配置
     *
     * @param config 配置信息
     */
    void setConfig(TemplateEngineConfig config);

    /**
     * 模板处理异常
     */
    class TemplateProcessingException extends Exception {
        private final String templateContent;
        private final Map<String, Object> variables;
        private final String engineName;

        public TemplateProcessingException(String message, String templateContent,
                                           Map<String, Object> variables, String engineName) {
            super(message);
            this.templateContent = templateContent;
            this.variables = variables;
            this.engineName = engineName;
        }

        public TemplateProcessingException(String message, Throwable cause, String templateContent,
                                           Map<String, Object> variables, String engineName) {
            super(message, cause);
            this.templateContent = templateContent;
            this.variables = variables;
            this.engineName = engineName;
        }

        public String getTemplateContent() {
            return templateContent;
        }

        public Map<String, Object> getVariables() {
            return variables;
        }

        public String getEngineName() {
            return engineName;
        }
    }

    /**
     * 模板验证结果
     */
    class TemplateValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final List<String> warnings;
        private final List<String> suggestions;

        public TemplateValidationResult(boolean valid, List<String> errors,
                                        List<String> warnings, List<String> suggestions) {
            this.valid = valid;
            this.errors = errors != null ? errors : new java.util.ArrayList<>();
            this.warnings = warnings != null ? warnings : new java.util.ArrayList<>();
            this.suggestions = suggestions != null ? suggestions : new java.util.ArrayList<>();
        }

        public static TemplateValidationResult valid() {
            return new TemplateValidationResult(true, null, null, null);
        }

        public static TemplateValidationResult invalid(List<String> errors) {
            return new TemplateValidationResult(false, errors, null, null);
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public List<String> getSuggestions() {
            return suggestions;
        }

        public String getErrorMessage() {
            return String.join("; ", errors);
        }

        @Override
        public String toString() {
            if (valid) {
                return "模板验证通过";
            } else {
                return "模板验证失败: " + getErrorMessage();
            }
        }
    }

    /**
     * 模板引擎配置
     */
    class TemplateEngineConfig {
        private String templateDirectory;
        private String templateSuffix;
        private String encoding;
        private boolean cacheEnabled;
        private long cacheExpireTime;
        private Map<String, Object> customProperties;

        public TemplateEngineConfig() {
            this.templateDirectory = "templates";
            this.templateSuffix = ".html";
            this.encoding = "UTF-8";
            this.cacheEnabled = true;
            this.cacheExpireTime = 3600000; // 1小时
            this.customProperties = new java.util.HashMap<>();
        }

        // Getters and Setters
        public String getTemplateDirectory() {
            return templateDirectory;
        }

        public void setTemplateDirectory(String templateDirectory) {
            this.templateDirectory = templateDirectory;
        }

        public String getTemplateSuffix() {
            return templateSuffix;
        }

        public void setTemplateSuffix(String templateSuffix) {
            this.templateSuffix = templateSuffix;
        }

        public String getEncoding() {
            return encoding;
        }

        public void setEncoding(String encoding) {
            this.encoding = encoding;
        }

        public boolean isCacheEnabled() {
            return cacheEnabled;
        }

        public void setCacheEnabled(boolean cacheEnabled) {
            this.cacheEnabled = cacheEnabled;
        }

        public long getCacheExpireTime() {
            return cacheExpireTime;
        }

        public void setCacheExpireTime(long cacheExpireTime) {
            this.cacheExpireTime = cacheExpireTime;
        }

        public Map<String, Object> getCustomProperties() {
            return customProperties;
        }

        public void setCustomProperties(Map<String, Object> customProperties) {
            this.customProperties = customProperties;
        }

        public void setCustomProperty(String key, Object value) {
            this.customProperties.put(key, value);
        }

        public Optional<Object> getCustomProperty(String key) {
            return Optional.ofNullable(customProperties.get(key));
        }
    }
}