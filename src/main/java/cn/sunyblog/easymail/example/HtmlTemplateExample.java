//package cn.sunyblog.easymail.example;
//
//import cn.sunyblog.easymail.api.EasyMailSenderService;
//import cn.sunyblog.easymail.send.EasyMailSendResult;
//import org.springframework.stereotype.Component;
//
//import javax.annotation.Resource;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.HashMap;
//import java.util.Map;
//
///**
// * HTML模板邮件发送示例
// * 演示如何使用HTML模板文件发送邮件
// *
// * @author suny
// * @version 1.0.0
// */
//@Component
//public class HtmlTemplateExample {
//
//    @Resource
//    private EasyMailSenderService easyMailSenderService;
//
//    /**
//     * 发送欢迎邮件示例
//     */
//    public void sendWelcomeEmail() {
//        // 准备模板变量
//        Map<String, Object> variables = new HashMap<>();
//        variables.put("username", "张三");
//        variables.put("email", "zhangsan@example.com");
//        variables.put("registerTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
//        variables.put("activationUrl", "https://example.com/activate?token=abc123");
//
//        // 使用HTML模板发送邮件
//        EasyMailSendResult result = easyMailSenderService.sendHtmlTemplate(
//                "zhangsan@example.com",
//                "欢迎加入我们！",
//                "welcome", // 模板文件名（不需要.html后缀）
//                variables
//        );
//
//        if (result.isSuccess()) {
//            System.out.println("欢迎邮件发送成功！");
//        } else {
//            System.err.println("欢迎邮件发送失败：" + result.getErrorMessage());
//        }
//    }
//
//    /**
//     * 发送验证码邮件示例
//     */
//    public void sendVerificationCodeEmail() {
//        // 准备模板变量
//        Map<String, Object> variables = new HashMap<>();
//        variables.put("username", "李四");
//        variables.put("operation", "登录验证");
//        variables.put("verificationCode", "123456");
//        variables.put("expireMinutes", "5");
//        variables.put("sendTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
//
//        // 使用HTML模板发送邮件
//        EasyMailSendResult result = easyMailSenderService.sendHtmlTemplate(
//                "lisi@example.com",
//                "您的验证码",
//                "verification-code",
//                variables
//        );
//
//        if (result.isSuccess()) {
//            System.out.println("验证码邮件发送成功！");
//        } else {
//            System.err.println("验证码邮件发送失败：" + result.getErrorMessage());
//        }
//    }
//
//    /**
//     * 发送系统通知邮件示例
//     */
//    public void sendNotificationEmail() {
//        // 准备模板变量
//        Map<String, Object> variables = new HashMap<>();
//        variables.put("username", "王五");
//        variables.put("notificationType", "info");
//        variables.put("notificationTypeText", "信息通知");
//        variables.put("title", "账户安全提醒");
//        variables.put("message", "您的账户在新设备上登录，如非本人操作请及时修改密码。");
//        variables.put("details", true);
//        variables.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
//        variables.put("operation", "设备登录");
//        variables.put("result", "成功");
//        variables.put("additionalInfo", "登录IP: 192.168.1.100");
//        variables.put("actionUrl", "https://example.com/security");
//        variables.put("actionText", "查看安全设置");
//        variables.put("sendTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
//
//        // 使用HTML模板发送邮件
//        EasyMailSendResult result = easyMailSenderService.sendHtmlTemplate(
//                "wangwu@example.com",
//                "账户安全提醒",
//                "notification",
//                variables
//        );
//
//        if (result.isSuccess()) {
//            System.out.println("通知邮件发送成功！");
//        } else {
//            System.err.println("通知邮件发送失败：" + result.getErrorMessage());
//        }
//    }
//
//    /**
//     * 发送简单HTML模板邮件（无变量）
//     */
//    public void sendSimpleTemplateEmail() {
//        // 不使用变量的简单模板
//        EasyMailSendResult result = easyMailSenderService.sendHtmlTemplate(
//                "test@example.com",
//                "测试邮件",
//                "welcome" // 即使模板中有变量，不传入变量也可以发送
//        );
//
//        if (result.isSuccess()) {
//            System.out.println("简单模板邮件发送成功！");
//        } else {
//            System.err.println("简单模板邮件发送失败：" + result.getErrorMessage());
//        }
//    }
//
//    /**
//     * 批量发送模板邮件示例
//     */
//    public void sendBatchTemplateEmails() {
//        // 用户列表
//        String[] users = {"user1@example.com", "user2@example.com", "user3@example.com"};
//        String[] usernames = {"用户1", "用户2", "用户3"};
//
//        for (int i = 0; i < users.length; i++) {
//            Map<String, Object> variables = new HashMap<>();
//            variables.put("username", usernames[i]);
//            variables.put("email", users[i]);
//            variables.put("registerTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
//            variables.put("activationUrl", "https://example.com/activate?user=" + (i + 1));
//
//            EasyMailSendResult result = easyMailSenderService.sendHtmlTemplate(
//                    users[i],
//                    "欢迎加入我们！",
//                    "welcome",
//                    variables
//            );
//
//            System.out.println("发送给 " + users[i] + " 的邮件：" +
//                    (result.isSuccess() ? "成功" : "失败 - " + result.getErrorMessage()));
//        }
//    }
//}