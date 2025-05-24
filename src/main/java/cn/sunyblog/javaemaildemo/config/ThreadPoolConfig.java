package cn.sunyblog.javaemaildemo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Configuration
public class ThreadPoolConfig {
    @Value("16")
    private int corePoolSize;  // 核心线程数

    @Value("50")
    private int maximumPoolSize;  // 最大线程数

    @Value("60")
    private long keepAliveTime;  // 空闲线程存活时间

    @Value("100")
    private int queueCapacity;  // 阻塞队列容量

    @Bean
    public ExecutorService noticeThreadPool() {
        // 使用自定义的线程工厂
        ThreadFactory threadFactory = new ThreadFactory() {
            //volatile修饰的变量，保证线程安全
            private final AtomicInteger threadNumber = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "NoticeThreadPool-Thread-" + threadNumber.getAndIncrement());
            }
        };

        // 使用自定义的拒绝策略
        RejectedExecutionHandler rejectedExecutionHandler = (r, executor) -> {
            log.error("通知任务被拒绝，线程池已满");
        };

        // 创建线程池
        return new ThreadPoolExecutor(
                corePoolSize,  // 核心线程数
                maximumPoolSize,  // 最大线程数
                keepAliveTime,  // 空闲线程存活时间
                TimeUnit.SECONDS,  // 时间单位
                new ArrayBlockingQueue<>(queueCapacity),  // 阻塞队列
                threadFactory,  // 线程工厂
                rejectedExecutionHandler  // 拒绝策略
        );
    }
}