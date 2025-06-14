package cn.sunyblog.javaemaildemo.mail;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 邮件发送结果类
 * 提供详细的发送状态和错误信息
 * 
 * @author JavaEmailSpringBoot
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendResult {
    
    /**
     * 发送是否成功
     */
    private boolean success;
    
    /**
     * 发送的邮件ID
     */
    private String messageId;
    
    /**
     * 发送开始时间
     */
    private long startTime;
    
    /**
     * 发送结束时间
     */
    private long endTime;
    
    /**
     * 发送耗时（毫秒）
     */
    private long duration;
    
    /**
     * 收件人列表
     */
    private List<String> recipients;
    
    /**
     * 抄送人列表
     */
    private List<String> ccRecipients;
    
    /**
     * 密送人列表
     */
    private List<String> bccRecipients;
    
    /**
     * 邮件主题
     */
    private String subject;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 错误代码
     */
    private String errorCode;
    
    /**
     * 异常堆栈信息
     */
    private String stackTrace;
    
    /**
     * 重试次数
     */
    private int retryCount;
    
    /**
     * 是否使用了重试机制
     */
    private boolean retried;
    
    /**
     * 批量发送结果（当批量发送时使用）
     */
    private Map<String, Boolean> batchResults;
    
    /**
     * 成功发送的数量（批量发送时）
     */
    private int successCount;
    
    /**
     * 失败发送的数量（批量发送时）
     */
    private int failureCount;
    
    /**
     * 总发送数量（批量发送时）
     */
    private int totalCount;
    
    /**
     * 附加信息
     */
    private Map<String, Object> additionalInfo;
    
    /**
     * 创建成功的发送结果
     * 
     * @param messageId 邮件ID
     * @param recipients 收件人列表
     * @param subject 邮件主题
     * @param duration 发送耗时
     * @return 成功的发送结果
     */
    public static SendResult success(String messageId, List<String> recipients, String subject, long duration) {
        return SendResult.builder()
                .success(true)
                .messageId(messageId)
                .recipients(recipients)
                .subject(subject)
                .duration(duration)
                .endTime(System.currentTimeMillis())
                .startTime(System.currentTimeMillis() - duration)
                .build();
    }
    
    /**
     * 创建失败的发送结果
     * 
     * @param recipients 收件人列表
     * @param subject 邮件主题
     * @param errorMessage 错误信息
     * @param duration 发送耗时
     * @return 失败的发送结果
     */
    public static SendResult failure(List<String> recipients, String subject, String errorMessage, long duration) {
        return SendResult.builder()
                .success(false)
                .recipients(recipients)
                .subject(subject)
                .errorMessage(errorMessage)
                .duration(duration)
                .endTime(System.currentTimeMillis())
                .startTime(System.currentTimeMillis() - duration)
                .build();
    }
    
    /**
     * 创建失败的发送结果（带异常信息）
     * 
     * @param recipients 收件人列表
     * @param subject 邮件主题
     * @param errorMessage 错误信息
     * @param errorCode 错误代码
     * @param stackTrace 异常堆栈
     * @param duration 发送耗时
     * @return 失败的发送结果
     */
    public static SendResult failure(List<String> recipients, String subject, String errorMessage, 
                                   String errorCode, String stackTrace, long duration) {
        return SendResult.builder()
                .success(false)
                .recipients(recipients)
                .subject(subject)
                .errorMessage(errorMessage)
                .errorCode(errorCode)
                .stackTrace(stackTrace)
                .duration(duration)
                .endTime(System.currentTimeMillis())
                .startTime(System.currentTimeMillis() - duration)
                .build();
    }
    
    /**
     * 创建批量发送结果
     * 
     * @param batchResults 批量发送结果映射
     * @param subject 邮件主题
     * @param duration 发送耗时
     * @return 批量发送结果
     */
    public static SendResult batchResult(Map<String, Boolean> batchResults, String subject, long duration) {
        int successCount = (int) batchResults.values().stream().mapToLong(success -> success ? 1 : 0).sum();
        int totalCount = batchResults.size();
        int failureCount = totalCount - successCount;
        
        return SendResult.builder()
                .success(successCount > 0)
                .batchResults(batchResults)
                .subject(subject)
                .successCount(successCount)
                .failureCount(failureCount)
                .totalCount(totalCount)
                .duration(duration)
                .endTime(System.currentTimeMillis())
                .startTime(System.currentTimeMillis() - duration)
                .build();
    }
    
    /**
     * 获取发送成功率（百分比）
     * 
     * @return 成功率
     */
    public double getSuccessRate() {
        if (totalCount == 0) {
            return success ? 100.0 : 0.0;
        }
        return (double) successCount / totalCount * 100.0;
    }
    
    /**
     * 是否为批量发送
     * 
     * @return 是否为批量发送
     */
    public boolean isBatchSend() {
        return batchResults != null && !batchResults.isEmpty();
    }
    
    /**
     * 获取总收件人数量
     * 
     * @return 总收件人数量
     */
    public int getTotalRecipientCount() {
        int count = 0;
        if (recipients != null) count += recipients.size();
        if (ccRecipients != null) count += ccRecipients.size();
        if (bccRecipients != null) count += bccRecipients.size();
        return count;
    }
    
    /**
     * 获取详细的结果描述
     * 
     * @return 结果描述
     */
    public String getDetailedDescription() {
        StringBuilder sb = new StringBuilder();
        
        if (isBatchSend()) {
            sb.append(String.format("批量发送结果: 成功 %d/%d (%.1f%%), 耗时 %dms", 
                    successCount, totalCount, getSuccessRate(), duration));
            if (failureCount > 0) {
                sb.append(String.format(", 失败 %d 个", failureCount));
            }
        } else {
            sb.append(String.format("发送%s, 耗时 %dms", success ? "成功" : "失败", duration));
            if (!success && errorMessage != null) {
                sb.append(String.format(", 错误: %s", errorMessage));
            }
        }
        
        if (retried) {
            sb.append(String.format(", 重试 %d 次", retryCount));
        }
        
        return sb.toString();
    }
    
    /**
     * 转换为简单的字符串表示
     * 
     * @return 字符串表示
     */
    @Override
    public String toString() {
        return getDetailedDescription();
    }
}