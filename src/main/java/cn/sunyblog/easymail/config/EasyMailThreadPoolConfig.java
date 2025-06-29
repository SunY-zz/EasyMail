package cn.sunyblog.easymail.config;

import cn.sunyblog.easymail.dynamicThreadPool.DynamicThreadPoolExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置类
 * 用于配置邮件通知服务的动态线程池
 *
 * @author suny
 */
@Configuration
@EnableConfigurationProperties(EasyMailDynamicThreadPoolConfig.class)
public class EasyMailThreadPoolConfig {

    @Autowired
    private EasyMailDynamicThreadPoolConfig dynamicThreadPoolConfig;

    /**
     * 创建邮件通知动态线程池
     * 当启用动态线程池时使用
     *
     * @return DynamicThreadPoolExecutor 动态线程池实例
     */
    @Bean(name = "noticeThreadPool")
    @Primary
    @ConditionalOnProperty(name = "mail.dynamic-thread-pool.enabled", havingValue = "true", matchIfMissing = true)
    public ThreadPoolExecutor noticeThreadPool() {
        return new DynamicThreadPoolExecutor(dynamicThreadPoolConfig.toDynamicThreadPoolConfig());
    }

    /**
     * 创建普通线程池（备用）
     * 当禁用动态线程池时使用
     *
     * @return ThreadPoolExecutor 普通线程池实例
     */
    @Bean(name = "fallbackThreadPool")
    @ConditionalOnProperty(name = "mail.dynamic-thread-pool.enabled", havingValue = "false")
    public ThreadPoolExecutor fallbackThreadPool() {
        return new java.util.concurrent.ThreadPoolExecutor(
                dynamicThreadPoolConfig.getBasic().getCorePoolSize(),
                dynamicThreadPoolConfig.getBasic().getMaximumPoolSize(),
                dynamicThreadPoolConfig.getBasic().getKeepAliveTime(),
                java.util.concurrent.TimeUnit.SECONDS,
                new java.util.concurrent.LinkedBlockingQueue<>(dynamicThreadPoolConfig.getBasic().getQueueCapacity()),
                new EasyMailThreadFactory("EasyMail-Notice-"),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /**
     * 自定义线程工厂
     */
    private static class EasyMailThreadFactory implements java.util.concurrent.ThreadFactory {
        private final java.util.concurrent.atomic.AtomicInteger threadNumber = new java.util.concurrent.atomic.AtomicInteger(1);
        private final String namePrefix;

        EasyMailThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }
}