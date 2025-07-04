package cn.sunyblog.easymail.mail;

import cn.sunyblog.easymail.api.EasyMailListenerApi;
import cn.sunyblog.easymail.config.EasyMailImapConfig;
import cn.sunyblog.easymail.processor.config.AnnotationDrivenEasyMailProcessorManager;
import cn.sunyblog.easymail.processor.handler.EasyMailContextBuilder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import java.io.IOException;

import cn.sunyblog.easymail.exception.EasyMailExceptionHandler;
import cn.sunyblog.easymail.exception.EasyMailProcessException;

/**
 * @author suny
 * @version 1.0
 * @description: 邮件处理器  负责处理单个邮件，包括获取内容、保存附件等
 * @date 2025/05/12 16:24
 */
@Slf4j
@Data
@Component
public class EasyMailProcessor {

    @Autowired
    private EasyMailImapConfig easyMailImapConfig;
    @Autowired
    private EasyMailContentParser contentParser;
    @Autowired
    private EasyMailCache easyMailCache;
    @Autowired
    private EasyMailListenerApi easyMailListenerApi;

    // 移除对EasyMailService的直接依赖，避免循环依赖
    // 使用volatile确保线程安全的状态检查
    private volatile boolean serviceRunning = true;

    @Autowired(required = false)
    private AnnotationDrivenEasyMailProcessorManager annotationProcessorManager;

    @Autowired(required = false)
    private EasyMailContextBuilder easyMailContextBuilder;

    @Autowired(required = false)
    private EasyMailProcessorFunction easyMailProcessorFunction;

    /**
     * 处理单封邮件
     *
     * @param message 邮件消息
     * @return 是否成功处理
     */
    public boolean processMessage(Message message) {
        // 检查邮件服务是否正在运行，如果已关闭则不处理
        if (!serviceRunning) {
            log.debug("邮件服务已关闭，跳过邮件处理");
            return false;
        }

        // 检查当前线程是否被中断
        if (Thread.currentThread().isInterrupted()) {
            log.debug("处理线程已被中断，跳过邮件处理");
            return false;
        }

        try {
            // 获取邮件ID
            String messageId = easyMailCache.getMessageId(message);

            // 原子性检查并标记为已处理
            if (!easyMailCache.checkAndMarkAsProcessed(messageId)) {
                log.debug("邮件已处理，跳过: {}", messageId);
                return false;
            }

            long startTime = System.currentTimeMillis();
            String subject = easyMailCache.getSubjectSafely(message);
            log.debug("开始处理邮件，主题: {}", subject);

            // 获取发件人
            String from = "(未知发件人)";
            Address[] fromAddresses = message.getFrom();
            if (fromAddresses != null && fromAddresses.length > 0 && fromAddresses[0] != null) {
                from = contentParser.decodeText(fromAddresses[0].toString());
            }

            // 解析邮件内容（只解析一次）
            String emailContent;

            // 处理邮件
            boolean processed = false;

            // 首先尝试使用注解驱动处理器
            if (annotationProcessorManager != null && annotationProcessorManager.hasRegisteredHandlers()) {
                try {
                    // 再次检查服务状态和线程中断状态
                    if (!serviceRunning || Thread.currentThread().isInterrupted()) {
                        log.debug("邮件服务已关闭或线程被中断，停止注解驱动处理");
                        return false;
                    }
                    // 直接使用原始message，避免重复解析
                    processed = annotationProcessorManager.processEmail(message, easyMailImapConfig.getAttachmentDir());
                    log.info("注解驱动邮件处理完成");
                } catch (Exception e) {
                    // 如果是中断异常，直接返回
                    if (Thread.currentThread().isInterrupted()) {
                        log.debug("注解驱动处理被中断");
                        Thread.currentThread().interrupt();
                        return false;
                    }
                    EasyMailProcessException processEx = EasyMailProcessException.processingError("注解驱动处理器处理失败，回退到其他处理器", e);
                    log.error(processEx.getFullErrorMessage(), processEx);
                }
            }

            // 使用函数式处理方式
            if (!processed && easyMailProcessorFunction != null) {
                try {
                    // 检查服务状态和线程中断状态
                    if (!serviceRunning || Thread.currentThread().isInterrupted()) {
                        log.debug("邮件服务已关闭或线程被中断，停止函数式处理");
                        return false;
                    }
                    // 只有在需要时才解析邮件内容
                    emailContent = contentParser.parseContent(message, easyMailImapConfig.getAttachmentDir());
                    Object result = easyMailProcessorFunction.process(message, emailContent, subject, from);
                    processed = (result != null);
                    log.info("函数式邮件处理结果: {}", result);
                } catch (Exception e) {
                    // 如果是中断异常，直接返回
                    if (Thread.currentThread().isInterrupted()) {
                        log.debug("函数式处理被中断");
                        Thread.currentThread().interrupt();
                        return false;
                    }
                    EasyMailProcessException processEx = EasyMailProcessException.processingError("函数式邮件处理异常", e);
                    log.error(processEx.getFullErrorMessage(), processEx);
                }
            }
            // 其次使用接口方式
            if (!processed && easyMailListenerApi != null) {
                try {
                    // 检查服务状态和线程中断状态
                    if (!serviceRunning || Thread.currentThread().isInterrupted()) {
                        log.debug("邮件服务已关闭或线程被中断，停止接口处理");
                        return false;
                    }
                    // 只有在需要时才解析邮件内容
                    emailContent = contentParser.parseContent(message, easyMailImapConfig.getAttachmentDir());
                    processed = easyMailListenerApi.processEmail(message, emailContent, subject, from);
                    log.info("邮件处理器[{}]处理结果: {}", easyMailListenerApi.getProcessorName(), processed);
                } catch (Exception e) {
                    // 如果是中断异常，直接返回
                    if (Thread.currentThread().isInterrupted()) {
                        log.debug("接口处理被中断");
                        Thread.currentThread().interrupt();
                        return false;
                    }
                    EasyMailProcessException processEx = EasyMailProcessException.processingError("邮件处理器[" + easyMailListenerApi.getProcessorName() + "]处理异常", e);
                    log.error(processEx.getFullErrorMessage(), processEx);
                }
            }
            // 最后使用默认处理方法
            if (!processed) {
                // 检查服务状态和线程中断状态
                if (!serviceRunning || Thread.currentThread().isInterrupted()) {
                    log.debug("邮件服务已关闭或线程被中断，停止默认处理");
                    return false;
                }
                // 只有在需要时才解析邮件内容
                emailContent = contentParser.parseContent(message, easyMailImapConfig.getAttachmentDir());
                processed = processVerificationCodeEmail(subject, emailContent);
            }

            long endTime = System.currentTimeMillis();
            long processDuration = endTime - startTime;
            log.info("邮件处理完成，耗时: {}毫秒", processDuration);

            try {
                // 检查服务状态，避免在服务关闭后操作邮件
                if (!serviceRunning) {
                    log.debug("邮件服务已关闭，跳过标记邮件为已读");
                    return processed;
                }

                // 检查邮件和文件夹状态
                if (!message.isExpunged()) {
                    Folder messageFolder = message.getFolder();
                    if (messageFolder != null && messageFolder.isOpen()) {
                        // 标记为已读
                        message.setFlag(Flags.Flag.SEEN, true);
                        log.debug("邮件已标记为已读");
                    } else {
                        log.warn("邮件文件夹未打开，无法标记邮件为已读");
                    }
                } else {
                    log.warn("邮件对象无效或已删除，无法标记为已读");
                }
            } catch (javax.mail.FolderClosedException ex) {
                // 文件夹已关闭异常，通常发生在服务停止时
                if (serviceRunning) {
                    log.warn("邮件文件夹已关闭，无法标记邮件为已读: {}", ex.getMessage());
                } else {
                    log.debug("邮件处理服务已停止，标记操作被中断: {}", ex.getMessage());
                }
            } catch (MessagingException ex) {
                // 只有在服务运行时才记录为错误
                if (serviceRunning) {
                    EasyMailProcessException processEx = (EasyMailProcessException) EasyMailExceptionHandler.wrapMessagingException(ex, "标记邮件为已读失败");
                    log.error(processEx.getFullErrorMessage(), processEx);
                } else {
                    log.debug("邮件处理服务已停止，标记操作被中断: {}", ex.getMessage());
                }
            }

            return processed;
        } catch (javax.mail.FolderClosedException e) {
            // 文件夹已关闭异常，通常发生在服务停止时，这是正常现象
            if (serviceRunning) {
                log.warn("邮件文件夹已关闭，可能服务正在停止: {}", e.getMessage());
            } else {
                log.debug("邮件处理服务已停止，文件夹操作被中断: {}", e.getMessage());
            }
            return false;
        } catch (MessagingException e) {
            // 只有在服务运行时才记录为错误
            if (serviceRunning) {
                EasyMailProcessException processEx = (EasyMailProcessException) EasyMailExceptionHandler.wrapMessagingException(e, "处理邮件异常");
                log.error(processEx.getFullErrorMessage(), processEx);
            } else {
                log.debug("邮件处理服务已停止，邮件操作被中断: {}", e.getMessage());
            }
            return false;
        } catch (IOException e) {
            // 只有在服务运行时才记录为错误
            if (serviceRunning) {
                EasyMailProcessException processEx = EasyMailProcessException.processingError("处理邮件IO异常", e);
                log.error(processEx.getFullErrorMessage(), processEx);
            } else {
                log.debug("邮件处理服务已停止，IO操作被中断: {}", e.getMessage());
            }
            return false;
        } catch (Exception e) {
            // 只有在服务运行时才记录为错误
            if (serviceRunning) {
                EasyMailProcessException processEx = EasyMailProcessException.processingError("处理邮件失败", e);
                log.error(processEx.getFullErrorMessage(), processEx);
            } else {
                log.debug("邮件处理服务已停止，处理被中断: {}", e.getMessage());
            }
            return false;
        }
    }

    /**
     * 处理验证码邮件
     *
     * @param subject 邮件主题
     * @param content 邮件内容
     * @return 处理结果
     */
    private boolean processVerificationCodeEmail(String subject, String content) {
        // 默认实现，可以在这里添加验证码邮件的处理逻辑
        log.info("默认邮件处理: 主题={}", subject);
        log.debug("邮件内容: {}", content);
        return true;
    }


    /**
     * 获取邮件处理统计
     *
     * @return 处理统计信息
     */
    public String getProcessingStats() {
        return "已处理邮件数: " + easyMailCache.getProcessedCount();
    }

    /**
     * 设置邮件处理函数
     * 这种方式比实现接口更灵活，可以直接传入lambda表达式处理邮件
     *
     * @param processorFunction 邮件处理函数
     */
    public void setEasyMailProcessorFunction(EasyMailProcessorFunction processorFunction) {
        this.easyMailProcessorFunction = processorFunction;
        log.info("已设置函数式邮件处理器");
    }

    /**
     * 设置服务运行状态
     * 由EasyMailService调用来通知处理器服务状态变化
     *
     * @param running 服务是否正在运行
     */
    public void setServiceRunning(boolean running) {
        this.serviceRunning = running;
        log.debug("邮件处理器服务状态更新: {}", running ? "运行中" : "已停止");
    }

    /**
     * 检查服务是否正在运行
     *
     * @return 服务运行状态
     */
    public boolean isServiceRunning() {
        return serviceRunning;
    }
}
