package cn.sunyblog.easymail.send.strategy;

import cn.sunyblog.easymail.send.EasyMailSendResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 邮件发送策略管理器
 * 根据不同的发送场景自动选择最合适的发送策略
 *
 * @author suny
 * @version 1.0.0
 */
@Slf4j
@Component
public class EasyMailSendStrategyManager {

    @Resource
    private DefaultEasyMailSendStrategy defaultStrategy;

    @Resource
    private BatchEasyMailSendStrategy batchStrategy;


    /**
     * 策略注册表，按优先级排序
     */
    private final List<EasyMailSendStrategy> strategies = new ArrayList<>();

    /**
     * 策略使用统计
     */
    private final Map<String, Long> strategyUsageStats = new ConcurrentHashMap<>();

    /**
     * 策略性能统计
     */
    private final Map<String, StrategyPerformance> strategyPerformanceStats = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // 注册所有策略并按优先级排序
        registerStrategy(defaultStrategy);
        registerStrategy(batchStrategy);

        // 按优先级降序排序
        strategies.sort((s1, s2) -> Integer.compare(s2.getPriority(), s1.getPriority()));

        log.info("邮件发送策略管理器初始化完成，已注册 {} 个策略", strategies.size());
        strategies.forEach(strategy ->
                log.info("策略: {} (优先级: {}) - {}",
                        strategy.getStrategyName(), strategy.getPriority(), strategy.getDescription()));
    }

    /**
     * 注册策略
     */
    public void registerStrategy(EasyMailSendStrategy strategy) {
        if (strategy != null && !strategies.contains(strategy)) {
            strategies.add(strategy);
            strategyUsageStats.put(strategy.getStrategyName(), 0L);
            strategyPerformanceStats.put(strategy.getStrategyName(), new StrategyPerformance());
            log.debug("注册邮件发送策略: {}", strategy.getStrategyName());
        }
    }

    /**
     * 移除策略
     */
    public void unregisterStrategy(String strategyName) {
        strategies.removeIf(strategy -> strategy.getStrategyName().equals(strategyName));
        strategyUsageStats.remove(strategyName);
        strategyPerformanceStats.remove(strategyName);
        log.debug("移除邮件发送策略: {}", strategyName);
    }

    /**
     * 智能选择发送策略并发送邮件
     */
    public EasyMailSendResult sendEmail(List<String> toList, List<String> ccList, List<String> bccList,
                                        String subject, String content, boolean isHtml, List<File> attachments) {

        EasyMailSendStrategy selectedStrategy = selectStrategy(toList, ccList, bccList, subject, content, isHtml, attachments);

        log.info("选择策略: {} 发送邮件", selectedStrategy.getStrategyName());

        long startTime = System.currentTimeMillis();
        EasyMailSendResult result = selectedStrategy.send(toList, ccList, bccList, subject, content, isHtml, attachments);
        long duration = System.currentTimeMillis() - startTime;

        // 更新统计信息
        updateStatistics(selectedStrategy.getStrategyName(), result, duration);

        return result;
    }

    /**
     * 使用指定策略发送邮件
     */
    public EasyMailSendResult sendEmailWithStrategy(String strategyName, List<String> toList, List<String> ccList,
                                                    List<String> bccList, String subject, String content,
                                                    boolean isHtml, List<File> attachments) {

        EasyMailSendStrategy strategy = findStrategyByName(strategyName);
        if (strategy == null) {
            log.warn("未找到指定策略: {}，使用默认策略", strategyName);
            strategy = defaultStrategy;
        }

        log.info("使用指定策略: {} 发送邮件", strategy.getStrategyName());

        long startTime = System.currentTimeMillis();
        EasyMailSendResult result = strategy.send(toList, ccList, bccList, subject, content, isHtml, attachments);
        long duration = System.currentTimeMillis() - startTime;

        // 更新统计信息
        updateStatistics(strategy.getStrategyName(), result, duration);

        return result;
    }

    /**
     * 智能选择发送策略
     */
    private EasyMailSendStrategy selectStrategy(List<String> toList, List<String> ccList, List<String> bccList,
                                                String subject, String content, boolean isHtml, List<File> attachments) {

        int totalRecipients = getTotalRecipientCount(toList, ccList, bccList);
        boolean hasAttachments = attachments != null && !attachments.isEmpty();

        // 按优先级查找支持当前场景的策略
        for (EasyMailSendStrategy strategy : strategies) {
            if (strategy.supports(totalRecipients, hasAttachments, isHtml)) {
                log.debug("策略 {} 支持当前场景 (收件人: {}, 附件: {}, HTML: {})",
                        strategy.getStrategyName(), totalRecipients, hasAttachments, isHtml);
                return strategy;
            }
        }

        // 如果没有找到合适的策略，使用默认策略
        log.debug("未找到合适的策略，使用默认策略");
        return defaultStrategy;
    }

    /**
     * 根据名称查找策略
     */
    private EasyMailSendStrategy findStrategyByName(String strategyName) {
        return strategies.stream()
                .filter(strategy -> strategy.getStrategyName().equals(strategyName))
                .findFirst()
                .orElse(null);
    }

    /**
     * 计算总收件人数量
     */
    private int getTotalRecipientCount(List<String> toList, List<String> ccList, List<String> bccList) {
        int count = 0;
        if (toList != null) count += toList.size();
        if (ccList != null) count += ccList.size();
        if (bccList != null) count += bccList.size();
        return count;
    }

    /**
     * 更新策略使用统计
     */
    private void updateStatistics(String strategyName, EasyMailSendResult result, long duration) {
        // 更新使用次数
        strategyUsageStats.merge(strategyName, 1L, Long::sum);

        // 更新性能统计
        StrategyPerformance performance = strategyPerformanceStats.get(strategyName);
        if (performance != null) {
            performance.addExecution(result.isSuccess(), duration);
        }
    }

    /**
     * 获取策略使用统计
     */
    public Map<String, Long> getStrategyUsageStats() {
        return new HashMap<>(strategyUsageStats);
    }

    /**
     * 获取策略性能统计
     */
    public Map<String, StrategyPerformance> getStrategyPerformanceStats() {
        return new HashMap<>(strategyPerformanceStats);
    }

    /**
     * 获取所有已注册的策略
     */
    public List<EasyMailSendStrategy> getAllStrategies() {
        return new ArrayList<>(strategies);
    }

    /**
     * 获取策略详细信息
     */
    public Map<String, Object> getStrategyInfo() {
        Map<String, Object> info = new HashMap<>();

        List<Map<String, Object>> strategyList = new ArrayList<>();
        for (EasyMailSendStrategy strategy : strategies) {
            Map<String, Object> strategyInfo = new HashMap<>();
            strategyInfo.put("name", strategy.getStrategyName());
            strategyInfo.put("description", strategy.getDescription());
            strategyInfo.put("priority", strategy.getPriority());
            strategyInfo.put("usageCount", strategyUsageStats.getOrDefault(strategy.getStrategyName(), 0L));

            StrategyPerformance performance = strategyPerformanceStats.get(strategy.getStrategyName());
            if (performance != null) {
                strategyInfo.put("performance", performance.getStatistics());
            }

            strategyList.add(strategyInfo);
        }

        info.put("strategies", strategyList);
        info.put("totalStrategies", strategies.size());

        return info;
    }

    /**
     * 重置统计信息
     */
    public void resetStatistics() {
        strategyUsageStats.clear();
        strategyPerformanceStats.clear();

        // 重新初始化
        for (EasyMailSendStrategy strategy : strategies) {
            strategyUsageStats.put(strategy.getStrategyName(), 0L);
            strategyPerformanceStats.put(strategy.getStrategyName(), new StrategyPerformance());
        }

        log.info("策略统计信息已重置");
    }

    /**
     * 策略性能统计内部类
     */
    public static class StrategyPerformance {
        private long totalExecutions = 0;
        private long successfulExecutions = 0;
        private long totalDuration = 0;
        private long minDuration = Long.MAX_VALUE;
        private long maxDuration = 0;

        public synchronized void addExecution(boolean success, long duration) {
            totalExecutions++;
            if (success) {
                successfulExecutions++;
            }

            totalDuration += duration;
            minDuration = Math.min(minDuration, duration);
            maxDuration = Math.max(maxDuration, duration);
        }

        public synchronized Map<String, Object> getStatistics() {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalExecutions", totalExecutions);
            stats.put("successfulExecutions", successfulExecutions);
            stats.put("successRate", totalExecutions > 0 ? (double) successfulExecutions / totalExecutions : 0.0);
            stats.put("averageDuration", totalExecutions > 0 ? (double) totalDuration / totalExecutions : 0.0);
            stats.put("minDuration", minDuration == Long.MAX_VALUE ? 0 : minDuration);
            stats.put("maxDuration", maxDuration);
            return stats;
        }

        // Getters
        public long getTotalExecutions() {
            return totalExecutions;
        }

        public long getSuccessfulExecutions() {
            return successfulExecutions;
        }

        public double getSuccessRate() {
            return totalExecutions > 0 ? (double) successfulExecutions / totalExecutions : 0.0;
        }

        public double getAverageDuration() {
            return totalExecutions > 0 ? (double) totalDuration / totalExecutions : 0.0;
        }

        public long getMinDuration() {
            return minDuration == Long.MAX_VALUE ? 0 : minDuration;
        }

        public long getMaxDuration() {
            return maxDuration;
        }
    }
}