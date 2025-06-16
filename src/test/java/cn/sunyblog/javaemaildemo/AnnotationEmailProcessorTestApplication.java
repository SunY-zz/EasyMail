//package cn.sunyblog.javaemaildemo;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.SpringApplication;
//import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.context.ConfigurableApplicationContext;
//
///**
// * 注解驱动邮件处理器测试启动类
// *
// * @author suny
// * @version 1.0
// * @date 2025/06/14
// */
//@Slf4j
//// 移除@SpringBootApplication注解，避免与主配置类冲突
//// @SpringBootApplication
//public class AnnotationEmailProcessorTestApplication {
//
//    public static void main(String[] args) {
//        log.info("=== 启动注解驱动邮件处理器测试应用 ===");
//
//        try {
//            ConfigurableApplicationContext context = SpringApplication.run(AnnotationEmailProcessorTestApplication.class, args);
//
//            log.info("✅ 应用启动成功");
//            log.info("📋 已加载的Bean数量: {}", context.getBeanDefinitionCount());
//
//            // 检查关键组件是否加载
//            String[] beanNames = context.getBeanDefinitionNames();
//            for (String beanName : beanNames) {
//                if (beanName.contains("email") || beanName.contains("Email") ||
//                    beanName.contains("processor") || beanName.contains("Processor")) {
//                    log.info("📌 邮件相关Bean: {}", beanName);
//                }
//            }
//
//        } catch (Exception e) {
//            log.error("❌ 应用启动失败", e);
//            System.exit(1);
//        }
//    }
//}