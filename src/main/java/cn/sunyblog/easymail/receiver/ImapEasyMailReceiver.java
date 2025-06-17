package cn.sunyblog.easymail.receiver;

import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.*;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * IMAP邮件接收器实现
 * 提供完整的IMAP协议邮件接收功能
 * <p>
 * 功能特性：
 * - IMAP连接管理
 * - 邮件搜索和过滤
 * - 邮件内容解析
 * - 附件处理
 * - 实时邮件监听
 * - 连接池管理
 * - 自动重连机制
 *
 * @author suny
 * @version 1.0.0
 */
@Component
public class ImapEasyMailReceiver implements EasyMailReceiver {

    private static final Logger logger = LoggerFactory.getLogger(ImapEasyMailReceiver.class);
    private static final String RECEIVER_NAME = "imap";

    private Store store;
    private Folder currentFolder;
    private EmailReceiveConfig config;
    private EmailReceiverStats stats;
    private volatile boolean listening = false;
    private ExecutorService listenerExecutor;
    private ScheduledExecutorService scheduledExecutor;
    private EmailListener emailListener;
    private EmailListenConfig listenConfig;

    public ImapEasyMailReceiver() {
        this.stats = new EmailReceiverStats();
        this.stats.setReceiverName(RECEIVER_NAME);
        this.listenerExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "EmailListener-" + RECEIVER_NAME);
            t.setDaemon(true);
            return t;
        });
        this.scheduledExecutor = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "EmailScheduler-" + RECEIVER_NAME);
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public String getReceiverName() {
        return RECEIVER_NAME;
    }

    @Override
    public boolean supports(String protocol) {
        return "imap".equalsIgnoreCase(protocol) || "imaps".equalsIgnoreCase(protocol);
    }

    @Override
    public ConnectionResult connect(EmailReceiveConfig config) throws EmailReceiveException {
        this.config = config;

        try {
            Properties props = createProperties(config);
            Session session = Session.getInstance(props);

            store = session.getStore(config.getProtocol());
            store.connect(config.getHost(), config.getPort(), config.getUsername(), config.getPassword());

            stats.incrementConnections();

            Map<String, Object> serverInfo = new HashMap<>();
            serverInfo.put("protocol", config.getProtocol());
            serverInfo.put("host", config.getHost());
            serverInfo.put("port", config.getPort());
            serverInfo.put("sslEnabled", config.isSslEnabled());

            logger.info("成功连接到IMAP服务器: {}:{}", config.getHost(), config.getPort());
            return ConnectionResult.success("连接成功", serverInfo);

        } catch (Exception e) {
            stats.incrementErrors();
            logger.error("连接IMAP服务器失败: {}", e.getMessage(), e);
            throw new EmailReceiveException("连接失败: " + e.getMessage(), e, RECEIVER_NAME, "connect");
        }
    }

    @Override
    public void disconnect() throws EmailReceiveException {
        try {
            stopListening();

            if (currentFolder != null && currentFolder.isOpen()) {
                currentFolder.close(false);
            }

            if (store != null && store.isConnected()) {
                store.close();
            }

            logger.info("已断开IMAP连接");
        } catch (Exception e) {
            logger.error("断开IMAP连接失败: {}", e.getMessage(), e);
            throw new EmailReceiveException("断开连接失败: " + e.getMessage(), e, RECEIVER_NAME, "disconnect");
        }
    }

    @Override
    public boolean isConnected() {
        return store != null && store.isConnected();
    }

    @Override
    public List<EmailMessage> getEmails(EmailSearchCriteria criteria) throws EmailReceiveException {
        try {
            Folder folder = openFolder(criteria.getFolderName());

            SearchTerm searchTerm = buildSearchTerm(criteria);
            Message[] messages;

            if (searchTerm != null) {
                messages = folder.search(searchTerm);
            } else {
                messages = folder.getMessages();
            }

            // 排序
            Arrays.sort(messages, createComparator(criteria));

            // 分页
            int start = criteria.getOffset();
            int end = Math.min(start + criteria.getMaxResults(), messages.length);

            List<EmailMessage> result = new ArrayList<>();
            for (int i = start; i < end; i++) {
                EmailMessage emailMessage = convertToEmailMessage(messages[i]);
                result.add(emailMessage);
            }

            stats.setTotalEmailsReceived(stats.getTotalEmailsReceived() + result.size());
            return result;

        } catch (Exception e) {
            stats.incrementErrors();
            logger.error("获取邮件列表失败: {}", e.getMessage(), e);
            throw new EmailReceiveException("获取邮件失败: " + e.getMessage(), e, RECEIVER_NAME, "getEmails");
        }
    }

    @Override
    public CompletableFuture<List<EmailMessage>> getEmailsAsync(EmailSearchCriteria criteria) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getEmails(criteria);
            } catch (EmailReceiveException e) {
                throw new RuntimeException(e);
            }
        }, listenerExecutor);
    }

    @Override
    public Optional<EmailMessage> getEmail(String messageId) throws EmailReceiveException {
        try {
            Folder folder = openFolder(config.getDefaultFolder());

            SearchTerm searchTerm = new MessageIDTerm(messageId);
            Message[] messages = folder.search(searchTerm);

            if (messages.length > 0) {
                EmailMessage emailMessage = convertToEmailMessage(messages[0]);
                return Optional.of(emailMessage);
            }

            return Optional.empty();

        } catch (Exception e) {
            stats.incrementErrors();
            logger.error("获取邮件详情失败: {}", e.getMessage(), e);
            throw new EmailReceiveException("获取邮件详情失败: " + e.getMessage(), e, RECEIVER_NAME, "getEmail");
        }
    }

    @Override
    public boolean markAsRead(String messageId) throws EmailReceiveException {
        return setFlag(messageId, Flags.Flag.SEEN, true);
    }

    @Override
    public boolean markAsUnread(String messageId) throws EmailReceiveException {
        return setFlag(messageId, Flags.Flag.SEEN, false);
    }

    @Override
    public boolean deleteEmail(String messageId) throws EmailReceiveException {
        return setFlag(messageId, Flags.Flag.DELETED, true);
    }

    @Override
    public boolean moveToFolder(String messageId, String folderName) throws EmailReceiveException {
        try {
            Folder sourceFolder = openFolder(config.getDefaultFolder());
            Folder targetFolder = store.getFolder(folderName);

            if (!targetFolder.exists()) {
                throw new EmailReceiveException("目标文件夹不存在: " + folderName, RECEIVER_NAME, "moveToFolder");
            }

            SearchTerm searchTerm = new MessageIDTerm(messageId);
            Message[] messages = sourceFolder.search(searchTerm);

            if (messages.length > 0) {
                sourceFolder.copyMessages(messages, targetFolder);
                messages[0].setFlag(Flags.Flag.DELETED, true);
                sourceFolder.expunge();
                return true;
            }

            return false;

        } catch (Exception e) {
            stats.incrementErrors();
            logger.error("移动邮件失败: {}", e.getMessage(), e);
            throw new EmailReceiveException("移动邮件失败: " + e.getMessage(), e, RECEIVER_NAME, "moveToFolder");
        }
    }

    @Override
    public List<EmailFolder> getFolders() throws EmailReceiveException {
        try {
            Folder[] folders = store.getDefaultFolder().list("*");
            List<EmailFolder> result = new ArrayList<>();

            for (Folder folder : folders) {
                EmailFolder emailFolder = convertToEmailFolder(folder);
                result.add(emailFolder);
            }

            return result;

        } catch (Exception e) {
            stats.incrementErrors();
            logger.error("获取文件夹列表失败: {}", e.getMessage(), e);
            throw new EmailReceiveException("获取文件夹列表失败: " + e.getMessage(), e, RECEIVER_NAME, "getFolders");
        }
    }

    @Override
    public int getUnreadCount(String folderName) throws EmailReceiveException {
        try {
            Folder folder = openFolder(folderName != null ? folderName : config.getDefaultFolder());
            return folder.getUnreadMessageCount();
        } catch (Exception e) {
            stats.incrementErrors();
            logger.error("获取未读邮件数量失败: {}", e.getMessage(), e);
            throw new EmailReceiveException("获取未读邮件数量失败: " + e.getMessage(), e, RECEIVER_NAME, "getUnreadCount");
        }
    }

    @Override
    public void startListening(EmailListener listener, EmailListenConfig config) {
        if (listening) {
            logger.warn("邮件监听已经在运行中");
            return;
        }

        this.emailListener = listener;
        this.listenConfig = config;
        this.listening = true;
        this.stats.setListening(true);

        listenerExecutor.submit(this::listenForEmails);
        logger.info("开始监听邮件，文件夹: {}, 检查间隔: {}ms", config.getFolderName(), config.getCheckInterval());
    }

    @Override
    public void stopListening() {
        if (!listening) {
            return;
        }

        listening = false;
        stats.setListening(false);

        if (listenerExecutor != null && !listenerExecutor.isShutdown()) {
            listenerExecutor.shutdownNow();
            try {
                if (!listenerExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.warn("邮件监听器未能在5秒内正常关闭");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        logger.info("已停止邮件监听");
    }

    @Override
    public EmailReceiverStats getStats() {
        return stats;
    }

    /**
     * 创建连接属性
     */
    private Properties createProperties(EmailReceiveConfig config) {
        Properties props = new Properties();

        String protocol = config.getProtocol();
        props.setProperty("mail.store.protocol", protocol);
        props.setProperty("mail." + protocol + ".host", config.getHost());
        props.setProperty("mail." + protocol + ".port", String.valueOf(config.getPort()));

        if (config.isSslEnabled()) {
            props.setProperty("mail." + protocol + ".ssl.enable", "true");
        }

        if (config.isTlsEnabled()) {
            props.setProperty("mail." + protocol + ".starttls.enable", "true");
        }

        props.setProperty("mail." + protocol + ".connectiontimeout", String.valueOf(config.getConnectionTimeout()));
        props.setProperty("mail." + protocol + ".timeout", String.valueOf(config.getReadTimeout()));

        // 添加自定义属性
        config.getCustomProperties().forEach((key, value) -> props.setProperty(key, value.toString()));

        return props;
    }

    /**
     * 打开文件夹
     */
    private Folder openFolder(String folderName) throws MessagingException {
        if (currentFolder != null && currentFolder.isOpen() &&
                currentFolder.getFullName().equals(folderName)) {
            return currentFolder;
        }

        if (currentFolder != null && currentFolder.isOpen()) {
            currentFolder.close(false);
        }

        currentFolder = store.getFolder(folderName);
        currentFolder.open(Folder.READ_WRITE);
        return currentFolder;
    }

    /**
     * 构建搜索条件
     */
    private SearchTerm buildSearchTerm(EmailSearchCriteria criteria) {
        List<SearchTerm> terms = new ArrayList<>();

        if (criteria.isUnreadOnly()) {
            terms.add(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
        }

        if (criteria.getFromDate() != null) {
            Date fromDate = Date.from(criteria.getFromDate().atZone(ZoneId.systemDefault()).toInstant());
            terms.add(new SentDateTerm(ComparisonTerm.GE, fromDate));
        }

        if (criteria.getToDate() != null) {
            Date toDate = Date.from(criteria.getToDate().atZone(ZoneId.systemDefault()).toInstant());
            terms.add(new SentDateTerm(ComparisonTerm.LE, toDate));
        }

        if (criteria.getFromAddress() != null && !criteria.getFromAddress().isEmpty()) {
            terms.add(new FromStringTerm(criteria.getFromAddress()));
        }

        if (criteria.getToAddress() != null && !criteria.getToAddress().isEmpty()) {
            terms.add(new RecipientStringTerm(Message.RecipientType.TO, criteria.getToAddress()));
        }

        if (criteria.getSubject() != null && !criteria.getSubject().isEmpty()) {
            terms.add(new SubjectTerm(criteria.getSubject()));
        }

        if (criteria.getContent() != null && !criteria.getContent().isEmpty()) {
            terms.add(new BodyTerm(criteria.getContent()));
        }

        if (terms.isEmpty()) {
            return null;
        }

        if (terms.size() == 1) {
            return terms.get(0);
        }

        SearchTerm result = terms.get(0);
        for (int i = 1; i < terms.size(); i++) {
            result = new AndTerm(result, terms.get(i));
        }

        return result;
    }

    /**
     * 创建比较器
     */
    private Comparator<Message> createComparator(EmailSearchCriteria criteria) {
        return (m1, m2) -> {
            try {
                int result;

                switch (criteria.getSortBy().toLowerCase()) {
                    case "date":
                        Date d1 = m1.getSentDate() != null ? m1.getSentDate() : m1.getReceivedDate();
                        Date d2 = m2.getSentDate() != null ? m2.getSentDate() : m2.getReceivedDate();
                        result = d1.compareTo(d2);
                        break;
                    case "subject":
                        result = m1.getSubject().compareTo(m2.getSubject());
                        break;
                    case "from":
                        result = m1.getFrom()[0].toString().compareTo(m2.getFrom()[0].toString());
                        break;
                    default:
                        result = m1.getMessageNumber() - m2.getMessageNumber();
                }

                return criteria.isAscending() ? result : -result;
            } catch (MessagingException e) {
                return 0;
            }
        };
    }

    /**
     * 转换为EmailMessage
     */
    private EmailMessage convertToEmailMessage(Message message) throws MessagingException, IOException {
        EmailMessage emailMessage = new EmailMessage();

        // 基本信息
        emailMessage.setMessageId(getMessageId(message));
        emailMessage.setSubject(message.getSubject());
        emailMessage.setFrom(getAddressString(message.getFrom()));
        emailMessage.setTo(getAddressList(message.getRecipients(Message.RecipientType.TO)));
        emailMessage.setCc(getAddressList(message.getRecipients(Message.RecipientType.CC)));
        emailMessage.setBcc(getAddressList(message.getRecipients(Message.RecipientType.BCC)));

        // 日期
        if (message.getSentDate() != null) {
            emailMessage.setSentDate(LocalDateTime.ofInstant(message.getSentDate().toInstant(), ZoneId.systemDefault()));
        }
        if (message.getReceivedDate() != null) {
            emailMessage.setReceivedDate(LocalDateTime.ofInstant(message.getReceivedDate().toInstant(), ZoneId.systemDefault()));
        }

        // 标志
        emailMessage.setRead(message.isSet(Flags.Flag.SEEN));
        emailMessage.setFlagged(message.isSet(Flags.Flag.FLAGGED));

        // 内容和附件
        parseContent(message, emailMessage);

        // 文件夹
        emailMessage.setFolderName(message.getFolder().getFullName());

        return emailMessage;
    }

    /**
     * 解析邮件内容
     */
    private void parseContent(Message message, EmailMessage emailMessage) throws MessagingException, IOException {
        Object content = message.getContent();

        if (content instanceof String) {
            if (message.isMimeType("text/html")) {
                emailMessage.setHtmlContent((String) content);
            } else {
                emailMessage.setTextContent((String) content);
            }
        } else if (content instanceof MimeMultipart) {
            parseMultipart((MimeMultipart) content, emailMessage);
        }
    }

    /**
     * 解析多部分内容
     */
    private void parseMultipart(MimeMultipart multipart, EmailMessage emailMessage) throws MessagingException, IOException {
        int count = multipart.getCount();

        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);

            if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) ||
                    (bodyPart.getFileName() != null && !bodyPart.getFileName().isEmpty())) {
                // 附件
                EmailAttachment attachment = parseAttachment(bodyPart);
                emailMessage.getAttachments().add(attachment);
            } else if (bodyPart.isMimeType("text/plain")) {
                // 纯文本内容
                emailMessage.setTextContent((String) bodyPart.getContent());
            } else if (bodyPart.isMimeType("text/html")) {
                // HTML内容
                emailMessage.setHtmlContent((String) bodyPart.getContent());
            } else if (bodyPart.isMimeType("multipart/*")) {
                // 嵌套多部分
                parseMultipart((MimeMultipart) bodyPart.getContent(), emailMessage);
            }
        }
    }

    /**
     * 解析附件
     */
    private EmailAttachment parseAttachment(BodyPart bodyPart) throws MessagingException, IOException {
        EmailAttachment attachment = new EmailAttachment();

        attachment.setFileName(bodyPart.getFileName());
        attachment.setContentType(bodyPart.getContentType());
        attachment.setSize(bodyPart.getSize());

        // 读取附件内容
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bodyPart.getDataHandler().writeTo(baos);
        attachment.setContent(baos.toByteArray());

        // 检查是否为内联附件
        String[] contentId = bodyPart.getHeader("Content-ID");
        if (contentId != null && contentId.length > 0) {
            attachment.setContentId(contentId[0]);
            attachment.setInline(true);
        }

        return attachment;
    }

    /**
     * 转换为EmailFolder
     */
    private EmailFolder convertToEmailFolder(Folder folder) throws MessagingException {
        EmailFolder emailFolder = new EmailFolder();

        emailFolder.setName(folder.getName());
        emailFolder.setFullName(folder.getFullName());
        emailFolder.setCanHoldMessages((folder.getType() & Folder.HOLDS_MESSAGES) != 0);
        emailFolder.setCanHoldFolders((folder.getType() & Folder.HOLDS_FOLDERS) != 0);

        if (emailFolder.isCanHoldMessages()) {
            try {
                folder.open(Folder.READ_ONLY);
                emailFolder.setMessageCount(folder.getMessageCount());
                emailFolder.setUnreadCount(folder.getUnreadMessageCount());
                folder.close(false);
            } catch (MessagingException e) {
                logger.warn("无法获取文件夹统计信息: {}", folder.getFullName());
            }
        }

        return emailFolder;
    }

    /**
     * 设置邮件标志
     */
    private boolean setFlag(String messageId, Flags.Flag flag, boolean value) throws EmailReceiveException {
        try {
            Folder folder = openFolder(config.getDefaultFolder());

            SearchTerm searchTerm = new MessageIDTerm(messageId);
            Message[] messages = folder.search(searchTerm);

            if (messages.length > 0) {
                messages[0].setFlag(flag, value);
                return true;
            }

            return false;

        } catch (Exception e) {
            stats.incrementErrors();
            logger.error("设置邮件标志失败: {}", e.getMessage(), e);
            throw new EmailReceiveException("设置邮件标志失败: " + e.getMessage(), e, RECEIVER_NAME, "setFlag");
        }
    }

    /**
     * 监听邮件
     */
    private void listenForEmails() {
        int reconnectAttempts = 0;

        while (listening) {
            try {
                if (!isConnected()) {
                    if (listenConfig.isAutoReconnect() && reconnectAttempts < listenConfig.getMaxReconnectAttempts()) {
                        logger.info("尝试重新连接... (第{}次)", reconnectAttempts + 1);
                        connect(config);
                        stats.incrementReconnections();
                        reconnectAttempts++;
                    } else {
                        logger.error("连接丢失，停止监听");
                        emailListener.onConnectionLost(new Exception("连接丢失"));
                        break;
                    }
                }

                checkForNewEmails();
                reconnectAttempts = 0; // 重置重连计数

                Thread.sleep(listenConfig.getCheckInterval());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("邮件监听错误: {}", e.getMessage(), e);
                emailListener.onListenError(e);

                try {
                    Thread.sleep(listenConfig.getReconnectDelay());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /**
     * 检查新邮件
     */
    private void checkForNewEmails() throws EmailReceiveException {
        EmailSearchCriteria criteria = new EmailSearchCriteria()
                .folder(listenConfig.getFolderName())
                .unreadOnly(true)
                .limit(10);

        List<EmailMessage> newEmails = getEmails(criteria);

        for (EmailMessage email : newEmails) {
            try {
                emailListener.onNewEmail(email);
                stats.incrementEmailsProcessed();
            } catch (Exception e) {
                logger.error("处理新邮件时发生错误: {}", e.getMessage(), e);
                emailListener.onListenError(e);
            }
        }
    }

    /**
     * 获取消息ID
     */
    private String getMessageId(Message message) throws MessagingException {
        if (message instanceof MimeMessage) {
            return ((MimeMessage) message).getMessageID();
        }
        return String.valueOf(message.getMessageNumber());
    }

    /**
     * 获取地址字符串
     */
    private String getAddressString(Address[] addresses) {
        if (addresses == null || addresses.length == 0) {
            return null;
        }
        return addresses[0].toString();
    }

    /**
     * 获取地址列表
     */
    private List<String> getAddressList(Address[] addresses) {
        if (addresses == null) {
            return new ArrayList<>();
        }
        return Arrays.stream(addresses)
                .map(Address::toString)
                .collect(Collectors.toList());
    }
}