package cn.sunyblog.easymail.send.strategy;

import cn.sunyblog.easymail.send.EasyMailSendResult;

import java.io.File;
import java.util.List;

/**
 * 邮件发送策略接口
 * 使用策略模式支持不同的邮件发送方式
 *
 * @author suny
 * @version 1.0.0
 */
public interface EasyMailSendStrategy {

    /**
     * 发送邮件
     *
     * @param toList      收件人列表
     * @param ccList      抄送人列表
     * @param bccList     密送人列表
     * @param subject     邮件主题
     * @param content     邮件内容
     * @param isHtml      是否为HTML格式
     * @param attachments 附件列表
     * @return 发送结果
     */
    EasyMailSendResult send(List<String> toList, List<String> ccList, List<String> bccList,
                            String subject, String content, boolean isHtml, List<File> attachments);

    /**
     * 获取策略名称
     *
     * @return 策略名称
     */
    String getStrategyName();

    /**
     * 获取策略描述
     *
     * @return 策略描述
     */
    String getDescription();

    /**
     * 检查策略是否支持指定的发送场景
     *
     * @param recipientCount 收件人总数
     * @param hasAttachments 是否有附件
     * @param isHtml         是否为HTML格式
     * @return 是否支持
     */
    boolean supports(int recipientCount, boolean hasAttachments, boolean isHtml);

    /**
     * 获取策略优先级（数值越小优先级越高）
     *
     * @return 优先级
     */
    int getPriority();
}