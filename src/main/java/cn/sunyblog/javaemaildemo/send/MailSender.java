package cn.sunyblog.javaemaildemo.send;

import cn.sunyblog.javaemaildemo.config.SmtpConfig;
import cn.sunyblog.javaemaildemo.util.RetryUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.annotation.Resource;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 邮件发送器
 * 负责发送普通邮件和带附件的邮件
 */
@Slf4j
@Data
@Component
public class MailSender {

    @Resource
    private SmtpConfig smtpConfig;
    @Resource
    private ThreadPoolExecutor executor;
    
    /**
     * 创建邮件会话
     *
     * @return 邮件会话对象
     */
    public Session createSession() {
        Properties props = configureProperties();
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                // 确保使用完整的邮箱地址作为用户名
                String username = smtpConfig.getUsername();
                log.info("使用认证用户名: {}", username);
                return new PasswordAuthentication(username, smtpConfig.getPassword());
            }
        });
        // 设置调试模式，默认关闭
        session.setDebug(smtpConfig.getLog().isDebugEnabled()); // 启用调试模式以获取详细错误信息
        return session;
    }

    /**
     * 配置邮件属性
     *
     * @return 配置好的属性对象
     */
    private Properties configureProperties() {
        Properties props = new Properties();
        // 基本连接属性
        props.put("mail.smtp.auth", String.valueOf(smtpConfig.getProperties().isMailSmtpAuth()));
        props.put("mail.smtp.host", smtpConfig.getServer());
        props.put("mail.smtp.port", smtpConfig.getPort());
        
        // STARTTLS设置
        props.put("mail.smtp.starttls.enable", String.valueOf(smtpConfig.getProperties().isMailSmtpStarttlsEnable()));
        
        // SSL设置 - 新浪邮箱需要SSL
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.ssl.trust", smtpConfig.getServer());
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.socketFactory.port", smtpConfig.getPort());
        
        // 连接超时参数
        props.put("mail.smtp.connectiontimeout", String.valueOf(smtpConfig.getConnection().getTimeout()));
        props.put("mail.smtp.timeout", String.valueOf(smtpConfig.getConnection().getReadTimeout()));
        props.put("mail.smtp.writetimeout", String.valueOf(smtpConfig.getConnection().getWriteTimeout()));
        
        return props;
    }

    /**
     * 发送简单文本邮件
     *
     * @param to 收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容
     * @return 是否发送成功
     */
    public boolean sendSimpleEmail(String to, String subject, String content) {
        return sendEmail(to, null, null, subject, content, false, null);
    }

    /**
     * 发送HTML格式邮件
     *
     * @param to 收件人邮箱
     * @param subject 邮件主题
     * @param htmlContent HTML格式的邮件内容
     * @return 是否发送成功
     */
    public boolean sendHtmlEmail(String to, String subject, String htmlContent) {
        return sendEmail(to, null, null, subject, htmlContent, true, null);
    }

    /**
     * 发送带抄送的邮件
     *
     * @param to 收件人邮箱
     * @param cc 抄送人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容
     * @param isHtml 是否为HTML格式
     * @return 是否发送成功
     */
    public boolean sendEmailWithCC(String to, String cc, String subject, String content, boolean isHtml) {
        return sendEmail(to, cc, null, subject, content, isHtml, null);
    }

    /**
     * 发送带密送的邮件
     *
     * @param to 收件人邮箱
     * @param bcc 密送人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容
     * @param isHtml 是否为HTML格式
     * @return 是否发送成功
     */
    public boolean sendEmailWithBCC(String to, String bcc, String subject, String content, boolean isHtml) {
        return sendEmail(to, null, bcc, subject, content, isHtml, null);
    }

    /**
     * 发送带附件的邮件
     *
     * @param to 收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容
     * @param isHtml 是否为HTML格式
     * @param attachments 附件列表
     * @return 是否发送成功
     */
    public boolean sendEmailWithAttachments(String to, String subject, String content, boolean isHtml, List<File> attachments) {
        return sendEmail(to, null, null, subject, content, isHtml, attachments);
    }

    /**
     * 批量发送邮件（相同内容）- 使用线程池并行发送
     *
     * @param toList 收件人列表
     * @param subject 邮件主题
     * @param content 邮件内容
     * @param isHtml 是否为HTML格式
     * @return 成功发送的邮件数量
     */
    public int sendBatchEmails(List<String> toList, String subject, String content, boolean isHtml) {
        log.info("开始批量发送邮件，收件人数量: {}", toList.size());
        
        // 使用计数器跟踪成功发送的邮件数量
        java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(toList.size());
        
        // 并行发送邮件
        for (String to : toList) {
            executor.execute(() -> {
                try {
                    boolean success = sendEmail(to, null, null, subject, content, isHtml, null);
                    if (success) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    log.error("发送邮件给 {} 时发生异常: {}", to, e.getMessage(), e);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        try {
            // 等待所有邮件发送完成
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("等待邮件发送完成时被中断", e);
        }
        
        int finalCount = successCount.get();
        log.info("批量发送邮件完成，成功: {}/{}", finalCount, toList.size());
        return finalCount;
    }
    
    /**
     * 异步发送简单文本邮件
     *
     * @param to 收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容
     */
    public void sendSimpleEmailAsync(String to, String subject, String content) {
        executor.execute(() -> sendEmail(to, null, null, subject, content, false, null));
        log.info("异步发送邮件任务已提交，收件人: {}, 主题: {}", to, subject);
    }
    
    /**
     * 异步发送HTML格式邮件
     *
     * @param to 收件人邮箱
     * @param subject 邮件主题
     * @param htmlContent HTML格式的邮件内容
     */
    public void sendHtmlEmailAsync(String to, String subject, String htmlContent) {
        executor.execute(() -> sendEmail(to, null, null, subject, htmlContent, true, null));
        log.info("异步发送HTML邮件任务已提交，收件人: {}, 主题: {}", to, subject);
    }
    
    /**
     * 带回调的异步发送邮件
     *
     * @param to 收件人邮箱
     * @param cc 抄送人邮箱
     * @param bcc 密送人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容
     * @param isHtml 是否为HTML格式
     * @param attachments 附件列表
     * @param callback 发送完成后的回调函数
     */
    public void sendEmailAsync(String to, String cc, String bcc, String subject, String content, 
                              boolean isHtml, List<File> attachments, java.util.function.Consumer<Boolean> callback) {
        executor.execute(() -> {
            boolean success = false;
            try {
                success = sendEmail(to, cc, bcc, subject, content, isHtml, attachments);
            } catch (Exception e) {
                log.error("异步发送邮件失败: {}", e.getMessage(), e);
            } finally {
                if (callback != null) {
                    callback.accept(success);
                }
            }
        });
        log.info("带回调的异步发送邮件任务已提交，收件人: {}, 主题: {}", to, subject);
    }
    
    /**
     * 批量异步发送邮件（相同内容）- 不等待完成
     *
     * @param toList 收件人列表
     * @param subject 邮件主题
     * @param content 邮件内容
     * @param isHtml 是否为HTML格式
     * @param finalCallback 所有邮件发送完成后的回调，参数为成功发送的邮件数量
     */
    public void sendBatchEmailsAsync(List<String> toList, String subject, String content, 
                                    boolean isHtml, java.util.function.Consumer<Integer> finalCallback) {
        log.info("开始批量异步发送邮件，收件人数量: {}", toList.size());
        
        // 使用计数器跟踪成功发送的邮件数量
        java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(toList.size());
        
        // 提交所有发送任务到线程池
        for (String to : toList) {
            executor.execute(() -> {
                try {
                    boolean success = sendEmail(to, null, null, subject, content, isHtml, null);
                    if (success) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    log.error("异步发送邮件给 {} 时发生异常: {}", to, e.getMessage(), e);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // 提交一个任务来等待所有邮件发送完成并执行回调
        executor.execute(() -> {
            try {
                latch.await();
                int finalCount = successCount.get();
                log.info("批量异步发送邮件完成，成功: {}/{}", finalCount, toList.size());
                
                if (finalCallback != null) {
                    finalCallback.accept(finalCount);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("等待邮件发送完成时被中断", e);
            }
        });
        
        log.info("批量异步发送邮件任务已全部提交");
    }
    
    /**
     * 获取当前线程池状态信息
     * 
     * @return 线程池状态信息
     */
    public String getThreadPoolStatus() {
        return String.format(
            "线程池状态 - 活动线程: %d, 完成任务: %d, 任务总数: %d, 队列大小: %d",
            executor.getActiveCount(),
            executor.getCompletedTaskCount(),
            executor.getTaskCount(),
            executor.getQueue().size()
        );
    }

    /**
     * 发送邮件的核心方法
     *
     * @param to 收件人邮箱
     * @param cc 抄送人邮箱
     * @param bcc 密送人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容
     * @param isHtml 是否为HTML格式
     * @param attachments 附件列表
     * @return 是否发送成功
     */
    private boolean sendEmail(String to, String cc, String bcc, String subject, String content, boolean isHtml, List<File> attachments) {
        // 如果重试功能未启用，直接调用原始发送方法
        if (!smtpConfig.getRetry().isEnabled()) {
            try {
                return sendEmailInternal(to, cc, bcc, subject, content, isHtml, attachments);
            } catch (MessagingException | UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
        
        // 配置重试策略
        RetryUtil.RetryConfig retryConfig = RetryUtil.RetryConfig.defaults()
                .maxAttempts(smtpConfig.getRetry().getMaxRetries() + 1) // +1是因为包括第一次尝试
                .initialDelayMs(smtpConfig.getRetry().getInitialDelayMs())
                .maxDelayMs(smtpConfig.getRetry().getMaxDelayMs())
                .useExponentialBackoff(smtpConfig.getRetry().isUseExponentialBackoff())
                .backoffMultiplier(smtpConfig.getRetry().getBackoffMultiplier())
                .retryableExceptions(
                    MessagingException.class,
                    SendFailedException.class,
                    javax.mail.MessagingException.class
                );
        
        try {
            // 使用重试工具执行邮件发送
            return RetryUtil.executeWithRetry(() -> sendEmailInternal(to, cc, bcc, subject, content, isHtml, attachments), retryConfig);
        } catch (Exception e) {
            log.error("邮件发送失败，已达到最大重试次数: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 内部邮件发送方法，实际执行发送邮件的逻辑
     *
     * @param to 收件人邮箱
     * @param cc 抄送人邮箱
     * @param bcc 密送人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容
     * @param isHtml 是否为HTML格式
     * @param attachments 附件列表
     * @return 是否发送成功
     * @throws MessagingException 如果发送过程中出现异常
     * @throws UnsupportedEncodingException 如果编码过程中出现异常
     */
    public boolean sendEmailInternal(String to, String cc, String bcc, String subject, String content, boolean isHtml, List<File> attachments) throws MessagingException, UnsupportedEncodingException {
        // 打印SMTP配置信息用于调试
        log.info("SMTP配置信息 - 服务器: {}, 端口: {}, 用户名: {}, 认证: {}, STARTTLS: {}", 
                smtpConfig.getServer(), 
                smtpConfig.getPort(), 
                smtpConfig.getUsername(), 
                smtpConfig.getProperties().isMailSmtpAuth(),
                smtpConfig.getProperties().isMailSmtpStarttlsEnable());
        
        Session session = createSession();
        MimeMessage message = new MimeMessage(session);
        
        // 设置发件人 - 使用完整邮箱地址
        message.setFrom(new InternetAddress(smtpConfig.getUsername(), "JavaEmailDemo", "UTF-8"));
        
        // 设置收件人
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        
        // 设置抄送人
        if (cc != null && !cc.isEmpty()) {
            message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(cc));
        }
        
        // 设置密送人
        if (bcc != null && !bcc.isEmpty()) {
            message.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(bcc));
        }
        
        // 设置主题
        message.setSubject(subject, "UTF-8");
        
        // 设置发送时间
        message.setSentDate(new Date());
        
        // 创建多部分消息
        Multipart multipart = new MimeMultipart();
        
        // 创建邮件正文
        MimeBodyPart messageBodyPart = new MimeBodyPart();
        if (isHtml) {
            messageBodyPart.setContent(content, "text/html;charset=UTF-8");
        } else {
            messageBodyPart.setText(content, "UTF-8");
        }
        multipart.addBodyPart(messageBodyPart);
        
        // 添加附件
        if (attachments != null && !attachments.isEmpty()) {
            for (File file : attachments) {
                MimeBodyPart attachmentPart = new MimeBodyPart();
                DataSource source = new FileDataSource(file);
                attachmentPart.setDataHandler(new DataHandler(source));
                attachmentPart.setFileName(MimeUtility.encodeText(file.getName()));
                multipart.addBodyPart(attachmentPart);
            }
        }
        
        // 设置邮件内容
        message.setContent(multipart);
        
        // 发送邮件
        Transport.send(message);
        log.info("邮件发送成功，收件人: {}, 主题: {}", to, subject);
        return true;
    }
}