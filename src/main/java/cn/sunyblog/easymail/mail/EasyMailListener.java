package cn.sunyblog.easymail.mail;

import cn.sunyblog.easymail.config.EasyMailImapConfig;
import cn.sunyblog.easymail.exception.EasyMailException;
import cn.sunyblog.easymail.exception.EasyMailExceptionHandler;
import cn.sunyblog.easymail.exception.EasyMailProcessException;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
 * <p>
 * 针对网易企业邮箱IDLE监听优化说明：
 * 1. IDLE退出后强制刷新文件夹状态，解决新邮件被忽略问题
 * 2. 增加针对网易邮箱的特殊处理逻辑和延迟配置
 * 3. 改进消息检索逻辑，直接处理SEARCH结果而不依赖消息编号范围
 * 4. 添加IDLE失败时的降级机制，确保邮件监听的可靠性
 * 5. 增强调试日志，便于问题排查和性能监控
 * @date 2025/05/12 16:23
 */
@Slf4j
@Data
@Component
public class EasyMailListener {

    @Autowired
    private EasyMailImapConfig easyMailImapConfig;
    @Autowired
    private EasyMailProcessor easyMailProcessor;
    @Autowired
    private ExecutorService noticeThreadPool;
    @Autowired
    private EasyMailCache easyMailCache;
    @Autowired
    private EasyMailServerConnector easyMailServerConnector;

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
    public void startListening(EasyMailServerConnector serverConnector) {
        int retryCount = 0;
        int maxRetries = easyMailImapConfig.getListener().getMaxRetries(); // 最大重试次数
        long retryDelay = easyMailImapConfig.getMonitor().getShortDelay() * 1000L; // 重试延迟(毫秒)

        while (retryCount < maxRetries) {
            try {
                // 创建会话并连接服务器
                session = serverConnector.createSession();
                store = serverConnector.connectToServer(session);
                folder = serverConnector.openInbox(store);

                // 设置新邮件监听器
                setupMessageListener();

                // 开始监听
                log.debug("开始监听新邮件");
                isRunning.set(true);

                // 启动监听线程（包含处理现有未读邮件的逻辑）
                startMonitorThread(serverConnector);

                // 启动保活线程
                startKeepAliveThread();

                log.debug("邮件监听服务启动完成");
                return; // 成功启动，退出方法
            } catch (Exception e) {
                retryCount++;
                EasyMailException connectionEx = EasyMailExceptionHandler.wrapException(e,
                        "启动邮件监听失败(第" + retryCount + "次尝试，共" + maxRetries + "次)");
                log.error(connectionEx.getFullErrorMessage(), connectionEx);

                // 关闭可能部分打开的资源
                serverConnector.closeConnection(folder, store);

                if (retryCount < maxRetries) {
                    log.info("将在{}秒后重试连接邮件服务器", easyMailImapConfig.getMonitor().getShortDelay());
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
    private void startRecoveryThread(EasyMailServerConnector serverConnector) {
        Thread recoveryThread = new Thread(() -> {
            log.info("启动邮件服务恢复线程，将定期尝试重新连接");
            int recoveryAttempt = 0;
            long connectionTimeout = easyMailImapConfig.getConnection().getTimeout() * 1000L; // 连接超时时间(毫秒)
            log.info("连接超时设置：{}秒", connectionTimeout / 1000);
            int maxRecoveryAttempts = easyMailImapConfig.getMonitor().getReconnectDelay(); // 最大恢复尝试次数
            while (!isRunning.get() && !Thread.currentThread().isInterrupted() && recoveryAttempt < maxRecoveryAttempts) {
                recoveryAttempt++;
                log.info("尝试恢复邮件服务连接(第{}次恢复尝试)最大尝试连接次数：{}", recoveryAttempt, maxRecoveryAttempts);

                try {
                    // 创建会话并连接服务器
                    session = serverConnector.createSession();
                    store = serverConnector.connectToServer(session);
                    folder = serverConnector.openInbox(store);

                    // 根据配置策略处理现有未读邮件
                    EasyMailImapConfig.StartupProcessStrategy strategy = easyMailImapConfig.getListener().getStartupProcessStrategy();
                    if (strategy != EasyMailImapConfig.StartupProcessStrategy.IGNORE_EXISTING) {
                        processExistingUnreadEmails();
                    } else {
                        log.info("IGNORE_EXISTING模式：恢复连接时跳过处理现有未读邮件");
                    }

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
                    EasyMailException connectionEx = EasyMailExceptionHandler.wrapException(e,
                            "恢复邮件服务连接失败");
                    log.error(connectionEx.getFullErrorMessage(), connectionEx);

                    // 关闭可能部分打开的资源
                    serverConnector.closeConnection(folder, store);

                    try {
                        log.info("将在{}秒后再次尝试恢复连接", easyMailImapConfig.getMonitor().getLongDelay());
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
                // 检查服务是否仍在运行
                if (!isRunning.get()) {
                    log.debug("邮件监听服务已停止，忽略新邮件事件");
                    return;
                }

                // IGNORE_EXISTING模式只在启动时忽略旧邮件，新邮件事件应该正常处理
                // 因为messagesAdded是邮件服务器推送的新邮件事件，不是启动时的旧邮件

                processingEvent.set(true); // 设置标志位
                try {
                    Message[] messages = e.getMessages();
                    long eventStartTime = System.currentTimeMillis();
                    log.debug("收到{}封新邮件", messages.length);

                    // 使用线程池处理邮件，不阻塞JavaMail事件线程
                    for (Message message : messages) {
                        // 再次检查服务状态，避免在处理过程中服务被关闭
                        if (!isRunning.get()) {
                            log.debug("邮件监听服务已停止，跳过新邮件处理");
                            break;
                        }

                        final Message finalMessage = message;
                        try {
                            noticeThreadPool.execute(() -> {
                                // 在任务执行时检查状态
                                if (!isRunning.get()) {
                                    log.debug("邮件监听服务已停止，跳过邮件处理任务");
                                    return;
                                }
                                easyMailProcessor.processMessage(finalMessage);
                            });
                        } catch (java.util.concurrent.RejectedExecutionException eg) {
                            log.warn("线程池已满，跳过邮件处理: {}", eg.getMessage());
                            break; // 线程池满了就停止提交新任务
                        }
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
     * 根据配置策略处理现有未读邮件
     */
    private void processExistingUnreadEmails() {
        EasyMailImapConfig.StartupProcessStrategy strategy = easyMailImapConfig.getListener().getStartupProcessStrategy();

        // 如果策略是忽略现有邮件，直接返回
        if (strategy == EasyMailImapConfig.StartupProcessStrategy.IGNORE_EXISTING) {
            log.info("配置为忽略现有未读邮件，跳过处理");
            return;
        }

        log.debug("开始处理现有未读邮件，策略: {}", strategy);

        switch (strategy) {
            case MARK_AS_READ_ONLY:
                markUnreadEmailsAsRead();
                break;
            case FULL_PROCESS:
                processUnreadEmailsWithContent();
                break;
            default:
                log.warn("未知的启动处理策略: {}, 使用默认策略", strategy);
                markUnreadEmailsAsRead();
                break;
        }
    }

    /**
     * 只标记未读邮件为已读，不处理内容
     */
    private void markUnreadEmailsAsRead() {
        try {
            long startTime = System.currentTimeMillis();

            // 查找未读邮件
            FlagTerm ft = new FlagTerm(new Flags(Flags.Flag.SEEN), false);
            Message[] messages;
            try {
                messages = folder.search(ft);
            } catch (javax.mail.FolderClosedException e) {
                log.warn("文件夹已关闭，无法标记未读邮件为已读: {}", e.getMessage());
                return;
            }

            if (messages.length == 0) {
                log.info("没有未读邮件需要标记");
                return;
            }

            log.info("开始标记{}封未读邮件为已读", messages.length);

            // 批量标记为已读
            for (Message message : messages) {
                try {
                    message.setFlag(Flags.Flag.SEEN, true);
                } catch (javax.mail.FolderClosedException e) {
                    log.warn("文件夹已关闭，无法标记邮件为已读: {}", e.getMessage());
                    break; // 文件夹关闭时停止处理
                } catch (Exception e) {
                    log.warn("标记邮件为已读失败: {}", e.getMessage());
                }
            }

            long endTime = System.currentTimeMillis();
            log.info("批量标记{}封邮件为已读完成，总耗时: {}毫秒",
                    messages.length, endTime - startTime);

        } catch (javax.mail.FolderClosedException e) {
            log.warn("文件夹已关闭，标记未读邮件为已读操作终止: {}", e.getMessage());
        } catch (Exception e) {
            log.error("标记未读邮件为已读失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 完全处理未读邮件（包括内容解析和业务处理）
     */
    private void processUnreadEmailsWithContent() {
        try {
            long startTime = System.currentTimeMillis();

            // 查找未读邮件
            FlagTerm ft = new FlagTerm(new Flags(Flags.Flag.SEEN), false);
            Message[] messages;
            try {
                messages = folder.search(ft);
            } catch (javax.mail.FolderClosedException e) {
                log.warn("文件夹已关闭，无法处理未读邮件: {}", e.getMessage());
                return;
            }

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
                // 检查服务是否仍在运行，避免在关闭过程中提交新任务
                if (!isRunning.get()) {
                    log.debug("邮件监听服务已停止，跳过未读邮件处理");
                    latch.countDown(); // 减少计数器
                    continue;
                }

                final Message finalMessage = message;
                try {
                    noticeThreadPool.execute(() -> {
                        try {
                            // 在任务执行时再次检查状态
                            if (!isRunning.get()) {
                                log.debug("邮件监听服务已停止，跳过邮件处理任务");
                                return;
                            }
                            boolean processed = easyMailProcessor.processMessage(finalMessage);
                            if (processed) {
                                synchronized (lock) {
                                    processedCount[0]++;
                                }
                            }
                        } finally {
                            latch.countDown();
                        }
                    });
                } catch (java.util.concurrent.RejectedExecutionException e) {
                    log.warn("线程池已满，跳过未读邮件处理: {}", e.getMessage());
                    latch.countDown(); // 确保计数器正确减少
                }
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
        } catch (javax.mail.FolderClosedException e) {
            log.warn("文件夹已关闭，处理未读邮件操作终止: {}", e.getMessage());
        } catch (Exception e) {
            EasyMailProcessException processEx = EasyMailProcessException.processingError("处理未读邮件失败", e);
            log.error(processEx.getFullErrorMessage(), processEx);
        }
    }

    /**
     * 启动监听线程
     *
     * @param serverConnector 服务器连接器
     */
    private void startMonitorThread(EasyMailServerConnector serverConnector) {
        monitorThread = new Thread(() -> {
            log.debug("邮件监听线程已启动");

            // 根据配置策略处理现有未读邮件
            EasyMailImapConfig.StartupProcessStrategy strategy = easyMailImapConfig.getListener().getStartupProcessStrategy();
            if (strategy != EasyMailImapConfig.StartupProcessStrategy.IGNORE_EXISTING) {
                processExistingUnreadEmails();
            } else {
                log.info("IGNORE_EXISTING模式：跳过处理现有未读邮件，仅监听新邮件");
            }

            // 添加一个短暂延迟，避免与初始化时的事件检测冲突
            try {
                TimeUnit.MILLISECONDS.sleep(500);
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
                        boolean isIdleMode = false;
                        try {
                            // 获取IMAP协议对象并检查能力
                            if (imapFolder.getStore() instanceof IMAPStore) {
                                IMAPStore imapStore = (IMAPStore) imapFolder.getStore();
                                supportsIdle = imapStore.hasCapability("IDLE");
                                isIdleMode = supportsIdle;
                                if (supportsIdle) {
                                    log.debug("检测到IDLE支持，将使用优化的IDLE策略");
                                }
                            }
                        } catch (Exception e) {
                            EasyMailException connectionEx = EasyMailExceptionHandler.wrapException(e,
                                    "检查IDLE能力时出错");
                            log.warn(connectionEx.getFullErrorMessage());
                        }

                        if (supportsIdle) {
                            // IDLE命令会阻塞，直到有新邮件或超时
                            log.debug("进入IDLE模式");
                            try {
                                // 使用标准IDLE配置
                                //log.info("进入 imapFolder.idle(true);方法中");
                                imapFolder.idle(true);
                                //log.info("退出 imapFolder.idle(true);");
                            } catch (MessagingException e) {
                                EasyMailException connectionEx = EasyMailExceptionHandler.wrapMessagingException(e,
                                        "IDLE命令执行失败");
                                log.warn(connectionEx.getFullErrorMessage());

                                // IDLE失败时降级到轮询模式
                                log.info("IDLE失败，降级到轮询模式");
                                try {
                                    TimeUnit.SECONDS.sleep(easyMailImapConfig.getMonitor().getShortDelay());
                                } catch (InterruptedException ie) {
                                    Thread.currentThread().interrupt();
                                    return;
                                }
                                checkNewMessages();
                                continue; // 跳过后续IDLE处理，直接进入下一轮循环
                            }
                            log.debug("退出IDLE模式");

                            // IDLE模式优化：IDLE退出后轻量级刷新文件夹状态
                            try {
                                // IDLE模式：使用轻量级刷新策略，避免频繁重新打开文件夹
                                log.debug("IDLE模式：执行轻量级文件夹刷新");

                                // 检查连接状态
                                if (store == null || !store.isConnected()) {
                                    log.warn("IDLE模式：Store连接已断开，跳过刷新");
                                    continue;
                                }

                                // 只进行轻量级刷新，不重新打开文件夹
                                int messageCount = imapFolder.getMessageCount();
                                log.debug("IDLE模式轻量级刷新：当前邮件数={}", messageCount);

                                // 减少延迟提高响应速度
                                TimeUnit.MILLISECONDS.sleep(50);
                            } catch (FolderClosedException fce) {
                                log.warn("刷新时文件夹连接断开: {}", fce.getMessage());
                                try {
                                    // 检查store连接
                                    if (store != null && store.isConnected()) {
                                        imapFolder.open(Folder.READ_WRITE);
                                        setupMessageListener();
                                        log.info("文件夹重新连接成功");
                                    } else {
                                        log.error("Store连接已断开，无法重新打开文件夹");
                                        continue;
                                    }
                                } catch (Exception reopenEx) {
                                    log.error("重新打开文件夹失败: {}", reopenEx.getMessage());
                                    continue;
                                }
                            } catch (Exception refreshEx) {
                                log.warn("刷新文件夹状态失败: {}", refreshEx.getMessage());
                                // 如果刷新失败，尝试重新打开文件夹
                                try {
                                    if (!imapFolder.isOpen()) {
                                        imapFolder.open(Folder.READ_WRITE);
                                        setupMessageListener();
                                    }
                                } catch (Exception reopenEx) {
                                    log.error("重新打开文件夹失败: {}", reopenEx.getMessage());
                                }
                            }

                            // IDLE退出后强制同步文件夹状态，解决邮件编号超出范围问题
                            try {
                                if (imapFolder.isOpen()) {
                                    // 关闭并重新打开文件夹，确保完全同步
                                    imapFolder.close(false);
                                    imapFolder.open(Folder.READ_WRITE);
                                    setupMessageListener();
                                    int messageCount = imapFolder.getMessageCount();
                                    log.debug("IDLE退出后已重新打开文件夹并同步状态，当前邮件数: {}", messageCount);
                                }
                            } catch (Exception syncEx) {
                                log.warn("IDLE退出后同步文件夹状态失败: {}", syncEx.getMessage());
                                // 如果同步失败，尝试简单的重新打开
                                try {
                                    if (!imapFolder.isOpen()) {
                                        imapFolder.open(Folder.READ_WRITE);
                                        setupMessageListener();
                                    }
                                } catch (Exception reopenEx) {
                                    log.error("重新打开文件夹失败: {}", reopenEx.getMessage());
                                }
                            }

                            // 在IDLE之后，主动检查一次新邮件
                            checkNewMessages();

                            // IDLE模式下添加快速检查
                            if (isIdleMode) {
                                try {
                                    TimeUnit.MILLISECONDS.sleep(50); // 大幅减少延迟
                                    // 再次检查，确保没有遗漏的邮件
                                    checkNewMessages();
                                } catch (InterruptedException ie) {
                                    Thread.currentThread().interrupt();
                                    return;
                                }
                            }
                        } else {
                            // 如果不支持IDLE，使用轮询
                            //log.warn("当前邮件服务不支持IDLE模式，使用轮询方式");
                            try {
                                TimeUnit.SECONDS.sleep(easyMailImapConfig.getMonitor().getShortDelay());
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
                        TimeUnit.SECONDS.sleep(easyMailImapConfig.getMonitor().getShortDelay());
                        checkNewMessages();
                    }
                } catch (FolderClosedException fce) {
                    log.warn("文件夹已关闭: {}", fce.getMessage());
                    try {
                        if (!folder.isOpen()) {
                            folder.open(Folder.READ_WRITE);
                            log.info("已重新打开文件夹");
                        }
                        TimeUnit.SECONDS.sleep(easyMailImapConfig.getMonitor().getShortDelay());
                    } catch (Exception reconnectEx) {
                        // 检查是否为认证失败异常
                        if (isAuthenticationFailure(reconnectEx)) {
                            log.warn("重新打开文件夹时认证失败，停止监听服务");
                            isRunning.set(false);
                            break;
                        }

                        EasyMailException connectionEx = EasyMailExceptionHandler.wrapException(reconnectEx,
                                "重新打开文件夹失败");
                        log.error(connectionEx.getFullErrorMessage(), connectionEx);
                        try {
                            TimeUnit.SECONDS.sleep(1); // 快速重连，减少延迟
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            log.warn("线程被中断");
                        }
                    }
                } catch (MessagingException me) {
                    // 检查是否为认证失败异常
                    if (isAuthenticationFailure(me)) {
                        log.warn("邮件服务器认证失败，停止监听服务");
                        isRunning.set(false);
                        break;
                    }

                    EasyMailException connectionEx = EasyMailExceptionHandler.wrapMessagingException(me,
                            "邮件服务异常");
                    log.error(connectionEx.getFullErrorMessage(), connectionEx);

                    // 只有在非认证失败的情况下才尝试重连
                    try {
                        // 尝试重新连接
                        folder = serverConnector.reconnectIfNeeded(store, folder);
                        // 重新设置监听器
                        setupMessageListener();
                        log.info("邮件服务器重连成功，已重新设置监听器");
                        TimeUnit.SECONDS.sleep(1); // 快速重连，减少延迟
                    } catch (Exception reconnectEx) {
                        // 检查重连异常是否也是认证失败
                        if (isAuthenticationFailure(reconnectEx)) {
                            log.warn("重连时认证失败，停止监听服务");
                            isRunning.set(false);
                            break;
                        }

                        EasyMailException reconnectConnectionEx = EasyMailExceptionHandler.wrapException(reconnectEx,
                                "重新连接失败");
                        log.error(reconnectConnectionEx.getFullErrorMessage(), reconnectConnectionEx);
                        try {
                            TimeUnit.SECONDS.sleep(easyMailImapConfig.getMonitor().getLongDelay());
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            log.warn("线程被中断");
                        }
                    }
                } catch (Exception e) {
                    EasyMailProcessException processEx = EasyMailProcessException.processingError("邮件监听异常", e);
                    log.error(processEx.getFullErrorMessage(), processEx);
                    try {
                        TimeUnit.SECONDS.sleep(1); // 快速重连，减少延迟
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("线程被中断");
                    }
                }
            }
            log.debug("邮件监听线程已停止");
        });

        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    /**
     * 启动保活线程
     */
    private void startKeepAliveThread() {
        keepAliveThread = new Thread(() -> {
            log.debug("保活线程已启动");
            while (isRunning.get()) {
                try {
                    if (store != null && store.isConnected() &&
                            folder != null && folder.isOpen()) {
                        // 发送NOOP命令中断IDLE并保持连接活跃
                        if (folder instanceof IMAPFolder) {
                            IMAPFolder imapFolder = (IMAPFolder) folder;
                            try {
                                // 使用NOOP命令中断IDLE，这是标准做法
                                imapFolder.doCommand(protocol -> {
                                    protocol.simpleCommand("NOOP", null);
                                    return null;
                                });
                                log.debug("发送NOOP命令中断IDLE");
                            } catch (Exception e) {
                                // 如果NOOP失败，降级到getMessageCount
                                folder.getMessageCount();
                                log.debug("NOOP失败，使用getMessageCount保活");
                            }
                        } else {
                            // 非IMAP文件夹使用标准方法
                            folder.getMessageCount();
                            log.debug("发送保活信号");
                        }
                    } else {
                        log.warn("保活线程检测到连接断开，等待主线程重连");
                        // 保活线程不直接重连，避免与主线程冲突
                        // 这里只是记录状态，让主监听线程处理重连
                    }
                    TimeUnit.SECONDS.sleep(easyMailImapConfig.getMonitor().getKeepAliveInterval());
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
                        // 检查是否为认证失败异常
                        if (isAuthenticationFailure(e)) {
                            log.warn("保活线程检测到认证失败，停止监听服务");
                            isRunning.set(false);
                            break;
                        }

                        // 对于其他异常，只记录debug级别日志，避免过多错误日志
                        log.debug("保活线程异常: {}", e.getMessage());
                        try {
                            TimeUnit.SECONDS.sleep(easyMailImapConfig.getMonitor().getShortDelay());
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

        // 检查连接状态，如果连接已断开则不进行轮询
        if (store == null || !store.isConnected()) {
            log.debug("邮件服务器连接已断开，跳过轮询检查");
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

            // IDLE模式优化：改进消息检索逻辑
            // 检测当前是否为IDLE模式
            boolean isIdleMode = folder instanceof IMAPFolder &&
                    folder.getStore() instanceof IMAPStore &&
                    ((IMAPStore) folder.getStore()).hasCapability("IDLE");

            // 先强制刷新文件夹状态，确保获取最新的邮件信息
            try {
                // 检查连接状态
                if (store == null || !store.isConnected()) {
                    log.warn("Store连接已断开，跳过邮件检查");
                    return;
                }

                if (isIdleMode && folder instanceof IMAPFolder) {
                    // IDLE模式：使用轻量级刷新策略，避免频繁重新打开文件夹
                    IMAPFolder imapFolder = (IMAPFolder) folder;

                    // 只进行轻量级刷新，不重新打开文件夹
                    int messageCount = imapFolder.getMessageCount();
                    log.debug("IDLE模式轻量级刷新：当前邮件数={}", messageCount);

                    // 减少延迟提高响应速度
                    Thread.sleep(100);
                } else {
                    // 轮询模式：标准刷新
                    folder.getMessageCount(); // 强制同步文件夹状态
                    log.debug("已刷新文件夹状态，当前邮件总数: {}", folder.getMessageCount());
                }
            } catch (FolderClosedException fce) {
                log.warn("轮询刷新时文件夹连接断开: {}", fce.getMessage());
                try {
                    // 检查store连接
                    if (store != null && store.isConnected()) {
                        folder.open(Folder.READ_WRITE);
                        setupMessageListener();
                        log.info("轮询时文件夹重新连接成功");
                    } else {
                        log.error("Store连接已断开，无法重新打开文件夹");
                        return;
                    }
                } catch (Exception reopenEx) {
                    log.error("轮询时重新打开文件夹失败: {}", reopenEx.getMessage());
                    return;
                }
            } catch (Exception e) {
                log.debug("刷新文件夹状态时出现异常: {}", e.getMessage());
                // 如果刷新失败，尝试重新打开文件夹
                try {
                    if (!folder.isOpen()) {
                        folder.open(Folder.READ_WRITE);
                        setupMessageListener();
                    }
                } catch (Exception reopenEx) {
                    log.warn("重新打开文件夹失败: {}", reopenEx.getMessage());
                }
            }

            // 获取未读邮件
            FlagTerm ft = new FlagTerm(new Flags(Flags.Flag.SEEN), false);
            Message[] messages;
            try {
                messages = folder.search(ft);
            } catch (javax.mail.FolderClosedException e) {
                log.warn("文件夹已关闭，无法搜索未读邮件: {}", e.getMessage());
                try {
                    if (store != null && store.isConnected()) {
                        folder.open(Folder.READ_WRITE);
                        setupMessageListener();
                        messages = folder.search(ft);
                        log.info("文件夹重新连接成功，继续搜索未读邮件");
                    } else {
                        log.error("Store连接已断开，无法重新搜索未读邮件");
                        return;
                    }
                } catch (Exception reopenEx) {
                    log.error("重新打开文件夹后搜索失败: {}", reopenEx.getMessage());
                    return;
                }
            }

            // IDLE模式优化：如果SEARCH返回空结果但可能有新邮件，尝试直接获取最新邮件
            if (messages.length == 0 && isIdleMode && folder instanceof IMAPFolder) {
                try {
                    // 再次检查连接状态
                    if (store == null || !store.isConnected()) {
                        log.warn("IDLE模式：Store连接已断开，跳过直接检查");
                    } else {
                        IMAPFolder imapFolder = (IMAPFolder) folder;

                        // 轻量级刷新文件夹状态
                        int totalCount = imapFolder.getMessageCount();
                        Thread.sleep(100);

                        if (totalCount > 0) {
                            log.debug("IDLE模式：SEARCH返回空，轻量级检查最后3封邮件，总邮件数: {}", totalCount);
                            // 检查最后几封邮件是否为未读
                            int checkCount = Math.min(3, totalCount); // 减少检查数量提高效率
                            Message[] recentMessages = imapFolder.getMessages(totalCount - checkCount + 1, totalCount);

                            java.util.List<Message> unreadMessages = new java.util.ArrayList<>();
                            for (Message msg : recentMessages) {
                                try {
                                    // 确保消息有效且未被删除
                                    if (msg != null && !msg.isExpunged() && !msg.isSet(Flags.Flag.SEEN)) {
                                        unreadMessages.add(msg);
                                        log.debug("IDLE模式：发现未读邮件: {}", msg.getMessageNumber());
                                    }
                                } catch (Exception msgEx) {
                                    log.debug("IDLE模式：检查邮件状态失败: {}", msgEx.getMessage());
                                }
                            }

                            if (!unreadMessages.isEmpty()) {
                                messages = unreadMessages.toArray(new Message[0]);
                                log.info("IDLE模式：通过轻量级检查发现 {} 封未读邮件", messages.length);
                            }
                        }
                    }
                } catch (FolderClosedException fce) {
                    log.warn("IDLE模式：直接检查时文件夹连接断开: {}", fce.getMessage());
                    try {
                        if (store != null && store.isConnected()) {
                            folder.open(Folder.READ_WRITE);
                            setupMessageListener();
                            log.info("IDLE模式：直接检查时文件夹重新连接成功");
                        }
                    } catch (Exception reopenEx) {
                        log.error("IDLE模式：直接检查时重新打开文件夹失败: {}", reopenEx.getMessage());
                    }
                } catch (Exception directEx) {
                    log.debug("IDLE模式：直接获取最新邮件失败: {}", directEx.getMessage());
                }
            }

            if (messages.length > 0) {
                log.debug("轮询检测到{}封未读邮件", messages.length);

                // 记录邮件编号范围用于调试
                int minMsgNum = Integer.MAX_VALUE;
                int maxMsgNum = Integer.MIN_VALUE;
                for (Message msg : messages) {
                    int msgNum = msg.getMessageNumber();
                    minMsgNum = Math.min(minMsgNum, msgNum);
                    maxMsgNum = Math.max(maxMsgNum, msgNum);
                }
                log.debug("未读邮件编号范围: {} - {}, 文件夹总邮件数: {}",
                        minMsgNum, maxMsgNum, folder.getMessageCount());

                // 检查启动处理策略
                EasyMailImapConfig.StartupProcessStrategy strategy = easyMailImapConfig.getListener().getStartupProcessStrategy();

                for (Message message : messages) {
                    // IDLE模式优化：增加消息有效性检查和连接状态确保
                    try {
                        // 首先检查store和folder的连接状态
                        if (store == null || !store.isConnected()) {
                            log.warn("[IDLE优化] Store连接已断开，跳过此邮件");
                            continue;
                        }

                        // 确保文件夹仍然打开
                        if (!folder.isOpen()) {
                            log.warn("[IDLE优化] 文件夹已关闭，重新打开");
                            folder.open(Folder.READ_WRITE);
                            setupMessageListener();
                        }

                        // 验证消息对象本身是否为null
                        if (message == null) {
                            log.warn("[IDLE优化] 消息对象为null，跳过");
                            continue;
                        }

                        // 验证消息是否仍然有效（先检查是否被删除）
                        if (message.isExpunged()) {
                            log.warn("[IDLE优化] 跳过已删除的邮件");
                            continue;
                        }

                        // 预先检查消息是否有效，避免后续处理时出现异常
                        int msgNumber = message.getMessageNumber();
                        if (msgNumber <= 0) {
                            log.warn("[IDLE优化] 无效的消息编号: {}", msgNumber);
                            continue;
                        }

                        // 尝试获取消息主题来验证消息完整性
                        String subject = null;
                        try {
                            subject = message.getSubject();
                        } catch (Exception subjectEx) {
                            log.warn("[IDLE优化] 无法获取消息主题，可能消息已损坏: {}", subjectEx.getMessage());
                            continue;
                        }

                        log.debug("[IDLE优化] 验证邮件有效性 - 编号: {}, 主题: {}",
                                msgNumber, subject != null ? subject.substring(0, Math.min(subject.length(), 30)) + "..." : "无主题");

                    } catch (FolderClosedException fce) {
                        log.warn("[IDLE优化] 文件夹连接已断开，尝试重新连接: {}", fce.getMessage());
                        try {
                            // 检查store连接状态
                            if (store == null || !store.isConnected()) {
                                log.error("[IDLE优化] Store连接已断开，无法重新连接文件夹");
                                continue;
                            }

                            // 重新打开文件夹
                            folder.open(Folder.READ_WRITE);
                            setupMessageListener();

                            // 重新获取消息
                            int originalMsgNum = message.getMessageNumber();
                            message = folder.getMessage(originalMsgNum);
                            log.info("[IDLE优化] 重新连接成功，继续处理邮件: {}", originalMsgNum);
                        } catch (Exception reconnectEx) {
                            log.error("[IDLE优化] 重新连接失败，跳过此邮件: {}", reconnectEx.getMessage());
                            continue;
                        }
                    } catch (Exception msgEx) {
                        log.warn("[IDLE优化] 跳过无效邮件消息: {}", msgEx.getMessage());
                        continue;
                    }

                    String messageId = easyMailCache.getMessageId(message);

                    // 如果是IGNORE_EXISTING模式，只处理缓存中没有记录的邮件（真正的新邮件）
                    if (strategy == EasyMailImapConfig.StartupProcessStrategy.IGNORE_EXISTING) {
                        // 在IGNORE_EXISTING模式下，只有通过事件监听器接收到的新邮件才会被处理
                        // 轮询检查时发现的未读邮件可能是启动前就存在的，应该跳过
                        if (!easyMailCache.shouldProcessMessage(messageId)) {
                            log.debug("IGNORE_EXISTING模式：邮件已处理，跳过轮询处理: {}", messageId);
                            continue;
                        } else {
                            // 在IGNORE_EXISTING模式下，轮询发现的未处理邮件被认为是启动前的旧邮件，跳过处理
                            log.debug("IGNORE_EXISTING模式：跳过处理启动前存在的未读邮件: {}", messageId);
                            // 将其标记为已处理，避免后续重复检查
                            easyMailCache.checkAndMarkAsProcessed(messageId);
                            continue;
                        }
                    }

                    // 非IGNORE_EXISTING模式的正常处理逻辑
                    if (!easyMailCache.shouldProcessMessage(messageId)) {
                        log.debug("邮件已处理，跳过轮询处理: {}", messageId);
                        continue;
                    }

                    // 创建final变量以在lambda中使用
                    final Message finalMessage = message;
                    noticeThreadPool.execute(() -> easyMailProcessor.processMessage(finalMessage));
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
                // 检查是否为认证失败异常，如果是则不记录详细错误信息
                if (isAuthenticationFailure(e)) {
                    log.warn("邮件服务器认证失败，停止轮询检查");
                    // 认证失败时停止监听服务
                    isRunning.set(false);
                } else {
                    EasyMailProcessException processEx = EasyMailProcessException.processingError("轮询检查邮件异常", e);
                    log.error(processEx.getFullErrorMessage(), processEx);
                }
            } else {
                log.debug("邮件监听服务已停止，操作被中断: {}", e.getMessage());
            }
        }
    }

    /**
     * 检查是否为认证失败异常
     */
    private boolean isAuthenticationFailure(Exception e) {
        String message = e.getMessage();
        if (message == null) {
            return false;
        }
        return message.contains("Unsafe Login") ||
                message.contains("authentication failed") ||
                message.contains("LOGIN failed") ||
                message.contains("Invalid credentials") ||
                message.contains("AUTH") ||
                message.contains("AUTHENTICATE");
    }

    /**
     * 检查是否为认证失败异常（MessagingException版本）
     */
    private boolean isAuthenticationFailure(MessagingException e) {
        return isAuthenticationFailure((Exception) e);
    }

    /**
     * 停止邮件监听
     *
     * @param serverConnector 服务器连接器
     */
    public void stopListening(EasyMailServerConnector serverConnector) {
        //log.info("正在关闭邮件监听服务");

        // 首先设置停止标志，阻止新任务提交
        isRunning.set(false);

        // 优雅关闭线程池
        if (noticeThreadPool != null && !noticeThreadPool.isShutdown()) {
            //log.info("正在关闭邮件处理线程池");
            noticeThreadPool.shutdown(); // 不再接受新任务

            try {
                // 先等待3秒让正在执行的任务完成
                if (!noticeThreadPool.awaitTermination(3, TimeUnit.SECONDS)) {
                    log.warn("邮件处理任务未能在3秒内完成，强制关闭线程池");
                    noticeThreadPool.shutdownNow(); // 强制关闭

                    // 再等待1秒确保线程被中断
                    if (!noticeThreadPool.awaitTermination(1, TimeUnit.SECONDS)) {
                        log.error("无法强制关闭邮件处理线程池");
                    }  //log.info("邮件处理线程池已强制关闭");
                }
            } catch (InterruptedException e) {
                log.warn("等待线程池关闭被中断");
                noticeThreadPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // 中断监听线程
        if (monitorThread != null) {
            monitorThread.interrupt();
            try {
                monitorThread.join(3000); // 等待最多3秒
            } catch (InterruptedException e) {
                log.warn("等待监听线程结束被中断");
                Thread.currentThread().interrupt();
            }
        }

        // 中断保活线程
        if (keepAliveThread != null) {
            keepAliveThread.interrupt();
            try {
                keepAliveThread.join(3000); // 等待最多3秒
            } catch (InterruptedException e) {
                log.warn("等待保活线程结束被中断");
                Thread.currentThread().interrupt();
            }
        }

        // 最后关闭邮件服务器连接
        serverConnector.closeConnection(folder, store);

        //log.info("邮件监听服务已成功关闭");
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