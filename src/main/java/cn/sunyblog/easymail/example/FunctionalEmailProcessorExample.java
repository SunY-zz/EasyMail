//package cn.sunyblog.easymail.example;
//
//
//import cn.sunyblog.easymail.mail.MailProcessor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//import javax.annotation.PostConstruct;
//import javax.mail.Message;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
///**
// * 函数式邮件处理器示例
// * 展示如何使用函数式接口处理邮件，比实现接口更灵活
// */
//@Slf4j
//@Component
//public class FunctionalEmailProcessorExample {
//
//    @Autowired
//    private MailProcessor mailProcessor;
//
//    /**
//     * 初始化方法，设置函数式邮件处理器
//     */
//    @PostConstruct
//    public void init() {
//        // 使用lambda表达式设置邮件处理函数
//        mailProcessor.setEasyMailProcessorFunction(this::processVerificationEmail);
//        log.info("函数式邮件处理器已注册");
//    }
//
//    /**
//     * 验证码邮件处理函数
//     *
//     * @param message 邮件消息
//     * @param content 邮件内容
//     * @param subject 邮件主题
//     * @param from 发件人
//     * @return 提取的验证码，如果没有找到则返回null
//     */
//    public String processVerificationEmail(Message message, String content, String subject, String from) {
//        log.info("函数式处理邮件: 主题={}, 发件人={}", subject, from);
//
//        // 提取验证码
//        String verificationCode = extractVerificationCode(content);
//        if (verificationCode != null) {
//            log.info("提取到验证码: {}", verificationCode);
//            // 在这里可以将验证码存储到数据库或缓存中，供其他服务使用
//        }
//
//        return verificationCode;
//    }
//
//    /**
//     * 从邮件内容中提取验证码
//     *
//     * @param content 邮件内容
//     * @return 验证码，如果没有找到则返回null
//     */
//    private String extractVerificationCode(String content) {
//        if (content == null) {
//            return null;
//        }
//
//        // 使用正则表达式匹配4-6位数字验证码
//        Pattern pattern = Pattern.compile("\\b(\\d{4,6})\\b");
//        Matcher matcher = pattern.matcher(content);
//
//        if (matcher.find()) {
//            return matcher.group(1);
//        }
//
//        return null;
//    }
//}