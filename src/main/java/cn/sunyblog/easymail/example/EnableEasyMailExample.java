//package cn.sunyblog.easymail.example;
//
//import cn.sunyblog.easymail.annotation.EnableEasyMail;
//import cn.sunyblog.easymail.config.EasyMailAutoConfiguration;
//import cn.sunyblog.easymail.mail.EasyMailService;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.boot.SpringApplication;
//import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.context.annotation.Bean;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.HashMap;
//import java.util.Map;
//
///**
// * @EnableEasyMail注解使用示例
// *
// * <p>此示例展示了如何使用@EnableEasyMail注解来启用EasyMail邮件服务。</p>
// * <p>只需要在Spring Boot主类上添加@EnableEasyMail注解，即可自动配置和启动邮件服务。</p>
// *
// * @author sunyblog
// * @since 1.0.0
// */
//@Slf4j
//@SpringBootApplication
//@EnableEasyMail(
//    autoStart = true,           // 自动启动邮件监听服务
//    enableSender = true,        // 启用邮件发送服务
//    enableListener = true,      // 启用邮件监听服务
//    enableProcessor = true,     // 启用注解驱动的邮件处理器
//    scanPackages = {            // 指定扫描包路径
//        "cn.sunyblog.easymail.example",
//        "cn.sunyblog.easymail.processor"
//    },
//    configPrefix = "mail"       // 配置文件前缀
//)
//public class EnableEasyMailExample {
//
//    @Autowired(required = false)
//    private EasyMailService easyMailService;
//
//    @Autowired(required = false)
//    private EasyMailAutoConfiguration.EasyMailStarter easyMailStarter;
//
//    public static void main(String[] args) {
//        SpringApplication.run(EnableEasyMailExample.class, args);
//    }
//
//    /**
//     * 应用启动后的初始化逻辑
//     */
//    @Bean
//    public CommandLineRunner init() {
//        return args -> {
//            log.info("=== EnableEasyMail示例应用启动完成 ===");
//
//            if (easyMailService != null) {
//                log.info("EasyMail服务状态: {}", easyMailService.isMailServiceRunning() ? "运行中" : "已停止");
//                log.info("邮件处理统计: {}", easyMailService.getMailProcessingStats());
//            } else {
//                log.warn("EasyMail服务未配置，请检查配置文件");
//            }
//
//            if (easyMailStarter != null) {
//                log.info("EasyMail启动器可用，可通过REST API控制服务");
//            }
//
//            log.info("=== 可通过以下REST API控制邮件服务 ===");
//            log.info("GET  /api/email/status   - 获取服务状态");
//            log.info("POST /api/email/start    - 启动邮件服务");
//            log.info("POST /api/email/stop     - 停止邮件服务");
//            log.info("POST /api/email/restart  - 重启邮件服务");
//        };
//    }
//
//    /**
//     * 邮件服务控制REST API
//     */
//    @RestController
//    @RequestMapping("/api/email")
//    public static class EmailServiceController {
//
//        @Autowired(required = false)
//        private EasyMailService easyMailService;
//
//        @Autowired(required = false)
//        private EasyMailAutoConfiguration.EasyMailStarter easyMailStarter;
//
//        /**
//         * 获取邮件服务状态
//         */
//        @GetMapping("/status")
//        public Map<String, Object> getStatus() {
//            Map<String, Object> status = new HashMap<>();
//
//            if (easyMailService != null) {
//                status.put("available", true);
//                status.put("running", easyMailService.isMailServiceRunning());
//                status.put("stats", easyMailService.getMailProcessingStats());
//
//                if (easyMailStarter != null) {
//                    status.put("configuration", easyMailStarter.getConfiguration());
//                }
//            } else {
//                status.put("available", false);
//                status.put("message", "EasyMail服务未配置");
//            }
//
//            status.put("timestamp", System.currentTimeMillis());
//            return status;
//        }
//
//        /**
//         * 启动邮件服务
//         */
//        @PostMapping("/start")
//        public Map<String, Object> startService() {
//            Map<String, Object> result = new HashMap<>();
//
//            if (easyMailStarter != null) {
//                try {
//                    boolean success = easyMailStarter.start();
//                    result.put("success", success);
//                    result.put("message", success ? "邮件服务启动成功" : "邮件服务启动失败或已在运行");
//                } catch (Exception e) {
//                    result.put("success", false);
//                    result.put("message", "启动失败: " + e.getMessage());
//                }
//            } else {
//                result.put("success", false);
//                result.put("message", "EasyMail启动器不可用");
//            }
//
//            result.put("timestamp", System.currentTimeMillis());
//            return result;
//        }
//
//        /**
//         * 停止邮件服务
//         */
//        @PostMapping("/stop")
//        public Map<String, Object> stopService() {
//            Map<String, Object> result = new HashMap<>();
//
//            if (easyMailStarter != null) {
//                try {
//                    easyMailStarter.stop();
//                    result.put("success", true);
//                    result.put("message", "邮件服务已停止");
//                } catch (Exception e) {
//                    result.put("success", false);
//                    result.put("message", "停止失败: " + e.getMessage());
//                }
//            } else {
//                result.put("success", false);
//                result.put("message", "EasyMail启动器不可用");
//            }
//
//            result.put("timestamp", System.currentTimeMillis());
//            return result;
//        }
//
//        /**
//         * 重启邮件服务
//         */
//        @PostMapping("/restart")
//        public Map<String, Object> restartService() {
//            Map<String, Object> result = new HashMap<>();
//
//            if (easyMailStarter != null) {
//                try {
//                    boolean success = easyMailStarter.restart();
//                    result.put("success", success);
//                    result.put("message", success ? "邮件服务重启成功" : "邮件服务重启失败");
//                } catch (Exception e) {
//                    result.put("success", false);
//                    result.put("message", "重启失败: " + e.getMessage());
//                }
//            } else {
//                result.put("success", false);
//                result.put("message", "EasyMail启动器不可用");
//            }
//
//            result.put("timestamp", System.currentTimeMillis());
//            return result;
//        }
//    }
//}