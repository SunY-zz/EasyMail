package cn.sunyblog.easymail.dynamicThreadPool.queue;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 支持动态调整容量的阻塞队列
 * 基于LinkedBlockingQueue改造，支持运行时修改队列大小
 *
 * @author suny
 */
public class DynamicBlockingQueue<E> extends LinkedBlockingQueue<E> {

    private volatile int capacity;
    private final ReentrantLock capacityLock = new ReentrantLock();

    public DynamicBlockingQueue(int capacity) {
        super(capacity);
        this.capacity = capacity;
    }

    /**
     * 动态设置队列容量
     *
     * @param newCapacity 新的容量大小
     */
    public void setCapacity(int newCapacity) {
        if (newCapacity <= 0) {
            throw new IllegalArgumentException("队列容量必须大于0");
        }

        capacityLock.lock();
        try {
            int oldCapacity = this.capacity;
            this.capacity = newCapacity;

            // 如果新容量小于当前队列大小，需要处理多余的元素
            if (newCapacity < size()) {
                handleCapacityReduction(oldCapacity, newCapacity);
            }

        } finally {
            capacityLock.unlock();
        }
    }

    /**
     * 处理容量缩减时的多余元素
     */
    private void handleCapacityReduction(int oldCapacity, int newCapacity) {
        // 这里可以根据业务需求选择不同的策略：
        // 1. 丢弃多余元素
        // 2. 等待元素被消费
        // 3. 抛出异常

        // 当前策略：等待元素被自然消费，不强制移除
        System.out.println(String.format(
                "队列容量从 %d 缩减到 %d，当前队列大小: %d，等待自然消费",
                oldCapacity, newCapacity, size()));
    }

    /**
     * 获取当前容量
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * 获取队列使用率
     */
    public double getUsageRate() {
        return (double) size() / capacity;
    }

    @Override
    public boolean offer(E e) {
        // 检查是否超过动态容量限制
        if (size() >= capacity) {
            return false;
        }
        return super.offer(e);
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        // 对于带超时的offer，需要考虑动态容量
        long nanos = unit.toNanos(timeout);
        long deadline = System.nanoTime() + nanos;

        while (size() >= capacity) {
            if (nanos <= 0) {
                return false;
            }

            // 等待一小段时间后重试
            Thread.sleep(1);
            nanos = deadline - System.nanoTime();
        }

        return super.offer(e, nanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public void put(E e) throws InterruptedException {
        // 等待直到队列有空间
        while (size() >= capacity) {
            Thread.sleep(1);
        }
        super.put(e);
    }

    /**
     * 获取队列状态信息
     */
    public QueueStatus getStatus() {
        return new QueueStatus(
                size(),
                capacity,
                getUsageRate(),
                remainingCapacity()
        );
    }

    @Override
    public int remainingCapacity() {
        return Math.max(0, capacity - size());
    }

    /**
     * 队列状态信息
     */
    public static class QueueStatus {
        private final int currentSize;
        private final int capacity;
        private final double usageRate;
        private final int remainingCapacity;

        public QueueStatus(int currentSize, int capacity, double usageRate, int remainingCapacity) {
            this.currentSize = currentSize;
            this.capacity = capacity;
            this.usageRate = usageRate;
            this.remainingCapacity = remainingCapacity;
        }

        public int getCurrentSize() {
            return currentSize;
        }

        public int getCapacity() {
            return capacity;
        }

        public double getUsageRate() {
            return usageRate;
        }

        public int getRemainingCapacity() {
            return remainingCapacity;
        }

        @Override
        public String toString() {
            return String.format(
                    "QueueStatus{size=%d, capacity=%d, usageRate=%.2f%%, remaining=%d}",
                    currentSize, capacity, usageRate * 100, remainingCapacity
            );
        }
    }
}