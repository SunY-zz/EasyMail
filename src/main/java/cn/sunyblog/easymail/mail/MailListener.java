package cn.sunyblog.easymail.mail;

import cn.sunyblog.easymail.config.MailConfig;
import cn.sunyblog.easymail.exception.EmailConnectionException;
import cn.sunyblog.easymail.exception.EmailException;
import cn.sunyblog.easymail.exception.EmailExceptionHandler;
import cn.sunyblog.easymail.exception.EmailProcessException;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.mail.*;
import javax.mail.event.MessageCountAdapter;
import javax.mail.event.MessageCountEvent;
import javax.mail.search.FlagTerm;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author suny
 * @version 1.0
 * @description: 邮件监听服务   负责监听新邮件并触发处理
 * @date 2025/05/12 16:23
 */
@Slf4j
@Data
@Component
public class MailListener {

    @Resource
    private MailConfig mailConfig;
    @Resource
    private MailProcessor mailProcessor;
    @Resource
    private ExecutorService noticeThreadPool;
    @Resource
    private MailCache mailCache;
    @Resource
    private MailServerConnector mailServerConnector;

    private Session session; // 邮箱会话
    private Store store; // 邮箱存储
    private Folder folder; // 邮箱文件夹
    private Thread monitorThread; // 监听线程
    private Thread keepAliveThread; // 保活线程
    private final AtomicBoolean isRunning = new AtomicBoolean(false); // 监听状态

    /**
     * 启动邮件监听
     *
     * @param serverConnector 服务器连接器
     */
    public void startListening(MailServerConnector serverConnector) {
        int retryCount = 0;
        int maxRetries = mailConfig.getListener().getMaxRetries(); // 最大重试次数
        long retryDelay = mailConfig.getMonitor().getShortDelay() * 1000L; // 重试延迟(毫秒)

        while (retryCount < maxRetries) {
            try {
                // 创建会话并连接服务器
                session = serverConnector.createSession();
                store = serverConnector.connectToServer(session);
                folder = serverConnector.openInbox(store);

                // 处理现有未读邮件
                log.debug("开始处理现有未读邮件");
                processUnreadEmails();

                // 设置新邮件监听器
                setupMessageListener();

                // 开始监听
                log.debug("开始监听新邮件");
                isRunning.set(true);

                // 启动监听线程
                startMonitorThread(serverConnector);

                // 启动保活线程
                startKeepAliveThread();

                log.info("邮件监听服务启动完成");
                return; // 成功启动，退出方法
            } catch (Exception e) {
                retryCount++;
                EmailException connectionEx = EmailExceptionHandler.wrapException(e,
                        "启动邮件监听失败(第" + retryCount + "次尝试，共" + maxRetries + "次)");
                log.error(connectionEx.getFullErrorMessage(), connectionEx);

                // 关闭可能部分打开的资源
                serverConnector.closeConnection(folder, store);

                if (retryCount < maxRetries) {
                    log.info("将在{}秒后重试连接邮件服务器", mailConfig.getMonitor().getShortDelay());
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("重试等待被中断");
                        break;
                    }
                }
            }
        }

        // 所有重试都失败后，启动一个后台线程定期尝试重连
        startRecoveryThread(serverConnector);
    }

    /**
     * 启动恢复线程，定期尝试重新连接邮件服务器
     */
    private void startRecoveryThread(MailServerConnector serverConnector) {
        Thread recoveryThread = new Thread(() -> {
            log.info("启动邮件服务恢复线程，将定期尝试重新连接");
            int recoveryAttempt = 0;
            long connectionTimeout = mailConfig.getConnection().getTimeout() * 1000L; // 连接超时时间(毫秒)
            log.info("连接超时设置：{}秒", connectionTimeout / 1000);
            int maxRecoveryAttempts = mailConfig.getMonitor().getReconnectDelay(); // 最大恢复尝试次数
            while (!isRunning.get() && !Thread.currentThread().isInterrupted() && recoveryAttempt < maxRecoveryAttempts) {
                recoveryAttempt++;
                log.info("尝试恢复邮件服务连接(第{}次恢复尝试)最大尝试连接次数：{}", recoveryAttempt, maxRecoveryAttempts);

                try {
                    // 创建会话并连接服务器
                    session = serverConnector.createSession();
                    store = serverConnector.connectToServer(session);
                    folder = serverConnector.openInbox(store);

                    // 处理现有未读邮件
                    log.debug("开始处理现有未读邮件");
                    processUnreadEmails();

                    // 设置新邮件监听器
                    setupMessageListener();

                    // 开始监听
                    log.debug("开始监听新邮件");
                    isRunning.set(true);

                    // 启动监听线程
                    startMonitorThread(serverConnector);

                    // 启动保活线程
                    startKeepAliveThread();

                    log.info("邮件监听服务恢复成功");
                    return; // 成功恢复，退出线程
                } catch (Exception e) {
                    EmailException connectionEx = EmailExceptionHandler.wrapException(e,
                            "恢复邮件服务连接失败");
                    log.error(connectionEx.getFullErrorMessage(), connectionEx);

                    // 关闭可能部分打开的资源
                    serverConnector.closeConnection(folder, store);

                    try {
                        log.info("将在{}秒后再次尝试恢复连接", mailConfig.getMonitor().getLongDelay());
                        Thread.sleep(connectionTimeout);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("恢复线程被中断");
                        break;
                    }
                }
            }
            log.error("邮件服务恢复失败，达到重试次数，将停止邮件监听服务");
        });

        recoveryThread.setDaemon(true);
        recoveryThread.setName("MailServiceRecoveryThread");
        recoveryThread.start();
    }

    /**
     * 设置邮件监听器
     */
    // 添加一个标志位
    private final AtomicBoolean processingEvent = new AtomicBoolean(false);

    private void setupMessageListener() {
        folder.addMessageCountListener(new MessageCountAdapter() {
            @Override
            public void messagesAdded(MessageCountEvent e) {
                processingEvent.set(true); // 设置标志位
                try {
                    Message[] messages = e.getMessages();
                    long eventStartTime = System.currentTimeMillis();
                    log.debug("收到{}封新邮件", messages.length);

                    // 使用线程池处理邮件，不阻塞JavaMail事件线程
                    for (Message message : messages) {
                        final Message finalMessage = message;
                        noticeThreadPool.execute(() -> mailProcessor.processMessage(finalMessage));
                    }

                    long eventEndTime = System.currentTimeMillis();
                    log.debug("邮件事件分发完成，总耗时: {}毫秒", eventEndTime - eventStartTime);
                } finally {
                    processingEvent.set(false); // 重置标志位
                }
            }
        });
    }

    /**
     * 处理未读邮件
     */
    private void processUnreadEmails() {
        try {
            long startTime = System.currentTimeMillis();

            // 查找未读邮件
            FlagTerm ft = new FlagTerm(new Flags(Flags.Flag.SEEN), false);
            Message[] messages = folder.search(ft);

            if (messages.length == 0) {
                log.info("没有未读邮件");
                return;
            }

            log.info("开始处理{}封未读邮件", messages.length);
            final int[] processedCount = {0};  // 使用数组作为封闭变量的引用
            final Object lock = new Object();  // 用于同步的锁

            // 创建CountDownLatch等待所有未读邮件处理完成
            CountDownLatch latch = new CountDownLatch(messages.length);

            for (Message message : messages) {
                final Message finalMessage = message;
                noticeThreadPool.execute(() -> {
                    try {
                        boolean processed = mailProcessor.processMessage(finalMessage);
                        if (processed) {
                            synchronized (lock) {
                                processedCount[0]++;
                            }
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // 等待所有邮件处理完成，最多等待60秒
            try {
                boolean completed = latch.await(60, TimeUnit.SECONDS);
                if (!completed) {
                    log.warn("批量邮件处理超时，部分邮件可能仍在处理中");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("等待邮件处理完成被中断");
            }

            long endTime = System.currentTimeMillis();
            log.info("批量处理{}封邮件完成，总耗时: {}毫秒，平均每封耗时: {}毫秒",
                    processedCount[0],
                    endTime - startTime,
                    processedCount[0] > 0 ? (endTime - startTime) / processedCount[0] : 0);
        } catch (Exception e) {
            EmailProcessException processEx = EmailProcessException.processingError("处理未读邮件失败", e);
            log.error(processEx.getFullErrorMessage(), processEx);
        }
    }

    /**
     * 启动监听线程
     *
     * @param serverConnector 服务器连接器
     */
    private void startMonitorThread(MailServerConnector serverConnector) {
        monitorThread = new Thread(() -> {
            log.info("邮件监听线程已启动");
            // 添加一个短暂延迟，避免与初始化时的事件检测冲突
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            while (isRunning.get()) {
                try {
                    if (folder instanceof IMAPFolder) {
                        IMAPFolder imapFolder = (IMAPFolder) folder;

                        // 确保文件夹是打开的
                        if (!imapFolder.isOpen()) {
                            log.info("文件夹已关闭，正在重新打开");
                            imapFolder.open(Folder.READ_WRITE);
                            // 重新添加监听器
                            setupMessageListener();
                        }

                        // 使用混合模式：先尝试IDLE，如果不支持或失败则使用轮询
                        // 检查服务器是否支持IDLE命令 (通过协议能力检查)
                        boolean supportsIdle = false;
                        try {
                            // 获取IMAP协议对象并检查能力
                            if (imapFolder.getStore() instanceof IMAPStore) {
                                IMAPStore imapStore = (IMAPStore) imapFolder.getStore();
                                supportsIdle = imapStore.hasCapability("IDLE");
                            }
                        } catch (Exception e) {
                            EmailException connectionEx = EmailExceptionHandler.wrapException(e,
                                    "检查IDLE能力时出错");
                            log.warn(connectionEx.getFullErrorMessage());
                        }

                        if (supportsIdle) {
                            // IDLE命令会阻塞，直到有新邮件或超时
                            log.debug("进入IDLE模式");
                            try {
                                // 修改：使用较短的超时时间
                                imapFolder.idle(false);
                            } catch (MessagingException e) {
                                EmailException connectionEx = EmailExceptionHandler.wrapMessagingException(e,
                                        "IDLE命令执行失败");
                                log.warn(connectionEx.getFullErrorMessage());
                            }
                            log.debug("退出IDLE模式");
                            // 在IDLE之后，主动检查一次新邮件
                            checkNewMessages();
                        } else {
                            // 如果不支持IDLE，使用轮询
                            //log.warn("当前邮件服务不支持IDLE模式，使用轮询方式");
                            try {
                                TimeUnit.SECONDS.sleep(mailConfig.getMonitor().getShortDelay());
                            } catch (InterruptedException e) {
                                // 恢复中断标志，以便上层逻辑可以感知到中断请求
                                Thread.currentThread().interrupt();
                                log.warn("邮件监听线程被中断，准备退出...");
                                return; // 或 break，视上下文决定是否终止循环
                            }
                            checkNewMessages();
                        }
                    } else {
                        log.warn("当前邮件服务不支持IDLE模式，使用轮询方式");
                        TimeUnit.SECONDS.sleep(mailConfig.getMonitor().getShortDelay());
                        checkNewMessages();
                    }
                } catch (FolderClosedException fce) {
                    log.warn("文件夹已关闭: {}", fce.getMessage());
                    try {
                        if (!folder.isOpen()) {
                            folder.open(Folder.READ_WRITE);
                            log.info("已重新打开文件夹");
                        }
                        TimeUnit.SECONDS.sleep(mailConfig.getMonitor().getShortDelay());
                    } catch (Exception reconnectEx) {
                        EmailException connectionEx = EmailExceptionHandler.wrapException(reconnectEx,
                                "重新打开文件夹失败");
                        log.error(connectionEx.getFullErrorMessage(), connectionEx);
                        try {
                            TimeUnit.SECONDS.sleep(mailConfig.getMonitor().getReconnectDelay());
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            log.warn("线程被中断");
                        }
                    }
                } catch (MessagingException me) {
                    EmailException connectionEx = EmailExceptionHandler.wrapMessagingException(me,
                            "邮件服务异常");
                    log.error(connectionEx.getFullErrorMessage(), connectionEx);
                    try {
                        // 尝试重新连接
                        serverConnector.reconnectIfNeeded(store, folder);
                        TimeUnit.SECONDS.sleep(mailConfig.getMonitor().getReconnectDelay());
                    } catch (Exception reconnectEx) {
                        EmailException reconnectConnectionEx = EmailExceptionHandler.wrapException(reconnectEx,
                                "重新连接失败");
                        log.error(reconnectConnectionEx.getFullErrorMessage(), reconnectConnectionEx);
                        try {
                            TimeUnit.SECONDS.sleep(mailConfig.getMonitor().getLongDelay());
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            log.warn("线程被中断");
                        }
                    }
                } catch (Exception e) {
                    EmailProcessException processEx = EmailProcessException.processingError("邮件监听异常", e);
                    log.error(processEx.getFullErrorMessage(), processEx);
                    try {
                        TimeUnit.SECONDS.sleep(mailConfig.getMonitor().getReconnectDelay());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("线程被中断");
                    }
                }
            }
            log.info("邮件监听线程已停止");
        });

        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    /**
     * 启动保活线程
     */
    private void startKeepAliveThread() {
        keepAliveThread = new Thread(() -> {
            log.info("保活线程已启动");
            while (isRunning.get()) {
                try {
                    if (store != null && store.isConnected() &&
                            folder != null && folder.isOpen()) {
                        // 发送NOOP命令保持连接活跃
                        folder.getMessageCount();
                        log.debug("发送保活信号");
                    } else {
                        log.warn("邮件连接已断开，等待主线程重连");
                    }
                    TimeUnit.SECONDS.sleep(mailConfig.getMonitor().getKeepAliveInterval());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    if (isRunning.get()) {
                        log.warn("保活线程被中断: {}", e.getMessage());
                    } else {
                        log.debug("邮件监听服务已停止，保活线程正常退出");
                    }
                    break;
                } catch (Exception e) {
                    if (isRunning.get()) {
                        EmailException connectionEx = EmailExceptionHandler.wrapException(e,
                                "保活线程异常");
                        log.warn(connectionEx.getFullErrorMessage());
                        try {
                            TimeUnit.SECONDS.sleep(mailConfig.getMonitor().getShortDelay());
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            log.debug("保活线程被中断，准备退出");
                            break;
                        }
                    } else {
                        log.debug("邮件监听服务已停止，保活线程退出");
                        break;
                    }
                }
            }
            log.info("保活线程已停止");
        });

        keepAliveThread.setDaemon(true);
        keepAliveThread.start();
    }

    /**
     * 检查新邮件（轮询方式）
     */
    private void checkNewMessages() {
        // 如果服务已停止运行，直接返回
        if (!isRunning.get()) {
            log.debug("邮件监听服务已停止，跳过邮件检查");
            return;
        }

        // 如果正在处理事件通知，则跳过本次轮询
        if (processingEvent.get()) {
            log.debug("正在处理邮件事件，跳过本次轮询");
            return;
        }

        try {
            log.debug("开始检查新邮件");

            // 再次检查运行状态，避免在打开文件夹过程中服务被停止
            if (!isRunning.get()) {
                log.debug("邮件监听服务已停止，终止邮件检查");
                return;
            }

            if (!folder.isOpen()) {
                folder.open(Folder.READ_WRITE);
                // 重新添加监听器
                setupMessageListener();
            }

            // 获取未读邮件
            FlagTerm ft = new FlagTerm(new Flags(Flags.Flag.SEEN), false);
            Message[] messages = folder.search(ft);

            if (messages.length > 0) {
                log.debug("轮询检测到{}封未读邮件", messages.length);

                for (Message message : messages) {
                    String messageId = mailCache.getMessageId(message);
                    if (!mailCache.shouldProcessMessage(messageId)) {
                        log.debug("邮件已处理，跳过轮询处理: {}", messageId);
                        continue;
                    }

                    noticeThreadPool.execute(() -> mailProcessor.processMessage(message));
                }
            } else {
                log.debug("轮询检查：没有新邮件");
            }
        } catch (IllegalStateException e) {
            // 文件夹已关闭的异常，在服务停止时是正常现象，不记录为错误
            if (!isRunning.get()) {
                log.debug("邮件监听服务已停止，文件夹操作被中断: {}", e.getMessage());
            } else {
                log.warn("文件夹状态异常: {}", e.getMessage());
            }
        } catch (Exception e) {
            // 只有在服务运行时才记录为错误
            if (isRunning.get()) {
                EmailProcessException processEx = EmailProcessException.processingError("轮询检查邮件异常", e);
                log.error(processEx.getFullErrorMessage(), processEx);
            } else {
                log.debug("邮件监听服务已停止，操作被中断: {}", e.getMessage());
            }
        }
    }

    /**
     * 停止邮件监听
     *
     * @param serverConnector 服务器连接器
     */
    public void stopListening(MailServerConnector serverConnector) {
        log.info("正在关闭邮件监听服务");

        isRunning.set(false);

        if (monitorThread != null) {
            monitorThread.interrupt();
        }

        if (keepAliveThread != null) {
            keepAliveThread.interrupt();
        }

        serverConnector.closeConnection(folder, store);

        log.info("邮件监听服务已成功关闭");
    }

    /**
     * 检查邮件监听器是否正在运行
     *
     * @return 是否正在运行
     */
    public boolean isRunning() {
        return isRunning.get();
    }
}