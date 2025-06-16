package cn.sunyblog.easymail.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.mail.Message;

/**
 * 邮件事件类
 * 用于在邮件处理过程中传递信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailEvent {

    /**
     * 原始邮件消息
     */
    private Message message;

    /**
     * 邮件ID
     */
    private String messageId;

    /**
     * 邮件主题
     */
    private String subject;

    /**
     * 发件人
     */
    private String from;

    /**
     * 邮件内容
     */
    private String content;

    /**
     * 接收时间
     */
    private long receivedTime;

    /**
     * 处理结果
     */
    private boolean processed;
}