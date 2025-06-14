//package cn.sunyblog.javaemaildemo.example;
//
//import cn.sunyblog.javaemaildemo.api.EmailListenerApi;
//import cn.sunyblog.javaemaildemo.mail.MailService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import javax.mail.Message;
//
///**
// * 邮件监听器示例
// * 包含一个示例处理器和一个示例控制器
// */
//public class EmailListenerExample {
//
//    /**
//     * 示例邮件处理器
//     * 实现EmailListenerApi接口，处理接收到的邮件
//     */
//    @Component
//    public static class ExampleEmailProcessor implements EmailListenerApi {
//
//        @Override
//        public boolean processEmail(Message message, String content, String subject, String from) {
//            System.out.println("收到新邮件: " + subject + " 来自: " + from);
//
//            // 提取验证码的简单示例
//            String verificationCode = extractVerificationCode(content);
//            if (verificationCode != null) {
//                System.out.println("提取到验证码: " + verificationCode);
//                // 在这里可以将验证码存储到数据库或缓存中，供其他服务使用
//            }
//
//            return true;
//        }
//
//        @Override
//        public String getProcessorName() {
//            return "验证码提取处理器";
//        }
//
//        /**
//         * 从邮件内容中提取验证码的简单示例
//         * 实际应用中可能需要更复杂的正则表达式或解析逻辑
//         */
//        private String extractVerificationCode(String content) {
//            // 这里使用一个简单的正则表达式来匹配4-6位数字作为验证码
//            // 实际应用中应根据邮件格式定制提取逻辑
//            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\b(\\d{4,6})\\b");
//            java.util.regex.Matcher matcher = pattern.matcher(content);
//            if (matcher.find()) {
//                return matcher.group(1);
//            }
//            return null;
//        }
//    }
//
//    /**
//     * 示例邮件控制器
//     * 提供API接口来控制邮件监听器
//     */
//    @RestController
//    @RequestMapping("/api/email")
//    public static class ExampleEmailController {
//
//        @Autowired
//        private MailService mailService;
//
//        /**
//         * 获取邮件监听器状态
//         */
//        @GetMapping("/status")
//        public String getStatus() {
//            boolean running = mailService.isMailServiceRunning();
//            return "邮件监听器状态: " + (running ? "运行中" : "已停止");
//        }
//
//        /**
//         * 启动邮件监听器
//         */
//        @GetMapping("/start")
//        public String startListener() {
//            boolean result = mailService.startMailMonitoring();
//            return "邮件监听器启动" + (result ? "成功" : "失败");
//        }
//
//        /**
//         * 停止邮件监听器
//         */
//        @GetMapping("/stop")
//        public String stopListener() {
//            mailService.stopMailMonitoring();
//            return "邮件监听器已停止";
//        }
//
//        /**
//         * 获取邮件处理统计
//         */
//        @GetMapping("/stats")
//        public String getStats() {
//            return mailService.getMailProcessingStats();
//        }
//    }
//}