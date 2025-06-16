package cn.sunyblog.easymail.processor.handler;

import lombok.Builder;
import lombok.Data;

import javax.mail.Message;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 邮件处理上下文
 * 封装邮件处理过程中的所有相关信息
 *
 * @author suny
 * @version 1.0
 * @date 2025/06/14
 */
@Data
@Builder
public class EmailContext {

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
     * 发件人（别名）
     */
    private String sender;

    /**
     * 收件人列表
     */
    private List<String> to;

    /**
     * 收件人列表（别名）
     */
    private List<String> recipients;

    /**
     * 抄送列表
     */
    private List<String> cc;

    /**
     * 密送列表
     */
    private List<String> bcc;

    /**
     * 邮件内容（纯文本）
     */
    private String textContent;

    /**
     * 邮件内容（HTML）
     */
    private String htmlContent;

    /**
     * 邮件内容（通用）
     */
    private String content;

    /**
     * 邮件标签
     */
    private List<String> tags;

    /**
     * 附件信息
     */
    private List<AttachmentInfo> attachments;

    /**
     * 邮件接收时间
     */
    private LocalDateTime receivedTime;

    /**
     * 邮件发送时间
     */
    private LocalDateTime sentTime;

    /**
     * 邮件接收日期（别名）
     */
    private LocalDateTime receivedDate;

    /**
     * 邮件发送日期（别名）
     */
    private LocalDateTime sentDate;

    /**
     * 邮件大小（字节）
     */
    private long size;

    /**
     * 邮件大小（Long类型）
     */
    private Long sizeAsLong;

    /**
     * 邮件优先级
     */
    private String priority;

    /**
     * 邮件编码
     */
    private String encoding;

    /**
     * 扩展属性
     */
    private Map<String, Object> attributes;

    /**
     * 处理开始时间
     */
    private LocalDateTime processStartTime;

    /**
     * 处理器名称
     */
    private String processorName;

    /**
     * 附件信息
     */
    @Data
    @Builder
    public static class AttachmentInfo {
        /**
         * 文件名
         */
        private String fileName;
        /**
         * 文件大小
         */
        private long fileSize;
        /**
         * 文件类型
         */
        private String contentType;
        /**
         * 文件保存路径
         */
        private String filePath;
        /**
         * 是否内联
         */
        private boolean inline;
    }

    /**
     * 获取发件人（兼容性方法）
     */
    public String getSender() {
        return sender != null ? sender : from;
    }

    /**
     * 获取收件人列表（兼容性方法）
     */
    public List<String> getRecipients() {
        return recipients != null ? recipients : to;
    }

    /**
     * 获取邮件内容（兼容性方法）
     */
    public String getContent() {
        if (content != null) return content;
        if (textContent != null) return textContent;
        return htmlContent;
    }

    /**
     * 获取接收日期（兼容性方法）
     */
    public LocalDateTime getReceivedDate() {
        return receivedDate != null ? receivedDate : receivedTime;
    }

    /**
     * 获取发送日期（兼容性方法）
     */
    public LocalDateTime getSentDate() {
        return sentDate != null ? sentDate : sentTime;
    }

    /**
     * 获取邮件大小（Long类型）
     */
    public Long getSize() {
        return sizeAsLong != null ? sizeAsLong : size;
    }

    /**
     * 获取扩展属性
     *
     * @param key 属性键
     * @param <T> 属性值类型
     * @return 属性值
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return attributes != null ? (T) attributes.get(key) : null;
    }

    /**
     * 设置扩展属性
     *
     * @param key   属性键
     * @param value 属性值
     */
    public void setAttribute(String key, Object value) {
        if (attributes != null) {
            attributes.put(key, value);
        }
    }

    /**
     * 检查是否包含指定标签
     *
     * @param tag 标签
     * @return 是否包含
     */
    public boolean hasTag(String tag) {
        return tags != null && tags.contains(tag);
    }

    /**
     * 检查是否有附件
     *
     * @return 是否有附件
     */
    public boolean hasAttachments() {
        return attachments != null && !attachments.isEmpty();
    }
}