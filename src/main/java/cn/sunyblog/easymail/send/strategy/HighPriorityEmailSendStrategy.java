package cn.sunyblog.easymail.send.strategy;

import cn.sunyblog.easymail.send.SendResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

/**
 * 高优先级邮件发送策略
 * 专门处理紧急邮件，设置高优先级标识，使用更短的超时时间
 *
 * @author suny
 * @version 1.0.0
 */
@Slf4j
@Component
public class HighPriorityEmailSendStrategy implements EmailSendStrategy {

    @Resource
    private DefaultEmailSendStrategy defaultStrategy;

    /**
     * 高优先级邮件的关键词
     */
    private static final String[] URGENT_KEYWORDS = {
            "紧急", "urgent", "URGENT", "重要", "important", "IMPORTANT",
            "立即", "immediately", "ASAP", "asap", "火急", "emergency"
    };

    @Override
    public SendResult send(List<String> toList, List<String> ccList, List<String> bccList,
                           String subject, String content, boolean isHtml, List<File> attachments) {

        long startTime = System.currentTimeMillis();
        long duration;

        try {
            log.info("使用高优先级策略发送邮件: {}", subject);

            // 使用默认策略发送，但会在邮件中设置高优先级标识
            SendResult result = sendWithHighPriority(toList, ccList, bccList, subject, content, isHtml, attachments);

            duration = System.currentTimeMillis() - startTime;
            result.setDuration(duration);

            if (result.isSuccess()) {
                log.info("高优先级邮件发送成功，耗时: {}ms", duration);
            } else {
                log.error("高优先级邮件发送失败: {}", result.getErrorMessage());
            }

            return result;

        } catch (Exception e) {
            duration = System.currentTimeMillis() - startTime;
            log.error("高优先级邮件发送异常: {}", e.getMessage(), e);
            return SendResult.failure(toList, subject, e.getMessage(), "HIGH_PRIORITY_SEND_ERROR", getStackTrace(e), duration);
        }
    }

    @Override
    public String getStrategyName() {
        return "HighPriorityStrategy";
    }

    @Override
    public String getDescription() {
        return "高优先级邮件发送策略，适用于紧急邮件，设置高优先级标识并使用更短的超时时间";
    }

    @Override
    public boolean supports(int recipientCount, boolean hasAttachments, boolean isHtml) {
        // 支持所有类型的邮件，但主要用于紧急邮件
        return true;
    }

    @Override
    public int getPriority() {
        // 最高优先级
        return 200;
    }

    /**
     * 检查是否为紧急邮件
     */
    public boolean isUrgentEmail(String subject, String content) {
        if (subject == null && content == null) {
            return false;
        }

        String textToCheck = (subject != null ? subject : "") + " " + (content != null ? content : "");
        textToCheck = textToCheck.toLowerCase();

        for (String keyword : URGENT_KEYWORDS) {
            if (textToCheck.contains(keyword.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    /**
     * 发送高优先级邮件
     */
    private SendResult sendWithHighPriority(List<String> toList, List<String> ccList, List<String> bccList,
                                            String subject, String content, boolean isHtml, List<File> attachments) {
        long startTime = System.currentTimeMillis();
        long duration;

        try {
            // 创建邮件会话，使用更短的超时时间
            Properties props = createHighPriorityProperties();
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    // 这里需要从配置中获取认证信息
                    return new PasswordAuthentication("username", "password");
                }
            });

            // 创建邮件消息
            MimeMessage message = new MimeMessage(session);

            // 设置高优先级
            message.setHeader("X-Priority", "1");
            message.setHeader("X-MSMail-Priority", "High");
            message.setHeader("Importance", "High");

            // 设置收件人
            if (toList != null && !toList.isEmpty()) {
                InternetAddress[] toAddresses = toList.stream()
                        .map(email -> {
                            try {
                                return new InternetAddress(email);
                            } catch (Exception e) {
                                log.warn("无效的邮件地址: {}", email);
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .toArray(InternetAddress[]::new);
                message.setRecipients(Message.RecipientType.TO, toAddresses);
            }

            // 设置抄送
            if (ccList != null && !ccList.isEmpty()) {
                InternetAddress[] ccAddresses = ccList.stream()
                        .map(email -> {
                            try {
                                return new InternetAddress(email);
                            } catch (Exception e) {
                                log.warn("无效的抄送邮件地址: {}", email);
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .toArray(InternetAddress[]::new);
                message.setRecipients(Message.RecipientType.CC, ccAddresses);
            }

            // 设置密送
            if (bccList != null && !bccList.isEmpty()) {
                InternetAddress[] bccAddresses = bccList.stream()
                        .map(email -> {
                            try {
                                return new InternetAddress(email);
                            } catch (Exception e) {
                                log.warn("无效的密送邮件地址: {}", email);
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .toArray(InternetAddress[]::new);
                message.setRecipients(Message.RecipientType.BCC, bccAddresses);
            }

            // 设置主题（添加紧急标识）
            String urgentSubject = "[紧急] " + (subject != null ? subject : "");
            message.setSubject(urgentSubject, "UTF-8");

            // 设置内容
            if (isHtml) {
                message.setContent(content, "text/html;charset=UTF-8");
            } else {
                message.setText(content, "UTF-8");
            }

            // 处理附件（简化版，实际应该使用 MimeMultipart）
            if (attachments != null && !attachments.isEmpty()) {
                log.warn("高优先级策略暂不支持附件，将使用默认策略发送");
                return defaultStrategy.send(toList, ccList, bccList, urgentSubject, content, isHtml, attachments);
            }

            // 发送邮件
            Transport.send(message);

            duration = System.currentTimeMillis() - startTime;
            log.info("高优先级邮件发送成功，耗时: {}ms", duration);
            return SendResult.success(message.getMessageID(), toList, urgentSubject, duration);

        } catch (Exception e) {
            log.error("高优先级邮件发送失败: {}", e.getMessage(), e);
            duration = System.currentTimeMillis() - startTime;
            // 如果高优先级发送失败，降级使用默认策略
            log.info("降级使用默认策略发送邮件");
            return defaultStrategy.send(toList, ccList, bccList, subject, content, isHtml, attachments);
        }
    }

    /**
     * 创建高优先级邮件的属性配置
     */
    private Properties createHighPriorityProperties() {
        Properties props = new Properties();

        // SMTP 服务器配置（这里应该从配置文件读取）
        props.put("mail.smtp.host", "smtp.example.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        // 设置更短的超时时间（紧急邮件需要快速发送）
        props.put("mail.smtp.connectiontimeout", "5000");  // 5秒连接超时
        props.put("mail.smtp.timeout", "10000");          // 10秒读取超时
        props.put("mail.smtp.writetimeout", "10000");     // 10秒写入超时

        // SSL 配置
        props.put("mail.smtp.ssl.enable", "false");
        props.put("mail.smtp.ssl.trust", "*");

        return props;
    }

    /**
     * 获取异常堆栈信息
     */
    private String getStackTrace(Exception e) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}