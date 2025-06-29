//package cn.sunyblog.easymail;
//
//import cn.sunyblog.easymail.api.EasyMailRequest;
//import cn.sunyblog.easymail.send.EasyMailSendResult;
//import cn.sunyblog.easymail.api.EasyMailSenderService;
//import org.junit.jupiter.api.Test;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.TestPropertySource;
//
//import javax.annotation.Resource;
//import java.util.HashMap;
//import java.util.Map;
//
///**
// * HTML模板功能测试
// */
//@SpringBootTest
//@TestPropertySource(properties = {
//        "mail.smtp.server=smtp.example.com",
//        "mail.smtp.port=587",
//        "mail.smtp.username=test@example.com",
//        "mail.smtp.password=password"
//})
//public class HtmlTemplateTest {
//
//    @Resource
//    private EasyMailSenderService easyMailSenderService;
//
//    @Test
//    public void testSendHtmlTemplate() {
//        // 准备模板变量
//        Map<String, Object> variables = new HashMap<>();
//        variables.put("username", "测试用户");
//        variables.put("email", "test@example.com");
//        variables.put("registerTime", "2024-01-01 12:00:00");
//        variables.put("activationUrl", "https://example.com/activate?token=test123");
//
//        try {
//            // 测试发送HTML模板邮件
//            EasyMailSendResult result = easyMailSenderService.sendHtmlTemplate(
//                    "test@example.com",
//                    "欢迎加入我们！",
//                    "welcome", // 使用文件路径，不是模板ID
//                    variables
//            );
//
//            System.out.println("HTML模板邮件发送结果: " + result.isSuccess());
//            if (!result.isSuccess()) {
//                System.out.println("发送失败原因: " + result.getErrorMessage());
//            }
//
//        } catch (Exception e) {
//            System.out.println("测试过程中出现异常: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//
//    @Test
//    public void testSendVerificationCodeTemplate() {
//        Map<String, Object> variables = new HashMap<>();
//        variables.put("username", "测试用户");
//        variables.put("operation", "账户验证");
//        variables.put("verificationCode", "123456");
//        variables.put("expireMinutes", "5");
//        variables.put("sendTime", "2024-01-01 12:00:00");
//
//        try {
//            EasyMailSendResult result = easyMailSenderService.sendHtmlTemplate(
//                    "test@example.com",
//                    "您的验证码",
//                    "verification-code",
//                    variables
//            );
//
//            System.out.println("验证码模板邮件发送结果: " + result.isSuccess());
//            if (!result.isSuccess()) {
//                System.out.println("发送失败原因: " + result.getErrorMessage());
//            }
//
//        } catch (Exception e) {
//            System.out.println("测试过程中出现异常: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//
//    @Test
//    public void testTemplateFileLoading() {
//        // 测试模板文件是否能正确加载
//        try {
//            // 创建一个EasyMailRequest来测试模板ID处理
//            EasyMailRequest request = EasyMailRequest.htmlTemplate(
//                    "test@example.com",
//                    "测试主题",
//                    "welcome",
//                    new HashMap<>()
//            );
//
//            System.out.println("生成的模板ID: " + request.getTemplateId());
//            System.out.println("是否为模板邮件: " + request.isTemplate());
//
//            // 验证模板ID是否正确添加了"file:"前缀
//            assert request.getTemplateId().startsWith("file:") : "模板ID应该以'file:'开头";
//            assert request.getTemplateId().equals("file:welcome") : "模板ID格式不正确";
//
//            System.out.println("模板ID格式验证通过");
//
//        } catch (Exception e) {
//            System.out.println("模板文件加载测试失败: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//}