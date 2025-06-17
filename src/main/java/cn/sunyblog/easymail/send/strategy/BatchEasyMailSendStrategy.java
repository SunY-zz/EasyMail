package cn.sunyblog.easymail.send.strategy;

import cn.sunyblog.easymail.send.EasyMailSendResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 批量邮件发送策略
 * 专门处理大量邮件发送的场景，使用并行发送提高效率
 *
 * @author suny
 * @version 1.0.0
 */
@Slf4j
@Component
public class BatchEasyMailSendStrategy implements EasyMailSendStrategy {

    @Resource
    private DefaultEasyMailSendStrategy defaultStrategy;

    @Resource
    private ThreadPoolExecutor executor;

    /**
     * 批量发送的阈值，超过此数量使用批量策略
     */
    private static final int BATCH_THRESHOLD = 5;

    @Override
    public EasyMailSendResult send(List<String> toList, List<String> ccList, List<String> bccList,
                                   String subject, String content, boolean isHtml, List<File> attachments) {

        long startTime = System.currentTimeMillis();

        try {
            // 参数验证
            if (toList == null || toList.isEmpty()) {
                return EasyMailSendResult.failure(toList, subject, "收件人列表不能为空", 0);
            }

            // 如果收件人数量较少，使用默认策略
            if (toList.size() < BATCH_THRESHOLD) {
                return defaultStrategy.send(toList, ccList, bccList, subject, content, isHtml, attachments);
            }

            // 批量并行发送
            return sendBatch(toList, ccList, bccList, subject, content, isHtml, attachments, startTime);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("批量邮件发送异常: {}", e.getMessage(), e);
            return EasyMailSendResult.failure(toList, subject, e.getMessage(), "BATCH_SEND_ERROR", getStackTrace(e), duration);
        }
    }

    @Override
    public String getStrategyName() {
        return "BatchStrategy";
    }

    @Override
    public String getDescription() {
        return "批量邮件发送策略，适用于大量邮件发送场景，使用并行发送提高效率";
    }

    @Override
    public boolean supports(int recipientCount, boolean hasAttachments, boolean isHtml) {
        // 支持收件人数量较多的场景
        return recipientCount >= BATCH_THRESHOLD;
    }

    @Override
    public int getPriority() {
        // 批量策略优先级较高
        return 100;
    }

    /**
     * 批量发送邮件
     */
    private EasyMailSendResult sendBatch(List<String> toList, List<String> ccList, List<String> bccList,
                                         String subject, String content, boolean isHtml,
                                         List<File> attachments, long startTime) {

        ConcurrentHashMap<String, Boolean> batchResults = new ConcurrentHashMap<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        log.info("开始批量发送邮件，收件人数量: {}", toList.size());

        // 创建并行发送任务
        CompletableFuture<Void>[] futures = toList.stream()
                .map(to -> CompletableFuture.runAsync(() -> {
                    try {
                        // 为每个收件人单独发送
                        EasyMailSendResult result = defaultStrategy.send(
                                java.util.Arrays.asList(to),
                                ccList,
                                bccList,
                                subject,
                                content,
                                isHtml,
                                attachments
                        );

                        batchResults.put(to, result.isSuccess());

                        if (result.isSuccess()) {
                            successCount.incrementAndGet();
                            log.debug("邮件发送成功: {}", to);
                        } else {
                            failureCount.incrementAndGet();
                            log.warn("邮件发送失败: {}, 错误: {}", to, result.getErrorMessage());
                        }

                    } catch (Exception e) {
                        batchResults.put(to, false);
                        failureCount.incrementAndGet();
                        log.error("发送邮件给 {} 时发生异常: {}", to, e.getMessage(), e);
                    }
                }, executor))
                .toArray(CompletableFuture[]::new);

        // 等待所有发送任务完成
        try {
            CompletableFuture.allOf(futures).join();
        } catch (Exception e) {
            log.error("等待批量发送完成时发生异常: {}", e.getMessage(), e);
        }

        long duration = System.currentTimeMillis() - startTime;

        // 构建批量发送结果
        EasyMailSendResult result = EasyMailSendResult.batchResult(batchResults, subject, duration);
        result.setSuccessCount(successCount.get());
        result.setFailureCount(failureCount.get());
        result.setTotalCount(toList.size());

        log.info("批量邮件发送完成，成功: {}/{}, 耗时: {}ms",
                successCount.get(), toList.size(), duration);

        return result;
    }

    /**
     * 分批发送邮件（当收件人数量非常大时）
     *
     * @param toList      收件人列表
     * @param ccList      抄送人列表
     * @param bccList     密送人列表
     * @param subject     邮件主题
     * @param content     邮件内容
     * @param isHtml      是否为HTML格式
     * @param attachments 附件列表
     * @param batchSize   每批发送的数量
     * @return 发送结果
     */
    public EasyMailSendResult sendInBatches(List<String> toList, List<String> ccList, List<String> bccList,
                                            String subject, String content, boolean isHtml,
                                            List<File> attachments, int batchSize) {

        long startTime = System.currentTimeMillis();
        Map<String, Boolean> allResults = new HashMap<>();

        // 分批处理
        for (int i = 0; i < toList.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, toList.size());
            List<String> batch = toList.subList(i, endIndex);

            log.info("发送第 {} 批邮件，收件人: {}-{}/{}",
                    (i / batchSize + 1), i + 1, endIndex, toList.size());

            EasyMailSendResult batchResult = send(batch, ccList, bccList, subject, content, isHtml, attachments);

            if (batchResult.isBatchSend() && batchResult.getBatchResults() != null) {
                allResults.putAll(batchResult.getBatchResults());
            } else {
                // 单个发送结果
                for (String to : batch) {
                    allResults.put(to, batchResult.isSuccess());
                }
            }

            // 批次间稍作延迟，避免对邮件服务器造成过大压力
            if (i + batchSize < toList.size()) {
                try {
                    Thread.sleep(100); // 100ms延迟
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("批次间延迟被中断");
                }
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        return EasyMailSendResult.batchResult(allResults, subject, duration);
    }

    /**
     * 获取异常堆栈信息
     */
    private String getStackTrace(Exception e) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}