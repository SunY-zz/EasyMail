package cn.sunyblog.easymail.mail.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import javax.mail.Message;
import javax.mail.MessagingException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 默认的邮件缓存管理器实现
 * 基于内存的缓存实现，提供基本的邮件去重功能
 * <p>
 * 特性：
 * - 线程安全的内存缓存
 * - 自动过期清理
 * - 统计信息收集
 * - 可配置的缓存大小限制
 *
 * @author suny
 * @version 1.0.0
 */
@Slf4j
@Component
@ConditionalOnMissingBean(EmailCacheManager.class)
public class DefaultEmailCacheManager implements EmailCacheManager {

    /**
     * 缓存存储
     */
    private final Map<String, EmailCacheInfo> cache = new ConcurrentHashMap<>();

    /**
     * 统计信息
     */
    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);
    private volatile LocalDateTime lastCleanTime = LocalDateTime.now();

    /**
     * 配置参数
     */
    private final int maxCacheSize = 10000; // 最大缓存条目数
    private final int cleanupThreshold = 8000; // 清理阈值

    @Override
    public boolean shouldProcessMessage(Message message) {
        try {
            String messageId = extractMessageId(message);
            if (messageId == null) {
                log.warn("无法提取邮件ID，默认处理");
                missCount.incrementAndGet();
                return true;
            }

            boolean exists = cache.containsKey(messageId);
            if (exists) {
                hitCount.incrementAndGet();
                log.debug("邮件已处理过: {}", messageId);
                return false;
            } else {
                missCount.incrementAndGet();
                log.debug("新邮件，需要处理: {}", messageId);
                return true;
            }

        } catch (Exception e) {
            log.error("检查邮件处理状态异常: {}", e.getMessage(), e);
            missCount.incrementAndGet();
            return true; // 异常情况下默认处理
        }
    }

    @Override
    public void markAsProcessed(Message message) {
        try {
            String messageId = extractMessageId(message);
            if (messageId == null) {
                log.warn("无法提取邮件ID，跳过缓存");
                return;
            }

            String subject = message.getSubject();
            String from = extractFrom(message);
            LocalDateTime receivedTime = extractReceivedTime(message);

            EmailCacheInfo cacheInfo = new EmailCacheInfo(
                    messageId, subject, from, LocalDateTime.now(),
                    receivedTime, "PROCESSED", "DefaultProcessor"
            );

            cache.put(messageId, cacheInfo);
            log.debug("邮件已标记为已处理: {}", messageId);

            // 检查是否需要清理缓存
            if (cache.size() > maxCacheSize) {
                cleanupCache();
            }

        } catch (Exception e) {
            log.error("标记邮件为已处理异常: {}", e.getMessage(), e);
        }
    }

    @Override
    public Optional<EmailCacheInfo> getCacheInfo(String messageId) {
        EmailCacheInfo info = cache.get(messageId);
        if (info != null) {
            hitCount.incrementAndGet();
        } else {
            missCount.incrementAndGet();
        }
        return Optional.ofNullable(info);
    }

    @Override
    public List<String> getAllCachedMessageIds() {
        return new ArrayList<>(cache.keySet());
    }

    @Override
    public int cleanExpiredEntries(LocalDateTime expireTime) {
        int removedCount = 0;
        Iterator<Map.Entry<String, EmailCacheInfo>> iterator = cache.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, EmailCacheInfo> entry = iterator.next();
            EmailCacheInfo info = entry.getValue();

            if (info.getProcessedTime().isBefore(expireTime)) {
                iterator.remove();
                removedCount++;
            }
        }

        if (removedCount > 0) {
            log.info("清理过期缓存条目: {} 个", removedCount);
            lastCleanTime = LocalDateTime.now();
        }

        return removedCount;
    }

    @Override
    public void clearAll() {
        int size = cache.size();
        cache.clear();
        hitCount.set(0);
        missCount.set(0);
        lastCleanTime = LocalDateTime.now();
        log.info("清空所有缓存，共清理 {} 个条目", size);
    }

    @Override
    public CacheStatistics getStatistics() {
        long memoryUsage = estimateMemoryUsage();
        return new CacheStatistics(
                cache.size(),
                hitCount.get(),
                missCount.get(),
                memoryUsage,
                lastCleanTime
        );
    }

    /**
     * 提取邮件ID
     */
    private String extractMessageId(Message message) {
        try {
            // 尝试获取Message-ID头
            String[] messageIds = message.getHeader("Message-ID");
            if (messageIds != null && messageIds.length > 0) {
                return messageIds[0];
            }

            // 如果没有Message-ID，使用主题+发件人+接收时间的组合
            String subject = message.getSubject();
            String from = extractFrom(message);
            Date receivedDate = message.getReceivedDate();

            if (subject != null && from != null && receivedDate != null) {
                return String.format("%s_%s_%d",
                        subject.hashCode(), from.hashCode(), receivedDate.getTime());
            }

            return null;

        } catch (MessagingException e) {
            log.error("提取邮件ID异常: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 提取发件人
     */
    private String extractFrom(Message message) {
        try {
            if (message.getFrom() != null && message.getFrom().length > 0) {
                return message.getFrom()[0].toString();
            }
        } catch (MessagingException e) {
            log.error("提取发件人异常: {}", e.getMessage(), e);
        }
        return "unknown";
    }

    /**
     * 提取接收时间
     */
    private LocalDateTime extractReceivedTime(Message message) {
        try {
            Date receivedDate = message.getReceivedDate();
            if (receivedDate != null) {
                return LocalDateTime.ofInstant(receivedDate.toInstant(),
                        java.time.ZoneId.systemDefault());
            }
        } catch (MessagingException e) {
            log.error("提取接收时间异常: {}", e.getMessage(), e);
        }
        return LocalDateTime.now();
    }

    /**
     * 清理缓存（保留最近的条目）
     */
    private void cleanupCache() {
        if (cache.size() <= cleanupThreshold) {
            return;
        }

        log.info("开始清理缓存，当前条目数: {}", cache.size());

        // 按处理时间排序，保留最新的条目
        List<Map.Entry<String, EmailCacheInfo>> sortedEntries = cache.entrySet()
                .stream()
                .sorted((e1, e2) -> e2.getValue().getProcessedTime()
                        .compareTo(e1.getValue().getProcessedTime()))
                .collect(Collectors.toList());

        // 清空缓存并重新添加最新的条目
        cache.clear();
        for (int i = 0; i < Math.min(cleanupThreshold, sortedEntries.size()); i++) {
            Map.Entry<String, EmailCacheInfo> entry = sortedEntries.get(i);
            cache.put(entry.getKey(), entry.getValue());
        }

        lastCleanTime = LocalDateTime.now();
        log.info("缓存清理完成，保留条目数: {}", cache.size());
    }

    /**
     * 估算内存使用量
     */
    private long estimateMemoryUsage() {
        // 简单估算：每个缓存条目大约占用200字节
        return cache.size() * 200L;
    }
}