package cn.sunyblog.javaemaildemo.processor.handler;

import cn.sunyblog.javaemaildemo.processor.annotation.EmailHandler;
import cn.sunyblog.javaemaildemo.processor.annotation.EmailProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 邮件处理器注册表
 * 负责扫描和管理所有的邮件处理器
 * 
 * @author suny
 * @version 1.0
 * @date 2025/06/14
 */
@Slf4j
@Component
public class EmailHandlerRegistry implements BeanPostProcessor {
    
    /** 处理器信息映射 */
    private final Map<String, EmailHandlerInfo> handlers = new ConcurrentHashMap<>();
    
    /** 按优先级排序的处理器列表 */
    private volatile List<EmailHandlerInfo> sortedHandlers = new ArrayList<>();
    
    /** 按组分类的处理器映射 */
    private final Map<String, List<EmailHandlerInfo>> handlersByGroup = new ConcurrentHashMap<>();
    
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> beanClass = bean.getClass();
        
        // 检查是否是邮件处理器
        EmailProcessor processorAnnotation = AnnotationUtils.findAnnotation(beanClass, EmailProcessor.class);
        if (processorAnnotation == null) {
            return bean;
        }
        
        if (!processorAnnotation.enabled()) {
            log.debug("邮件处理器 {} 已禁用，跳过注册", beanName);
            return bean;
        }
        
        // 扫描处理器方法
        scanHandlerMethods(bean, processorAnnotation);
        
        return bean;
    }
    
    /**
     * 扫描处理器方法
     * @param bean Bean实例
     * @param processorAnnotation 处理器注解
     */
    private void scanHandlerMethods(Object bean, EmailProcessor processorAnnotation) {
        Class<?> beanClass = bean.getClass();
        String group = processorAnnotation.group();
        boolean enabled = processorAnnotation.enabled();
        
        ReflectionUtils.doWithMethods(beanClass, method -> {
            EmailHandler handlerAnnotation = AnnotationUtils.findAnnotation(method, EmailHandler.class);
            if (handlerAnnotation != null) {
                registerHandler(bean, method, handlerAnnotation, group, enabled);
            }
        });
    }
    
    /**
     * 注册处理器
     * @param bean Bean实例
     * @param method 方法
     * @param annotation 注解
     * @param group 组名
     * @param enabled 是否启用
     */
    private void registerHandler(Object bean, Method method, EmailHandler annotation, 
                                String group, boolean enabled) {
        try {
            // 验证方法签名
            validateHandlerMethod(method);
            
            // 创建处理器信息
            EmailHandlerInfo handlerInfo = EmailHandlerInfo.fromAnnotation(
                    bean, method, annotation, group, enabled);
            
            // 注册处理器
            String handlerKey = generateHandlerKey(bean, method);
            handlers.put(handlerKey, handlerInfo);
            
            // 按组分类
            handlersByGroup.computeIfAbsent(group, k -> new ArrayList<>()).add(handlerInfo);
            
            // 重新排序
            refreshSortedHandlers();
            
            log.info("注册邮件处理器: {} [组: {}, 优先级: {}, 异步: {}]", 
                    handlerInfo.getName(), group, annotation.priority(), annotation.async());
            
        } catch (Exception e) {
            log.error("注册邮件处理器失败: {}.{}", bean.getClass().getSimpleName(), method.getName(), e);
        }
    }
    
    /**
     * 验证处理器方法签名
     * @param method 方法
     */
    private void validateHandlerMethod(Method method) {
        Class<?>[] paramTypes = method.getParameterTypes();
        
        // 支持的方法签名：
        // 1. void handler(EmailContext context)
        // 2. Object handler(EmailContext context)
        // 3. void handler(Message message, String content, String subject, String from)
        // 4. Object handler(Message message, String content, String subject, String from)
        
        if (paramTypes.length == 1 && paramTypes[0] == EmailContext.class) {
            return; // 新的上下文方式
        }
        
        if (paramTypes.length == 4 && 
            paramTypes[0] == javax.mail.Message.class &&
            paramTypes[1] == String.class &&
            paramTypes[2] == String.class &&
            paramTypes[3] == String.class) {
            return; // 兼容旧的方式
        }
        
        throw new IllegalArgumentException(
                "邮件处理器方法签名不正确: " + method.getName() + 
                ". 支持的签名: handler(EmailContext) 或 handler(Message, String, String, String)");
    }
    
    /**
     * 生成处理器键
     * @param bean Bean实例
     * @param method 方法
     * @return 处理器键
     */
    private String generateHandlerKey(Object bean, Method method) {
        return bean.getClass().getName() + "#" + method.getName();
    }
    
    /**
     * 刷新排序后的处理器列表
     */
    private void refreshSortedHandlers() {
        sortedHandlers = handlers.values().stream()
                .filter(EmailHandlerInfo::isEnabled)
                .sorted(Comparator.comparingInt(EmailHandlerInfo::getPriority))
                .collect(Collectors.toList());
    }
    
    /**
     * 获取匹配的处理器列表
     * @param context 邮件上下文
     * @return 匹配的处理器列表
     */
    public List<EmailHandlerInfo> getMatchingHandlers(EmailContext context) {
        return sortedHandlers.stream()
                .filter(handler -> handler.matches(context))
                .collect(Collectors.toList());
    }
    
    /**
     * 获取指定组的处理器
     * @param group 组名
     * @return 处理器列表
     */
    public List<EmailHandlerInfo> getHandlersByGroup(String group) {
        return handlersByGroup.getOrDefault(group, Collections.emptyList());
    }
    
    /**
     * 获取所有处理器
     * @return 处理器列表
     */
    public List<EmailHandlerInfo> getAllHandlers() {
        return new ArrayList<>(sortedHandlers);
    }
    
    /**
     * 获取处理器统计信息
     * @return 统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalHandlers", handlers.size());
        stats.put("enabledHandlers", sortedHandlers.size());
        stats.put("groups", handlersByGroup.keySet());
        
        Map<String, Integer> groupCounts = new HashMap<>();
        handlersByGroup.forEach((group, handlerList) -> 
                groupCounts.put(group, handlerList.size()));
        stats.put("handlersByGroup", groupCounts);
        
        return stats;
    }
    
    /**
     * 启用/禁用处理器
     * @param handlerName 处理器名称
     * @param enabled 是否启用
     */
    public void setHandlerEnabled(String handlerName, boolean enabled) {
        handlers.values().stream()
                .filter(handler -> handler.getName().equals(handlerName))
                .forEach(handler -> {
                    handler.setEnabled(enabled);
                    log.info("处理器 {} 已{}", handlerName, enabled ? "启用" : "禁用");
                });
        refreshSortedHandlers();
    }
}