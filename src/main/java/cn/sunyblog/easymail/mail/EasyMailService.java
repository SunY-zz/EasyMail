package cn.sunyblog.easymail.mail;

import cn.sunyblog.easymail.exception.EasyMailException;
import cn.sunyblog.easymail.exception.EasyMailExceptionHandler;
import cn.sunyblog.easymail.send.EasyMailSender;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import cn.sunyblog.easymail.exception.EasyMailProcessException;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author suny
 * @version 1.0
 * @description: 邮件服务
 * @date 2025/05/12 16:22
 */
@Slf4j
@Data
@Service
public class EasyMailService {

    @Resource
    private EasyMailServerConnector easyMailServerConnector;
    @Resource
    private EasyMailListener easyMailListener;
    @Resource
    private EasyMailProcessor easyMailProcessor;
    @Resource
    private EasyMailSender easyMailSender;
    private boolean autoStart = false;

    // 添加状态标志，保证线程安全
    private final AtomicBoolean isMailServiceRunning = new AtomicBoolean(false);

    /**
     * 服务初始化，调用前请先配置正确的邮件配置
     */
    //@PostConstruct
    public void init() {
        log.info("邮件监听服务初始化开始");
        if (autoStart) {
            startMailMonitoring();
        } else {
            log.info("邮件监听器自动启动已禁用，需要手动调用startMailMonitoring()方法启动");
        }
    }

    /**
     * 服务销毁
     */
    @PreDestroy
    public void destroy() {
        stopMailMonitoring();
    }

    /**
     * 启动邮件监控
     *
     * @return 是否成功启动（如果已经在运行则返回false）
     */
    public boolean startMailMonitoring() {
        // 检查服务是否已经在运行
        if (isMailServiceRunning.get()) {
            log.info("邮件监听服务已经在运行中，无需重复启动");
            return false;
        }

        try {
            // 尝试设置状态为运行中，如果有其他线程同时调用，只有一个会成功
            if (isMailServiceRunning.compareAndSet(false, true)) {
                log.info("开始启动邮件监听服务");
                easyMailListener.startListening(easyMailServerConnector);

                // 检查是否真正启动成功
                if (easyMailListener.isRunning()) {
                    log.info("邮件监听服务启动成功");
                    return true;
                } else {
                    // 如果监听器没有成功运行，重置服务状态
                    log.warn("邮件监听服务未能成功启动，但已启动恢复线程");
                    isMailServiceRunning.set(false);
                    return false;
                }
            } else {
                log.info("另一个线程已经启动了邮件服务，本次启动取消");
                return false;
            }
        } catch (Exception e) {
            // 启动失败，重置状态
            isMailServiceRunning.set(false);
            EasyMailException connectionEx = EasyMailExceptionHandler.wrapException(e, "启动邮件监听失败");
            log.error(connectionEx.getFullErrorMessage(), connectionEx);
            return false;
        }
    }

    /**
     * 停止邮件监控
     */
    public void stopMailMonitoring() {
        // 只有在服务运行时才需要停止
        if (isMailServiceRunning.get()) {
            try {
                log.info("正在关闭邮件监听服务");
                easyMailListener.stopListening(easyMailServerConnector);
                // 设置状态为未运行
                isMailServiceRunning.set(false);
                log.info("邮件监听服务已关闭");
            } catch (Exception e) {
                EasyMailProcessException processEx = EasyMailProcessException.processingError("停止邮件监听服务失败", e);
                log.error(processEx.getFullErrorMessage(), processEx);
            }
        } else {
            log.info("邮件监听服务未运行，无需关闭");
        }
    }

    /**
     * 检查邮件服务是否正在运行
     *
     * @return 是否正在运行
     */
    public boolean isMailServiceRunning() {
        return isMailServiceRunning.get();
    }

    /**
     * 获取邮件处理统计
     *
     * @return 处理统计信息
     */
    public String getMailProcessingStats() {
        return easyMailProcessor.getProcessingStats();
    }
}