//package cn.sunyblog.easymail.example;
//
//import cn.sunyblog.easymail.api.EasyMailRequest;
//import cn.sunyblog.easymail.api.EasyMailSenderService;
//import cn.sunyblog.easymail.send.EasyMailSendResult;
//import cn.sunyblog.easymail.send.schedule.EasyMailScheduledTask;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//
//import javax.annotation.Resource;
//import java.time.LocalDateTime;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Map;
//
///**
// * 邮件定时发送功能使用示例
// * 展示如何使用EasyMail的定时发送功能
// *
// * @author suny
// * @version 1.0.0
// */
//@Slf4j
//@Component
//public class EasyMailScheduleExample {
//
//    @Resource
//    private EasyMailSenderService easyMailSenderService;
//
//    /**
//     * 使用Cron表达式定时发送邮件示例
//     */
//    public void cronScheduleExample() {
//        log.info("=== Cron表达式定时发送邮件示例 ===");
//
//        // 创建邮件请求
//        EasyMailRequest request = EasyMailRequest.builder()
//                .to("recipient@example.com")
//                .subject("定时邮件 - Cron表达式")
//                .text("这是一封使用Cron表达式定时发送的邮件，每天上午9点发送")
//                .build();
//
//        // 每天上午9点发送邮件
//        String taskId = easyMailSenderService.sendScheduled(request, "0 0 9 * * ?", "每日报告邮件");
//        log.info("已创建Cron定时任务，任务ID: {}", taskId);
//    }
//
//    /**
//     * 延迟发送邮件示例
//     */
//    public void delayedSendExample() {
//        log.info("=== 延迟发送邮件示例 ===");
//
//        // 创建邮件请求
//        EasyMailRequest request = EasyMailRequest.builder()
//                .to("recipient@example.com")
//                .subject("延迟邮件")
//                .html("<h1>这是一封延迟5分钟发送的邮件</h1><p>发送时间: " + LocalDateTime.now() + "</p>")
//                .build();
//
//        // 延迟5分钟发送
//        long delayMillis = 5 * 60 * 1000; // 5分钟
//        String taskId = easyMailSenderService.sendDelayed(request, delayMillis, "延迟通知邮件");
//        log.info("已创建延迟任务，任务ID: {}，将在{}毫秒后发送", taskId, delayMillis);
//    }
//
//    /**
//     * 固定频率发送邮件示例
//     */
//    public void fixedRateExample() {
//        log.info("=== 固定频率发送邮件示例 ===");
//
//        // 创建邮件请求
//        EasyMailRequest request = EasyMailRequest.builder()
//                .to("recipient@example.com")
//                .subject("定期邮件")
//                .text("这是一封每30秒发送一次的定期邮件")
//                .build();
//
//        // 每30秒发送一次
//        long fixedRateMillis = 30 * 1000; // 30秒
//        String taskId = easyMailSenderService.sendAtFixedRate(request, fixedRateMillis, "定期监控邮件");
//        log.info("已创建固定频率任务，任务ID: {}，每{}毫秒发送一次", taskId, fixedRateMillis);
//    }
//
//    /**
//     * 固定延迟发送邮件示例
//     */
//    public void fixedDelayExample() {
//        log.info("=== 固定延迟发送邮件示例 ===");
//
//        // 创建邮件请求
//        EasyMailRequest request = EasyMailRequest.builder()
//                .to("recipient@example.com")
//                .subject("固定延迟邮件")
//                .text("这是一封上次发送完成后延迟1分钟再发送的邮件")
//                .build();
//
//        // 上次发送完成后延迟1分钟再发送
//        long fixedDelayMillis = 60 * 1000; // 1分钟
//        String taskId = easyMailSenderService.sendWithFixedDelay(request, fixedDelayMillis, "固定延迟邮件");
//        log.info("已创建固定延迟任务，任务ID: {}，每次发送完成后延迟{}毫秒", taskId, fixedDelayMillis);
//    }
//
//    /**
//     * 指定时间发送邮件示例
//     */
//    public void atTimeExample() {
//        log.info("=== 指定时间发送邮件示例 ===");
//
//        // 创建邮件请求
//        EasyMailRequest request = EasyMailRequest.builder()
//                .to("recipient@example.com")
//                .subject("定时邮件")
//                .html("<h2>这是一封在指定时间发送的邮件</h2><p>预定发送时间: " + LocalDateTime.now().plusMinutes(2) + "</p>")
//                .build();
//
//        // 2分钟后发送
//        LocalDateTime executeTime = LocalDateTime.now().plusMinutes(2);
//        String taskId = easyMailSenderService.sendAtTime(request, executeTime, "指定时间邮件");
//        log.info("已创建定时任务，任务ID: {}，将在{}发送", taskId, executeTime);
//    }
//
//    /**
//     * 任务管理示例
//     */
//    public void taskManagementExample() {
//        log.info("=== 任务管理示例 ===");
//
//        // 获取所有定时任务
//        List<EasyMailScheduledTask> allTasks = easyMailSenderService.getAllScheduledTasks();
//        log.info("当前共有{}个定时任务", allTasks.size());
//
//        // 获取运行中的任务
//        List<EasyMailScheduledTask> runningTasks = easyMailSenderService.getRunningScheduledTasks();
//        log.info("当前有{}个任务正在运行", runningTasks.size());
//
//        // 获取任务统计信息
//        Map<String, Object> statistics = easyMailSenderService.getScheduledTaskStatistics();
//        log.info("任务统计信息: {}", statistics);
//
//        // 获取待执行的任务
//        List<EasyMailScheduledTask> pendingTasks = easyMailSenderService
//                .getScheduledTasksByStatus(EasyMailScheduledTask.TaskStatus.PENDING);
//        log.info("待执行任务数量: {}", pendingTasks.size());
//    }
//
//    /**
//     * 取消任务示例
//     */
//    public void cancelTaskExample() {
//        log.info("=== 取消任务示例 ===");
//
//        // 创建一个测试任务
//        EasyMailRequest request = EasyMailRequest.builder()
//                .to("recipient@example.com")
//                .subject("测试任务")
//                .text("这是一个用于测试取消功能的任务")
//                .build();
//
//        // 创建延迟任务
//        String taskId = easyMailSenderService.sendDelayed(request, 60000, "测试取消任务");
//        log.info("已创建测试任务，任务ID: {}", taskId);
//
//        // 等待一段时间后取消任务
//        try {
//            Thread.sleep(2000); // 等待2秒
//            boolean cancelled = easyMailSenderService.cancelScheduledTask(taskId);
//            log.info("任务取消结果: {}", cancelled ? "成功" : "失败");
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//            log.error("线程被中断", e);
//        }
//    }
//
//    /**
//     * 清理已完成任务示例
//     */
//    public void cleanupExample() {
//        log.info("=== 清理已完成任务示例 ===");
//
//        // 获取清理前的任务数量
//        int beforeCount = easyMailSenderService.getScheduledTaskCount();
//        log.info("清理前任务数量: {}", beforeCount);
//
//        // 清理已完成的任务
//        easyMailSenderService.cleanupCompletedScheduledTasks();
//
//        // 获取清理后的任务数量
//        int afterCount = easyMailSenderService.getScheduledTaskCount();
//        log.info("清理后任务数量: {}", afterCount);
//        log.info("已清理{}个已完成的任务", beforeCount - afterCount);
//    }
//
//    /**
//     * 复杂邮件定时发送示例
//     */
//    public void complexEmailExample() {
//        log.info("=== 复杂邮件定时发送示例 ===");
//
//        // 创建包含附件的复杂邮件
//        EasyMailRequest request = EasyMailRequest.builder()
//                .to("recipient@example.com")
//                .cc("cc@example.com")
//                .bcc("bcc@example.com")
//                .subject("复杂邮件定时发送")
//                .html("<h1>复杂邮件内容</h1><p>这是一封包含多个收件人的复杂邮件</p>")
//                .priority(1) // 1为最高优先级
//                .build();
//
//        // 使用Cron表达式每周一上午10点发送
//        String taskId = easyMailSenderService.sendScheduled(request, "0 0 10 ? * MON", "周报邮件");
//        log.info("已创建复杂邮件定时任务，任务ID: {}", taskId);
//    }
//
//    /**
//     * 批量邮件定时发送示例
//     */
//    public void batchEmailExample() {
//        log.info("=== 批量邮件定时发送示例 ===");
//
//        // 准备批量邮件请求
//        List<EasyMailRequest> batchRequests = Arrays.asList(
//                EasyMailRequest.builder()
//                        .to("user1@example.com")
//                        .subject("批量通知1")
//                        .text("这是第一封批量邮件")
//                        .build(),
//                EasyMailRequest.builder()
//                        .to("user2@example.com")
//                        .subject("批量通知2")
//                        .html("这是第二封批量邮件")
//                        .build(),
//                EasyMailRequest.builder()
//                        .to("user3@example.com")
//                        .subject("批量通知3")
//                        .text("这是第三封批量邮件")
//                        .priority(1)
//                        .build()
//        );
//
//        // 批量定时发送（每周一上午10点）
//        String batchCronTaskId = easyMailSenderService.sendBatchScheduled(batchRequests, "0 0 10 ? * MON");
//        log.info("批量Cron定时任务ID: {}", batchCronTaskId);
//
//        // 批量延迟发送（30分钟后）
//        String batchDelayTaskId = easyMailSenderService.sendBatchDelayed(batchRequests, 30 * 60 * 1000L);
//        log.info("批量延迟任务ID: {}", batchDelayTaskId);
//
//        // 批量指定时间发送（明天下午2点）
//        LocalDateTime tomorrow2PM = LocalDateTime.now().plusDays(1).withHour(14).withMinute(0).withSecond(0);
//        String batchAtTimeTaskId = easyMailSenderService.sendBatchAtTime(batchRequests, tomorrow2PM);
//        log.info("批量指定时间任务ID: {}", batchAtTimeTaskId);
//
//        // 立即批量发送（非定时）
//        List<EasyMailSendResult> batchResults = easyMailSenderService.sendBatch(batchRequests);
//        int successCount = (int) batchResults.stream().mapToInt(r -> r.isSuccess() ? 1 : 0).sum();
//        log.info("批量发送完成，成功: {}/{}", successCount, batchResults.size());
//    }
//
//    /**
//     * 运行所有示例
//     */
//    public void runAllExamples() {
//        log.info("开始运行所有定时发送示例...");
//
//        try {
//            cronScheduleExample();
//            Thread.sleep(1000);
//
//            delayedSendExample();
//            Thread.sleep(1000);
//
//            fixedRateExample();
//            Thread.sleep(1000);
//
//            fixedDelayExample();
//            Thread.sleep(1000);
//
//            atTimeExample();
//            Thread.sleep(1000);
//
//            taskManagementExample();
//            Thread.sleep(1000);
//
//            cancelTaskExample();
//            Thread.sleep(1000);
//
//            complexEmailExample();
//            Thread.sleep(1000);
//
//            batchEmailExample();
//            Thread.sleep(1000);
//
//            cleanupExample();
//
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//            log.error("Example execution interrupted", e);
//        }
//
//        log.info("All scheduled email examples completed!");
//    }
//}