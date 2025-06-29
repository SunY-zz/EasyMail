package cn.sunyblog.easymail.template;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 简单邮件模板引擎实现
 * 支持 ${variable} 格式的变量替换
 * <p>
 * 功能特性：
 * - 变量替换：${variable}
 * - 条件判断：${if:condition}...${endif}
 * - 循环处理：${foreach:list}...${endforeach}
 * - 默认值：${variable:defaultValue}
 * - 模板缓存
 * - 嵌套变量支持
 *
 * @author suny
 * @version 1.0.0
 */
@Component
public class SimpleEasyMailSendTemplateEngine implements EasyMailSendTemplateEngine {

    private static final String ENGINE_NAME = "simple";
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");
    private static final Pattern IF_PATTERN = Pattern.compile("\\$\\{if:([^}]+)\\}(.*?)\\$\\{endif\\}", Pattern.DOTALL);
    private static final Pattern FOREACH_PATTERN = Pattern.compile("\\$\\{foreach:([^}]+)\\}(.*?)\\$\\{endforeach\\}", Pattern.DOTALL);

    private TemplateEngineConfig config;
    private final Map<String, String> templateCache = new ConcurrentHashMap<>();
    private final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();

    public SimpleEasyMailSendTemplateEngine() {
        this.config = new TemplateEngineConfig();
    }

    @Override
    public String getEngineName() {
        return ENGINE_NAME;
    }

    @Override
    public boolean supports(String templateFormat) {
        return "simple".equalsIgnoreCase(templateFormat) ||
                "default".equalsIgnoreCase(templateFormat) ||
                StringUtils.isEmpty(templateFormat);
    }

    @Override
    public String processTemplate(String template, Map<String, Object> variables) throws TemplateProcessingException {
        if (template == null) {
            throw new TemplateProcessingException("模板内容不能为空", template, variables, ENGINE_NAME);
        }

        if (variables == null) {
            variables = new HashMap<>();
        }

        try {
            String result = template;

            // 处理条件判断
            result = processConditionals(result, variables);

            // 处理循环
            result = processForeach(result, variables);

            // 处理变量替换
            result = processVariables(result, variables);

            return result;
        } catch (Exception e) {
            throw new TemplateProcessingException(
                    "模板处理失败: " + e.getMessage(), e, template, variables, ENGINE_NAME);
        }
    }

    @Override
    public String processTemplateFile(String templatePath, Map<String, Object> variables) throws TemplateProcessingException {
        String template = loadTemplate(templatePath);
        return processTemplate(template, variables);
    }

    @Override
    public TemplateValidationResult validateTemplate(String template) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        if (template == null || template.trim().isEmpty()) {
            errors.add("模板内容不能为空");
            return new TemplateValidationResult(false, errors, warnings, suggestions);
        }

        // 检查变量语法
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        Set<String> variables = new HashSet<>();
        while (matcher.find()) {
            String variable = matcher.group(1);
            variables.add(variable);

            // 检查变量名是否合法
            if (!isValidVariableName(variable)) {
                errors.add("无效的变量名: " + variable);
            }
        }

        // 检查条件语句配对
        if (!areConditionalsBalanced(template)) {
            errors.add("条件语句不匹配，请检查 ${if:...} 和 ${endif} 的配对");
        }

        // 检查循环语句配对
        if (!areForeachBalanced(template)) {
            errors.add("循环语句不匹配，请检查 ${foreach:...} 和 ${endforeach} 的配对");
        }

        // 提供建议
        if (variables.isEmpty()) {
            suggestions.add("模板中没有发现变量，考虑添加动态内容");
        }

        return new TemplateValidationResult(errors.isEmpty(), errors, warnings, suggestions);
    }

    @Override
    public List<String> extractVariables(String template) {
        Set<String> variables = new HashSet<>();
        Matcher matcher = VARIABLE_PATTERN.matcher(template);

        while (matcher.find()) {
            String variable = matcher.group(1);
            // 移除默认值部分
            if (variable.contains(":")) {
                variable = variable.split(":")[0];
            }
            variables.add(variable);
        }

        return new ArrayList<>(variables);
    }

    @Override
    public TemplateEngineConfig getConfig() {
        return config;
    }

    @Override
    public void setConfig(TemplateEngineConfig config) {
        this.config = config != null ? config : new TemplateEngineConfig();
    }

    /**
     * 加载模板文件
     * 支持从文件系统和classpath加载模板
     */
    private String loadTemplate(String templatePath) throws TemplateProcessingException {
        if (config.isCacheEnabled()) {
            String cached = getCachedTemplate(templatePath);
            if (cached != null) {
                return cached;
            }
        }

        String content = null;

        // 首先尝试从classpath:static/templates/加载
        try {
            content = loadTemplateFromClasspath(templatePath);
        } catch (Exception e) {
            // 如果classpath加载失败，尝试从文件系统加载
            try {
                content = loadTemplateFromFileSystem(templatePath);
            } catch (Exception ex) {
                throw new TemplateProcessingException(
                        "无法加载模板文件: " + templatePath + ". 已尝试从classpath:static/templates/和文件系统加载",
                        ex, null, null, ENGINE_NAME);
            }
        }

        if (config.isCacheEnabled()) {
            cacheTemplate(templatePath, content);
        }

        return content;
    }

    /**
     * 从classpath加载模板文件
     */
    private String loadTemplateFromClasspath(String templatePath) throws IOException {
        // 确保模板路径以.html结尾
        String fullTemplatePath = templatePath;
        if (!templatePath.endsWith(".html")) {
            fullTemplatePath = templatePath + ".html";
        }

        // 构建完整的classpath路径
        String resourcePath = "static/templates/" + fullTemplatePath;

        ClassPathResource resource = new ClassPathResource(resourcePath);
        if (!resource.exists()) {
            throw new IOException("模板文件不存在: " + resourcePath);
        }

        try (InputStream inputStream = resource.getInputStream()) {
            byte[] buffer = new byte[1024];
            ByteArrayOutputStream resultStream = new ByteArrayOutputStream();
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                resultStream.write(buffer, 0, length);
            }
            return resultStream.toString(StandardCharsets.UTF_8.name());
        }
    }

    /**
     * 从文件系统加载模板文件（保持原有功能）
     */
    private String loadTemplateFromFileSystem(String templatePath) throws IOException {
        Path fullPath = Paths.get(config.getTemplateDirectory(), templatePath + config.getTemplateSuffix());
        return new String(Files.readAllBytes(fullPath), java.nio.charset.Charset.forName(config.getEncoding()));
    }

    /**
     * 获取缓存的模板
     */
    private String getCachedTemplate(String templatePath) {
        Long timestamp = cacheTimestamps.get(templatePath);
        if (timestamp != null && System.currentTimeMillis() - timestamp < config.getCacheExpireTime()) {
            return templateCache.get(templatePath);
        }

        // 清理过期缓存
        templateCache.remove(templatePath);
        cacheTimestamps.remove(templatePath);
        return null;
    }

    /**
     * 缓存模板
     */
    private void cacheTemplate(String templatePath, String content) {
        templateCache.put(templatePath, content);
        cacheTimestamps.put(templatePath, System.currentTimeMillis());
    }

    /**
     * 处理变量替换
     */
    private String processVariables(String template, Map<String, Object> variables) {
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String variableExpression = matcher.group(1);
            String replacement = resolveVariable(variableExpression, variables);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 解析变量值
     */
    private String resolveVariable(String variableExpression, Map<String, Object> variables) {
        String[] parts = variableExpression.split(":", 2);
        String variableName = parts[0].trim();
        String defaultValue = parts.length > 1 ? parts[1].trim() : "";

        Object value = getNestedValue(variableName, variables);
        return value != null ? value.toString() : defaultValue;
    }

    /**
     * 获取嵌套变量值
     */
    private Object getNestedValue(String variableName, Map<String, Object> variables) {
        if (variableName.contains(".")) {
            String[] parts = variableName.split("\\.", 2);
            Object parent = variables.get(parts[0]);

            if (parent instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> parentMap = (Map<String, Object>) parent;
                return getNestedValue(parts[1], parentMap);
            }
        }

        return variables.get(variableName);
    }

    /**
     * 处理条件判断
     */
    private String processConditionals(String template, Map<String, Object> variables) {
        Matcher matcher = IF_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String condition = matcher.group(1);
            String content = matcher.group(2);

            if (evaluateCondition(condition, variables)) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(content));
            } else {
                matcher.appendReplacement(result, "");
            }
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 处理循环
     */
    private String processForeach(String template, Map<String, Object> variables) {
        Matcher matcher = FOREACH_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String listName = matcher.group(1);
            String content = matcher.group(2);

            String loopResult = processLoop(listName, content, variables);
            matcher.appendReplacement(result, Matcher.quoteReplacement(loopResult));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 处理循环内容
     */
    private String processLoop(String listName, String content, Map<String, Object> variables) {
        Object listObj = variables.get(listName);
        if (!(listObj instanceof List)) {
            return "";
        }

        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) listObj;
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            Map<String, Object> loopVariables = new HashMap<>(variables);
            loopVariables.put("item", item);
            loopVariables.put("index", i);
            loopVariables.put("first", i == 0);
            loopVariables.put("last", i == list.size() - 1);

            String processedContent = processVariables(content, loopVariables);
            result.append(processedContent);
        }

        return result.toString();
    }

    /**
     * 评估条件
     */
    private boolean evaluateCondition(String condition, Map<String, Object> variables) {
        // 简单的条件评估，支持存在性检查和相等比较
        if (condition.contains("==")) {
            String[] parts = condition.split("==", 2);
            String left = resolveVariable(parts[0].trim(), variables);
            String right = parts[1].trim().replaceAll("['\"]?", "");
            return left.equals(right);
        } else if (condition.contains("!=")) {
            String[] parts = condition.split("!=", 2);
            String left = resolveVariable(parts[0].trim(), variables);
            String right = parts[1].trim().replaceAll("['\"]?", "");
            return !left.equals(right);
        } else {
            // 检查变量是否存在且不为空
            Object value = getNestedValue(condition.trim(), variables);
            return value != null && !value.toString().isEmpty() && !"false".equalsIgnoreCase(value.toString());
        }
    }

    /**
     * 检查变量名是否合法
     */
    private boolean isValidVariableName(String variableName) {
        if (variableName == null || variableName.trim().isEmpty()) {
            return false;
        }

        String name = variableName.split(":")[0].trim();
        return name.matches("[a-zA-Z_][a-zA-Z0-9_.]*");
    }

    /**
     * 检查条件语句是否平衡
     */
    private boolean areConditionalsBalanced(String template) {
        int ifCount = countOccurrences(template, "${if:");
        int endifCount = countOccurrences(template, "${endif}");
        return ifCount == endifCount;
    }

    /**
     * 检查循环语句是否平衡
     */
    private boolean areForeachBalanced(String template) {
        int foreachCount = countOccurrences(template, "${foreach:");
        int endforeachCount = countOccurrences(template, "${endforeach}");
        return foreachCount == endforeachCount;
    }

    /**
     * 计算字符串出现次数
     */
    private int countOccurrences(String text, String pattern) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(pattern, index)) != -1) {
            count++;
            index += pattern.length();
        }
        return count;
    }

    /**
     * 清理缓存
     */
    public void clearCache() {
        templateCache.clear();
        cacheTimestamps.clear();
    }

    /**
     * 获取缓存统计信息
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cacheSize", templateCache.size());
        stats.put("cacheEnabled", config.isCacheEnabled());
        stats.put("cacheExpireTime", config.getCacheExpireTime());
        return stats;
    }
}