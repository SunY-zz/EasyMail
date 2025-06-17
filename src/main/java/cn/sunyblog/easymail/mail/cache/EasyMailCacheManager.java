package cn.sunyblog.easymail.mail.cache;

import javax.mail.Message;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 邮件缓存管理器接口
 * 提供可扩展的邮件缓存功能，支持自定义实现
 * <p>
 * 默认实现基于内存缓存，用户可以实现此接口来提供：
 * - Redis缓存
 * - 数据库缓存
 * - 文件缓存
 * - 混合缓存策略
 *
 * @author suny
 * @version 1.0.0
 */
public interface EasyMailCacheManager {

    /**
     * 检查邮件是否应该被处理
     *
     * @param message 邮件消息
     * @return true表示应该处理，false表示已处理过
     */
    boolean shouldProcessMessage(Message message);

    /**
     * 标记邮件为已处理
     *
     * @param message 邮件消息
     */
    void markAsProcessed(Message message);

    /**
     * 获取邮件的缓存信息
     *
     * @param messageId 邮件ID
     * @return 缓存信息
     */
    Optional<EmailCacheInfo> getCacheInfo(String messageId);

    /**
     * 获取所有已缓存的邮件ID
     *
     * @return 邮件ID列表
     */
    List<String> getAllCachedMessageIds();

    /**
     * 清理过期的缓存条目
     *
     * @param expireTime 过期时间
     * @return 清理的条目数量
     */
    int cleanExpiredEntries(LocalDateTime expireTime);

    /**
     * 清空所有缓存
     */
    void clearAll();

    /**
     * 获取缓存统计信息
     *
     * @return 统计信息
     */
    CacheStatistics getStatistics();

    /**
     * 邮件缓存信息
     */
    class EmailCacheInfo {
        private final String messageId;
        private final String subject;
        private final String from;
        private final LocalDateTime processedTime;
        private final LocalDateTime receivedTime;
        private final String status;
        private final String processorName;

        public EmailCacheInfo(String messageId, String subject, String from,
                              LocalDateTime processedTime, LocalDateTime receivedTime,
                              String status, String processorName) {
            this.messageId = messageId;
            this.subject = subject;
            this.from = from;
            this.processedTime = processedTime;
            this.receivedTime = receivedTime;
            this.status = status;
            this.processorName = processorName;
        }

        // Getters
        public String getMessageId() {
            return messageId;
        }

        public String getSubject() {
            return subject;
        }

        public String getFrom() {
            return from;
        }

        public LocalDateTime getProcessedTime() {
            return processedTime;
        }

        public LocalDateTime getReceivedTime() {
            return receivedTime;
        }

        public String getStatus() {
            return status;
        }

        public String getProcessorName() {
            return processorName;
        }

        @Override
        public String toString() {
            return String.format("EmailCacheInfo{messageId='%s', subject='%s', from='%s', processedTime=%s, status='%s'}",
                    messageId, subject, from, processedTime, status);
        }
    }

    /**
     * 缓存统计信息
     */
    class CacheStatistics {
        private final long totalEntries;
        private final long hitCount;
        private final long missCount;
        private final double hitRate;
        private final long memoryUsage;
        private final LocalDateTime lastCleanTime;

        public CacheStatistics(long totalEntries, long hitCount, long missCount,
                               long memoryUsage, LocalDateTime lastCleanTime) {
            this.totalEntries = totalEntries;
            this.hitCount = hitCount;
            this.missCount = missCount;
            this.hitRate = hitCount + missCount > 0 ? (double) hitCount / (hitCount + missCount) : 0.0;
            this.memoryUsage = memoryUsage;
            this.lastCleanTime = lastCleanTime;
        }

        // Getters
        public long getTotalEntries() {
            return totalEntries;
        }

        public long getHitCount() {
            return hitCount;
        }

        public long getMissCount() {
            return missCount;
        }

        public double getHitRate() {
            return hitRate;
        }

        public long getMemoryUsage() {
            return memoryUsage;
        }

        public LocalDateTime getLastCleanTime() {
            return lastCleanTime;
        }

        @Override
        public String toString() {
            return String.format("CacheStatistics{totalEntries=%d, hitCount=%d, missCount=%d, hitRate=%.2f%%, memoryUsage=%d bytes}",
                    totalEntries, hitCount, missCount, hitRate * 100, memoryUsage);
        }
    }
}