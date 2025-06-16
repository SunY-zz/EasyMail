package cn.sunyblog.javaemaildemo.processor.handler;

import cn.sunyblog.javaemaildemo.processor.annotation.EmailHandler;
import lombok.Builder;
import lombok.Data;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * 邮件处理器信息
 * 存储处理器的元数据信息
 * 
 * @author suny
 * @version 1.0
 * @date 2025/06/14
 */
@Data
@Builder
public class EmailHandlerInfo {
    
    /** 处理器Bean实例 */
    private Object bean;
    
    /** 处理器方法 */
    private Method method;
    
    /** 注解信息 */
    private EmailHandler annotation;
    
    /** 处理器名称 */
    private String name;
    
    /** 处理器描述 */
    private String description;
    
    /** 优先级 */
    private int priority;
    
    /** 是否异步处理 */
    private boolean async;
    
    /** 主题匹配模式 */
    private List<String> subjectPatterns;
    
    /** 主题匹配类型 */
    private EmailHandler.MatchType subjectMatchType;
    
    /** 发件人匹配模式 */
    private List<String> fromPatterns;
    
    /** 发件人匹配类型 */
    private EmailHandler.MatchType fromMatchType;
    
    /** 标签匹配模式 */
    private List<String> tagPatterns;
    
    /** 处理器组名 */
    private String group;
    
    /** 是否启用 */
    private boolean enabled;
    
    /**
     * 从注解创建处理器信息
     * @param bean Bean实例
     * @param method 方法
     * @param annotation 注解
     * @param group 组名
     * @param enabled 是否启用
     * @return 处理器信息
     */
    public static EmailHandlerInfo fromAnnotation(Object bean, Method method, 
                                                  EmailHandler annotation, 
                                                  String group, boolean enabled) {
        String name = annotation.name();
        if (name.isEmpty()) {
            name = bean.getClass().getSimpleName() + "#" + method.getName();
        }
        
        return EmailHandlerInfo.builder()
                .bean(bean)
                .method(method)
                .annotation(annotation)
                .name(name)
                .description(annotation.description())
                .priority(annotation.priority())
                .async(annotation.async())
                .subjectPatterns(Arrays.asList(annotation.subject()))
                .subjectMatchType(annotation.subjectMatchType())
                .fromPatterns(Arrays.asList(annotation.from()))
                .fromMatchType(annotation.fromMatchType())
                .tagPatterns(Arrays.asList(annotation.tags()))
                .group(group)
                .enabled(enabled)
                .build();
    }
    
    /**
     * 检查是否匹配邮件上下文
     * @param context 邮件上下文
     * @return 是否匹配
     */
    public boolean matches(EmailContext context) {
        if (!enabled) {
            return false;
        }
        
        // 检查主题匹配
        if (!subjectPatterns.isEmpty() && !matchesPattern(context.getSubject(), subjectPatterns, subjectMatchType)) {
            return false;
        }
        
        // 检查发件人匹配
        if (!fromPatterns.isEmpty() && !matchesPattern(context.getFrom(), fromPatterns, fromMatchType)) {
            return false;
        }
        
        // 检查标签匹配
        if (!tagPatterns.isEmpty()) {
            if (context.getTags() == null || context.getTags().isEmpty()) {
                return false;
            }
            boolean tagMatched = false;
            for (String tag : tagPatterns) {
                if (context.hasTag(tag)) {
                    tagMatched = true;
                    break;
                }
            }
            if (!tagMatched) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 检查模式匹配
     * @param text 待匹配文本
     * @param patterns 模式列表
     * @param matchType 匹配类型
     * @return 是否匹配
     */
    private boolean matchesPattern(String text, List<String> patterns, EmailHandler.MatchType matchType) {
        if (text == null) {
            return false;
        }
        
        for (String pattern : patterns) {
            if (pattern.isEmpty()) {
                continue;
            }
            
            switch (matchType) {
                case EXACT:
                    if (text.equals(pattern)) {
                        return true;
                    }
                    break;
                case CONTAINS:
                    if (text.contains(pattern)) {
                        return true;
                    }
                    break;
                case REGEX:
                    if (text.matches(pattern)) {
                        return true;
                    }
                    break;
                case PREFIX:
                    if (text.startsWith(pattern)) {
                        return true;
                    }
                    break;
                case SUFFIX:
                    if (text.endsWith(pattern)) {
                        return true;
                    }
                    break;
            }
        }
        
        return false;
    }
}