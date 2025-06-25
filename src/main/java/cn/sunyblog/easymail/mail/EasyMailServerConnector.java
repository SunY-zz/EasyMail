package cn.sunyblog.easymail.mail;

import cn.sunyblog.easymail.config.EasyMailImapConfig;
import cn.sunyblog.easymail.exception.EasyMailException;
import cn.sunyblog.easymail.exception.EasyMailExceptionHandler;
import cn.sunyblog.easymail.util.EasyMailSSLTrustUtil;
import com.sun.mail.imap.IMAPStore;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author suny
 * @version 1.0
 * @description: 邮件服务器连接管理   负责连接和断开邮件服务器
 * @date 2025/05/12 16:24
 */
@Slf4j
@Data
@Component
public class EasyMailServerConnector {

    @Autowired
    private EasyMailImapConfig mailConfig;


    /**
     * 创建邮件会话
     *
     * @return 邮件会话对象
     */
    public Session createSession() {
        Properties props = configureProperties();
        Session session = Session.getDefaultInstance(props);
        session.setDebug(mailConfig.getLog().isDebugEnabled());
        return session;
    }

    /**
     * 配置邮件属性
     *
     * @return 配置好的属性对象
     */
    private Properties configureProperties() {
        Properties props = new Properties();
        // 基本连接属性
        props.setProperty("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.setProperty("mail.imap.socketFactory.fallback", "false");
        props.setProperty("mail.transport.protocol", mailConfig.getProtocol());
        props.setProperty("mail.imap.port", mailConfig.getPort());
        props.setProperty("mail.imap.socketFactory.port", mailConfig.getPort());

        // SSL调试（仅在需要排除问题时开启）
        if (mailConfig.getLog().isDebugEnabled()) {
            System.setProperty("javax.net.debug", "ssl,handshake");
        }

        // 连接超时参数
        props.setProperty("mail.imap.connectiontimeout", String.valueOf(mailConfig.getConnection().getTimeout()));
        props.setProperty("mail.imap.timeout", String.valueOf(mailConfig.getConnection().getReadTimeout()));
        props.setProperty("mail.imap.writetimeout", String.valueOf(mailConfig.getConnection().getWriteTimeout()));
        props.setProperty("mail.imap.statuscachetimeout", "1000");
        props.setProperty("mail.imap.minidletime", String.valueOf(mailConfig.getMonitor().getIdleTimeout()));
        props.setProperty("mail.imap.keepalive", "true");
        props.setProperty("mail.imap.connectionpoolsize", "1");

        // IDLE模式支持
        props.setProperty("mail.imap.usesocketchannels", "true");
        props.setProperty("mail.imap.enableimapevents", "true");
        props.setProperty("mail.event.scope", "session");
        // 添加SSL证书信任设置
        String[] split = mailConfig.getServer().split("\\.");
        props.setProperty("mail.imap.ssl.trust", "*" + "." + split[1] + "." + split[2]);
        props.setProperty("mail.imap.ssl.checkserveridentity", "false");

        // 禁用所有SSL验证
        props.setProperty("mail.imap.ssl.enable", "true");
        props.setProperty("mail.imap.starttls.enable", "true");
        props.setProperty("mail.imap.starttls.required", "false");
        // 使用自定义的SSL工厂
        props.put("mail.imap.ssl.socketFactory", new EasyMailSSLTrustUtil.TrustAllSSLSocketFactory());

        return props;
    }

    /**
     * 连接到邮件服务器，支持多次快速重试。因为大多数连接超时并非真正意义上的超时，只需要多次尝试连接，如果五次快速连接都失败，则进行较长的超时连接
     *
     * @param session JavaMail Session对象
     * @return 成功连接的Store对象
     * @throws MessagingException 连接失败时抛出异常
     */
    public Store connectToServer(Session session) throws MessagingException {
        log.info("正在连接邮件服务器: {}", mailConfig.getServer());

        // 快速重试配置
        final int quickRetryCount = 5;          // 快速重试次数
        final int quickRetryTimeout = 3;        // 每次快速重试的超时时间(秒)

        // 快速重试阶段
        for (int attempt = 1; attempt <= quickRetryCount; attempt++) {
            try {
                log.debug("连接尝试 {} (超时: {}秒)", attempt, quickRetryTimeout);
                return attemptConnection(session, quickRetryTimeout);
            } catch (MessagingException e) {
                log.warn("连接尝试 {} 失败: {}", attempt, e.getMessage());

                // 短暂等待后重试
                try {
                    Thread.sleep(500); // 500毫秒间隔
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new MessagingException("连接重试被中断");
                }
            }
        }
        int connectionTimeout = mailConfig.getConnection().getTimeout() / 1000;
        // 最终尝试（使用较长超时）
        log.debug("快速重试失败，进行最终尝试 (超时: {}秒)", connectionTimeout);
        try {
            return attemptConnection(session, connectionTimeout);
        } catch (MessagingException e) {
            EasyMailException connectionEx = EasyMailExceptionHandler.wrapMessagingException(e, "连接邮件服务器失败");
            log.error(connectionEx.getFullErrorMessage(), connectionEx);
            throw connectionEx;
        }
    }

    /**
     * 执行单次连接尝试，较长的超时
     *
     * @param session JavaMail Session对象
     * @param timeout 超时时间(秒)
     * @return 成功连接的Store对象
     * @throws MessagingException 连接失败时抛出异常
     */
    private Store attemptConnection(Session session, int timeout) throws MessagingException {
        final Store store = session.getStore(mailConfig.getProtocol());
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<MessagingException> exceptionRef = new AtomicReference<>();

        Thread connectThread = new Thread(() -> {
            try {
                store.connect(mailConfig.getServer(), mailConfig.getUsername(), mailConfig.getPassword());
                log.debug("邮件服务器连接成功");
                if (store instanceof IMAPStore) {
                    try {
                        // 构建 IMAP ID 信息
                        Map<String, String> id = new HashMap<>();
                        id.put("name", "easyMail");            // 客户端名称
                        id.put("version", "1.0.0");                 // 版本号
                        id.put("vendor", mailConfig.getUsername());               // 开发者/公司
                        id.put("support-email", mailConfig.getUsername()); // 支持邮箱

                        ((IMAPStore) store).id(id);
                        log.debug("IMAP ID 已发送");
                    } catch (Exception e) {
                        log.warn("发送 IMAP ID 失败: {}", e.getMessage());
                    }
                }

            } catch (MessagingException e) {
                exceptionRef.set(e);
            } finally {
                latch.countDown();
            }
        });

        connectThread.start();

        try {
            boolean completed = latch.await(timeout, TimeUnit.SECONDS);

            if (!completed) {
                connectThread.interrupt();
                connectThread.join(1000);
                throw new MessagingException("连接超时，超过" + timeout + "秒未响应");
            }

            MessagingException exception = exceptionRef.get();
            if (exception != null) {
                // 特别处理SSL握手错误
                if (isSSLHandshakeError(exception)) {
                    log.error("SSL握手失败: {}", exception.getMessage());
                    closeQuietly(store);
                }
                throw exception;
            }

            return store;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MessagingException("连接被中断");
        }
    }

    /**
     * 检查是否为SSL握手错误
     */
    private boolean isSSLHandshakeError(MessagingException e) {
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        Throwable cause = e.getCause();
        String causeMessage = cause != null && cause.getMessage() != null ?
                cause.getMessage().toLowerCase() : "";

        return message.contains("handshake") ||
                message.contains("ssl") ||
                causeMessage.contains("handshake") ||
                causeMessage.contains("ssl");
    }

    /**
     * 安静地关闭连接，不抛出异常
     */
    private void closeQuietly(Store store) {
        try {
            if (store != null && store.isConnected()) {
                store.close();
            }
        } catch (Exception e) {
            EasyMailException connectionEx = EasyMailExceptionHandler.wrapException(e, "关闭连接时出错");
            log.warn(connectionEx.getFullErrorMessage());
        }
    }

    /**
     * 打开收件箱
     *
     * @param store 邮件存储对象
     * @return 收件箱文件夹
     * @throws MessagingException 如果打开失败
     */
    public Folder openInbox(Store store) throws MessagingException {
        try {
            Folder folder = store.getFolder("INBOX");
            folder.open(Folder.READ_WRITE);
            log.debug("收件箱打开成功");
            return folder;
        } catch (MessagingException e) {
            EasyMailException connectionEx = EasyMailExceptionHandler.wrapMessagingException(e, "打开邮件文件夹失败");
            log.error(connectionEx.getFullErrorMessage(), connectionEx);
            throw connectionEx;
        }
    }

    /**
     * 关闭连接和资源
     *
     * @param folder 邮件文件夹
     * @param store  邮件存储对象
     */
    public void closeConnection(Folder folder, Store store) {
        try {
            if (folder != null && folder.isOpen()) {
                folder.close(false);
                log.debug("邮件文件夹已关闭");
            }

            if (store != null && store.isConnected()) {
                store.close();
                log.debug("邮件连接已关闭");
            }
        } catch (Exception e) {
            EasyMailException connectionEx = EasyMailExceptionHandler.wrapException(e, "重新连接失败");
            log.error(connectionEx.getFullErrorMessage(), connectionEx);
        }
    }

    /**
     * 重新连接邮件服务器（如果需要）
     *
     * @param store  邮件存储对象
     * @param folder 邮件文件夹对象
     * @return 重新连接后的文件夹对象
     * @throws MessagingException 如果重连失败
     */
    public Folder reconnectIfNeeded(Store store, Folder folder) throws MessagingException {
        if (store == null || !store.isConnected()) {
            log.debug("尝试重新连接到邮件服务器");
            assert store != null;
            store.connect(mailConfig.getServer(), mailConfig.getUsername(), mailConfig.getPassword());
            log.debug("邮件服务器重新连接成功");
        }

        if (folder == null || !folder.isOpen()) {
            log.debug("尝试重新打开收件箱");
            folder = store.getFolder("INBOX");
            folder.open(Folder.READ_WRITE);
            log.debug("收件箱重新打开成功");
        }
        
        return folder;
    }
}