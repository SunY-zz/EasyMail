package cn.sunyblog.easymail.mail;

import org.springframework.stereotype.Component;

import javax.mail.Message;

/**
 * 邮件处理函数接口
 * 用于函数式处理邮件，比实现接口更灵活
 */
@Component
@FunctionalInterface
public interface EasyMailProcessorFunction {
    /**
     * 处理邮件
     *
     * @param message 原始邮件消息
     * @param content 解析后的邮件内容
     * @param subject 邮件主题
     * @param from    发件人
     * @return 处理结果
     */
    Object process(Message message, String content, String subject, String from);
}