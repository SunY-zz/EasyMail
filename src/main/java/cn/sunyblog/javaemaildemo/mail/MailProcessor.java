package cn.sunyblog.javaemaildemo.mail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.MessagingException;
import java.io.IOException;

/**
 * @author suny
 * @version 1.0
 * @description: 邮件处理器  负责处理单个邮件，包括获取内容、保存附件等
 * @date 2025/05/12 16:24
 */
@Slf4j
@Component
public class MailProcessor {

    @Resource
    private MailConfig mailConfig;
    @Resource
    private MailContentParser contentParser;
    @Resource
    private MailCache mailCache;

    /**
     * 处理单封邮件
     *
     * @param message 邮件消息
     * @return 是否成功处理
     */
    public boolean processMessage(Message message) {
        try {
            // 获取邮件ID
            String messageId = mailCache.getMessageId(message);

            // 原子性检查并标记为已处理
            if (!mailCache.checkAndMarkAsProcessed(messageId)) {
                log.debug("邮件已处理，跳过: {}", messageId);
                return false;
            }

            long startTime = System.currentTimeMillis();
            String subject = mailCache.getSubjectSafely(message);
            log.info("开始处理邮件，主题: {}", subject);

            // 解析邮件内容
            String emailContent = contentParser.parseContent(message, mailConfig.getAttachmentDir());

            // 检查是否是验证码邮件并处理
            processVerificationCodeEmail(subject, emailContent);

            long endTime = System.currentTimeMillis();
            long processDuration = endTime - startTime;
            log.info("邮件处理完成，耗时: {}毫秒", processDuration);

            try {
                // 标记为已读
                message.setFlag(Flags.Flag.SEEN, true);
            } catch (MessagingException ex) {
                log.error("标记邮件为已读失败: {}", ex.getMessage(), ex);
            }

            return true;
        } catch (MessagingException | IOException e) {
            log.error("处理邮件异常: {}", e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("处理邮件未预期异常: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 处理验证码邮件
     *
     * @param subject 邮件主题
     * @param content 邮件内容
     */
    private void processVerificationCodeEmail(String subject, String content) {

    }


    /**
     * 获取邮件处理统计
     *
     * @return 处理统计信息
     */
    public String getProcessingStats() {
        return "已处理邮件数: " + mailCache.getProcessedCount();
    }
}
