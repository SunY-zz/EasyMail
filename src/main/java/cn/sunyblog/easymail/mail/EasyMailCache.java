package cn.sunyblog.easymail.mail;

import cn.sunyblog.easymail.exception.EasyMailProcessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author suny
 * @version 1.0
 * @description: 邮件缓存服务   负责记录已处理的邮件ID，避免重复处理
 * @date 2025/05/12 16:25
 */
@Slf4j
@Component
public class EasyMailCache {

    // 用于记录已处理邮件的ID，避免重复处理
    private final Set<String> processedMessageIds = Collections.newSetFromMap(new ConcurrentHashMap<>());

    // 最大缓存大小，避免内存泄漏
    private static final int MAX_CACHE_SIZE = 1000;

    /**
     * 获取邮件的唯一ID
     * 尝试使用Message-ID头，如果没有则使用发件人+主题+接收时间的组合
     *
     * @param message 邮件
     * @return 唯一ID
     */
    public String getMessageId(Message message) {
        try {
            // 首先尝试获取Message-ID头
            String[] headers = message.getHeader("Message-ID");
            if (headers != null && headers.length > 0) {
                return headers[0];
            }

            // 如果没有Message-ID，则使用主题+发件人+时间的组合
            StringBuilder sb = new StringBuilder();
            if (message.getSubject() != null) {
                sb.append(message.getSubject());
            }

            Address[] addresses = message.getFrom();
            if (addresses != null && addresses.length > 0) {
                sb.append("_").append(addresses[0].toString());
            }

            sb.append("_").append(message.getReceivedDate());

            return sb.toString();
        } catch (Exception e) {
            // 如果获取ID失败，抛出自定义异常
            throw EasyMailProcessException.parsingError("无法获取邮件ID", e);
        }
    }

    /**
     * 获取邮件主题，避免空指针异常
     *
     * @param message 邮件
     * @return 邮件主题
     */
    public String getSubjectSafely(Message message) {
        try {
            return message.getSubject() != null ? message.getSubject() : "(无主题)";
        } catch (MessagingException e) {
            throw EasyMailProcessException.parsingError("获取邮件主题异常", e);
        }
    }

    /**
     * 判断是否应该处理该邮件
     *
     * @param messageId 邮件ID
     * @return 如果邮件未处理过则返回true
     */
    public boolean shouldProcessMessage(String messageId) {
        return !processedMessageIds.contains(messageId);
    }

    /**
     * 检查并标记邮件为已处理（原子操作）
     *
     * @param messageId 邮件ID
     * @return 如果邮件之前未处理过则返回true，否则返回false
     */
    public synchronized boolean checkAndMarkAsProcessed(String messageId) {
        if (processedMessageIds.contains(messageId)) {
            return false; // 已处理过
        }

        // 标记为已处理
        processedMessageIds.add(messageId);

        // 如果缓存超过上限，清理一部分
        if (processedMessageIds.size() > MAX_CACHE_SIZE) {
            log.info("邮件ID缓存达到上限，正在清理...");
            // 通过创建新集合的方式，保留后面的一半ID
            List<String> idList = new ArrayList<>(processedMessageIds);
            int startIndex = idList.size() / 2;
            Set<String> newSet = new HashSet<>(idList.subList(startIndex, idList.size()));
            processedMessageIds.clear();
            processedMessageIds.addAll(newSet);
            log.info("邮件ID缓存清理完成，当前大小: {}", processedMessageIds.size());
        }

        return true; // 之前未处理过
    }

    /**
     * 获取已处理邮件数量
     *
     * @return 缓存中的邮件数量
     */
    public int getProcessedCount() {
        return processedMessageIds.size();
    }

    /**
     * 清空缓存
     */
    public void clearCache() {
        processedMessageIds.clear();
        log.info("邮件ID缓存已清空");
    }
}