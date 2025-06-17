package cn.sunyblog.easymail.util;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

/**
 * 通用重试工具类
 * 提供可配置的重试机制，支持自定义重试次数、重试间隔、重试条件等
 */
@Slf4j
public class EasyMailRetryUtil {

    /**
     * 使用指定的重试策略执行操作
     *
     * @param operation   需要执行的操作
     * @param retryConfig 重试配置
     * @param <T>         返回值类型
     * @return 操作的结果
     * @throws Exception 如果所有重试都失败，则抛出最后一次执行的异常
     */
    public static <T> T executeWithRetry(Callable<T> operation, RetryConfig retryConfig) throws Exception {
        int attempts = 0;
        Exception lastException = null;

        while (attempts < retryConfig.getMaxAttempts()) {
            try {
                T result = operation.call();

                // 如果设置了结果验证器，则验证结果
                if (retryConfig.getResultValidator() != null && !retryConfig.getResultValidator().test(result)) {
                    log.warn("第{}次尝试结果验证失败，将进行重试", attempts + 1);
                    attempts++;
                    sleep(retryConfig.getDelayMs(attempts));
                    continue;
                }

                // 操作成功，返回结果
                if (attempts > 0) {
                    log.info("在第{}次尝试后操作成功", attempts + 1);
                }
                return result;
            } catch (Exception e) {
                lastException = e;
                attempts++;

                // 检查是否应该重试这个异常
                if (retryConfig.getRetryableExceptions() != null &&
                        !retryConfig.getRetryableExceptions().isEmpty() &&
                        !retryConfig.getRetryableExceptions().contains(e.getClass())) {
                    log.error("发生不可重试的异常: {}", e.getMessage(), e);
                    throw e;
                }

                if (attempts >= retryConfig.getMaxAttempts()) {
                    log.error("达到最大重试次数({}次)，操作失败", retryConfig.getMaxAttempts());
                    break;
                }

                long delayMs = retryConfig.getDelayMs(attempts);
                log.warn("第{}次尝试失败: {}，将在{}ms后重试", attempts, e.getMessage(), delayMs);
                sleep(delayMs);
            }
        }

        // 所有重试都失败了，抛出最后一个异常
        assert lastException != null;
        throw lastException;
    }

    /**
     * 使用指定的重试策略执行无返回值的操作
     *
     * @param operation   需要执行的操作
     * @param retryConfig 重试配置
     * @throws Exception 如果所有重试都失败，则抛出最后一次执行的异常
     */
    public static void executeWithRetry(Runnable operation, RetryConfig retryConfig) throws Exception {
        executeWithRetry(() -> {
            operation.run();
            return null;
        }, retryConfig);
    }

    /**
     * 线程休眠指定的毫秒数
     *
     * @param ms 休眠的毫秒数
     */
    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("重试等待被中断");
        }
    }

    /**
     * 重试配置类
     */
    public static class RetryConfig {
        private int maxAttempts = 3;                      // 最大尝试次数（包括第一次）
        private long initialDelayMs = 1000;              // 初始延迟（毫秒）
        private long maxDelayMs = 10000;                 // 最大延迟（毫秒）
        private boolean useExponentialBackoff = true;    // 是否使用指数退避策略
        private double backoffMultiplier = 2.0;          // 退避乘数
        private Predicate<Object> resultValidator;       // 结果验证器
        private java.util.Set<Class<? extends Exception>> retryableExceptions = new java.util.HashSet<>(); // 可重试的异常类型

        /**
         * 创建默认的重试配置
         *
         * @return 重试配置
         */
        public static RetryConfig defaults() {
            return new RetryConfig();
        }

        /**
         * 计算指定尝试次数的延迟时间
         *
         * @param attempt 当前尝试次数（从1开始）
         * @return 延迟时间（毫秒）
         */
        public long getDelayMs(int attempt) {
            if (!useExponentialBackoff) {
                return initialDelayMs;
            }

            // 使用指数退避策略计算延迟
            long delay = (long) (initialDelayMs * Math.pow(backoffMultiplier, attempt - 1));
            return Math.min(delay, maxDelayMs);
        }

        // Getters and Setters

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public RetryConfig maxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }

        public long getInitialDelayMs() {
            return initialDelayMs;
        }

        public RetryConfig initialDelayMs(long initialDelayMs) {
            this.initialDelayMs = initialDelayMs;
            return this;
        }

        public long getMaxDelayMs() {
            return maxDelayMs;
        }

        public RetryConfig maxDelayMs(long maxDelayMs) {
            this.maxDelayMs = maxDelayMs;
            return this;
        }

        public boolean isUseExponentialBackoff() {
            return useExponentialBackoff;
        }

        public RetryConfig useExponentialBackoff(boolean useExponentialBackoff) {
            this.useExponentialBackoff = useExponentialBackoff;
            return this;
        }

        public double getBackoffMultiplier() {
            return backoffMultiplier;
        }

        public RetryConfig backoffMultiplier(double backoffMultiplier) {
            this.backoffMultiplier = backoffMultiplier;
            return this;
        }

        public Predicate<Object> getResultValidator() {
            return resultValidator;
        }

        public RetryConfig resultValidator(Predicate<Object> resultValidator) {
            this.resultValidator = resultValidator;
            return this;
        }

        public java.util.Set<Class<? extends Exception>> getRetryableExceptions() {
            return retryableExceptions;
        }

        @SafeVarargs
        public final RetryConfig retryableExceptions(Class<? extends Exception>... exceptions) {
            this.retryableExceptions.addAll(Arrays.asList(exceptions));
            return this;
        }
    }
}