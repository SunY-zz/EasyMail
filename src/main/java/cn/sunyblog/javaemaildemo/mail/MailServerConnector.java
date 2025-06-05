package cn.sunyblog.javaemaildemo.mail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import java.util.Properties;

/**
 * @author suny
 * @version 1.0
 * @description: 邮件服务器连接管理   负责连接和断开邮件服务器
 * @date 2025/05/12 16:24
 */
@Slf4j
@Component
public class MailServerConnector {

    @Resource
    private MailConfig mailConfig;


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
        // 修改IDLE模式支持
        props.setProperty("mail.imap.usesocketchannels", "true");
        props.setProperty("mail.imap.enableimapevents", "true");
        props.setProperty("mail.event.scope", "session");
        // 添加以下配置
        props.setProperty("mail.imap.folderopen.timeout", "5000");
        props.setProperty("mail.imap.fetchsize", "1048576");
        props.setProperty("mail.imap.peek", "true");
        props.setProperty("mail.imap.connectionpool.debug", "true");
        
        // 删除这一行错误配置
        // props.setProperty("mail.event.executor", "java.util.concurrent.Executors$DelegatedExecutorService");
        
        // 如果需要设置executor，应该使用put方法并传入实际的Executor实例
        // 例如：props.put("mail.event.executor", Executors.newFixedThreadPool(2));
        // 但在大多数情况下，可以让JavaMail使用默认的executor
        
        // 添加SSL证书信任设置
        props.setProperty("mail.imap.ssl.trust", "*.139.com");
        props.setProperty("mail.imap.ssl.checkserveridentity", "false");

        props.setProperty("mail.imaps.auth.login.disable", "false");
        props.setProperty("mail.imaps.auth.plain.disable", "true"); // 禁用PLAIN
        // 禁用所有SSL验证
        props.setProperty("mail.imap.ssl.enable", "true");
        props.setProperty("mail.imap.starttls.enable", "true");
        props.setProperty("mail.imap.starttls.required", "false");
        // 使用自定义的SSL工厂
        props.put("mail.imap.ssl.socketFactory", new SSLTrustUtil.TrustAllSSLSocketFactory());


        return props;
    }

    /**
     * 连接到邮件服务器
     *
     * @param session 邮件会话
     * @return 邮件存储对象
     * @throws MessagingException 如果连接失败
     */
    public Store connectToServer(Session session) throws MessagingException {
        log.info("正在连接邮件服务器: {}", mailConfig.getServer());
        Store store = session.getStore(mailConfig.getProtocol());
        store.connect(mailConfig.getServer(), mailConfig.getUsername(), mailConfig.getPassword());
        log.info("邮件服务器连接成功");
        return store;
    }

    /**
     * 打开收件箱
     *
     * @param store 邮件存储对象
     * @return 收件箱文件夹
     * @throws MessagingException 如果打开失败
     */
    public Folder openInbox(Store store) throws MessagingException {
        Folder folder = store.getFolder("INBOX");
        folder.open(Folder.READ_WRITE);
        log.info("收件箱打开成功");
        return folder;
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
                log.info("邮件文件夹已关闭");
            }

            if (store != null && store.isConnected()) {
                store.close();
                log.info("邮件连接已关闭");
            }
        } catch (Exception e) {
            log.error("关闭邮件资源异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 重新连接邮件服务器（如果需要）
     *
     * @param store  邮件存储对象
     * @param folder 邮件文件夹
     * @throws MessagingException 如果重连失败
     */
    public void reconnectIfNeeded(Store store, Folder folder) throws MessagingException {
        if (store == null || !store.isConnected()) {
            log.info("尝试重新连接到邮件服务器");
            store.connect(mailConfig.getServer(), mailConfig.getUsername(), mailConfig.getPassword());
            log.info("邮件服务器重新连接成功");
        }

        if (folder == null || !folder.isOpen()) {
            log.info("尝试重新打开收件箱");
            folder = store.getFolder("INBOX");
            folder.open(Folder.READ_WRITE);
            log.info("收件箱重新打开成功");
        }
    }
}