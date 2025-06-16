package cn.sunyblog.javaemaildemo.receiver;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * 邮件接收器接口
 * 提供可扩展的邮件接收、解析和处理功能
 * 
 * 支持的协议：
 * - IMAP
 * - POP3
 * - Exchange Web Services (EWS)
 * - Microsoft Graph API
 * 
 * @author JavaEmailSpringBoot
 * @version 1.0.0
 */
public interface EmailReceiver {
    
    /**
     * 获取接收器名称
     * 
     * @return 接收器名称
     */
    String getReceiverName();
    
    /**
     * 检查是否支持指定的协议
     * 
     * @param protocol 协议类型（如：imap, pop3, ews等）
     * @return 是否支持
     */
    boolean supports(String protocol);
    
    /**
     * 连接到邮件服务器
     * 
     * @param config 连接配置
     * @return 连接结果
     * @throws EmailReceiveException 连接异常
     */
    ConnectionResult connect(EmailReceiveConfig config) throws EmailReceiveException;
    
    /**
     * 断开连接
     * 
     * @throws EmailReceiveException 断开连接异常
     */
    void disconnect() throws EmailReceiveException;
    
    /**
     * 检查连接状态
     * 
     * @return 是否已连接
     */
    boolean isConnected();
    
    /**
     * 获取邮件列表
     * 
     * @param criteria 查询条件
     * @return 邮件列表
     * @throws EmailReceiveException 获取异常
     */
    List<EmailMessage> getEmails(EmailSearchCriteria criteria) throws EmailReceiveException;
    
    /**
     * 异步获取邮件列表
     * 
     * @param criteria 查询条件
     * @return 邮件列表的Future
     */
    CompletableFuture<List<EmailMessage>> getEmailsAsync(EmailSearchCriteria criteria);
    
    /**
     * 获取单个邮件详情
     * 
     * @param messageId 邮件ID
     * @return 邮件详情
     * @throws EmailReceiveException 获取异常
     */
    Optional<EmailMessage> getEmail(String messageId) throws EmailReceiveException;
    
    /**
     * 标记邮件为已读
     * 
     * @param messageId 邮件ID
     * @return 操作结果
     * @throws EmailReceiveException 操作异常
     */
    boolean markAsRead(String messageId) throws EmailReceiveException;
    
    /**
     * 标记邮件为未读
     * 
     * @param messageId 邮件ID
     * @return 操作结果
     * @throws EmailReceiveException 操作异常
     */
    boolean markAsUnread(String messageId) throws EmailReceiveException;
    
    /**
     * 删除邮件
     * 
     * @param messageId 邮件ID
     * @return 操作结果
     * @throws EmailReceiveException 操作异常
     */
    boolean deleteEmail(String messageId) throws EmailReceiveException;
    
    /**
     * 移动邮件到指定文件夹
     * 
     * @param messageId 邮件ID
     * @param folderName 目标文件夹名称
     * @return 操作结果
     * @throws EmailReceiveException 操作异常
     */
    boolean moveToFolder(String messageId, String folderName) throws EmailReceiveException;
    
    /**
     * 获取文件夹列表
     * 
     * @return 文件夹列表
     * @throws EmailReceiveException 获取异常
     */
    List<EmailFolder> getFolders() throws EmailReceiveException;
    
    /**
     * 获取未读邮件数量
     * 
     * @param folderName 文件夹名称，null表示默认收件箱
     * @return 未读邮件数量
     * @throws EmailReceiveException 获取异常
     */
    int getUnreadCount(String folderName) throws EmailReceiveException;
    
    /**
     * 监听新邮件
     * 
     * @param listener 邮件监听器
     * @param config 监听配置
     * @throws EmailReceiveException 监听异常
     */
    void startListening(EmailListener listener, EmailListenConfig config) throws EmailReceiveException;
    
    /**
     * 停止监听
     * 
     * @throws EmailReceiveException 停止异常
     */
    void stopListening() throws EmailReceiveException;
    
    /**
     * 获取接收器统计信息
     * 
     * @return 统计信息
     */
    EmailReceiverStats getStats();
    
    /**
     * 邮件接收异常
     */
    class EmailReceiveException extends Exception {
        private final String receiverName;
        private final String operation;
        private final Map<String, Object> context;
        
        public EmailReceiveException(String message, String receiverName, String operation) {
            super(message);
            this.receiverName = receiverName;
            this.operation = operation;
            this.context = new java.util.HashMap<>();
        }
        
        public EmailReceiveException(String message, Throwable cause, String receiverName, String operation) {
            super(message, cause);
            this.receiverName = receiverName;
            this.operation = operation;
            this.context = new java.util.HashMap<>();
        }
        
        public String getReceiverName() { return receiverName; }
        public String getOperation() { return operation; }
        public Map<String, Object> getContext() { return context; }
        
        public EmailReceiveException withContext(String key, Object value) {
            this.context.put(key, value);
            return this;
        }
    }
    
    /**
     * 连接结果
     */
    class ConnectionResult {
        private final boolean success;
        private final String message;
        private final Map<String, Object> serverInfo;
        private final LocalDateTime connectedAt;
        
        public ConnectionResult(boolean success, String message, Map<String, Object> serverInfo) {
            this.success = success;
            this.message = message;
            this.serverInfo = serverInfo != null ? serverInfo : new java.util.HashMap<>();
            this.connectedAt = LocalDateTime.now();
        }
        
        public static ConnectionResult success(String message, Map<String, Object> serverInfo) {
            return new ConnectionResult(true, message, serverInfo);
        }
        
        public static ConnectionResult failure(String message) {
            return new ConnectionResult(false, message, null);
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Map<String, Object> getServerInfo() { return serverInfo; }
        public LocalDateTime getConnectedAt() { return connectedAt; }
    }
    
    /**
     * 邮件接收配置
     */
    class EmailReceiveConfig {
        private String protocol;
        private String host;
        private int port;
        private String username;
        private String password;
        private boolean sslEnabled;
        private boolean tlsEnabled;
        private int connectionTimeout;
        private int readTimeout;
        private String defaultFolder;
        private Map<String, Object> customProperties;
        
        public EmailReceiveConfig() {
            this.protocol = "imap";
            this.port = 993;
            this.sslEnabled = true;
            this.tlsEnabled = false;
            this.connectionTimeout = 30000;
            this.readTimeout = 60000;
            this.defaultFolder = "INBOX";
            this.customProperties = new java.util.HashMap<>();
        }
        
        // Getters and Setters
        public String getProtocol() { return protocol; }
        public void setProtocol(String protocol) { this.protocol = protocol; }
        
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        
        public boolean isSslEnabled() { return sslEnabled; }
        public void setSslEnabled(boolean sslEnabled) { this.sslEnabled = sslEnabled; }
        
        public boolean isTlsEnabled() { return tlsEnabled; }
        public void setTlsEnabled(boolean tlsEnabled) { this.tlsEnabled = tlsEnabled; }
        
        public int getConnectionTimeout() { return connectionTimeout; }
        public void setConnectionTimeout(int connectionTimeout) { this.connectionTimeout = connectionTimeout; }
        
        public int getReadTimeout() { return readTimeout; }
        public void setReadTimeout(int readTimeout) { this.readTimeout = readTimeout; }
        
        public String getDefaultFolder() { return defaultFolder; }
        public void setDefaultFolder(String defaultFolder) { this.defaultFolder = defaultFolder; }
        
        public Map<String, Object> getCustomProperties() { return customProperties; }
        public void setCustomProperties(Map<String, Object> customProperties) { this.customProperties = customProperties; }
    }
    
    /**
     * 邮件搜索条件
     */
    class EmailSearchCriteria {
        private String folderName;
        private boolean unreadOnly;
        private LocalDateTime fromDate;
        private LocalDateTime toDate;
        private String fromAddress;
        private String toAddress;
        private String subject;
        private String content;
        private List<String> flags;
        private int maxResults;
        private int offset;
        private String sortBy;
        private boolean ascending;
        
        public EmailSearchCriteria() {
            this.folderName = "INBOX";
            this.unreadOnly = false;
            this.maxResults = 50;
            this.offset = 0;
            this.sortBy = "date";
            this.ascending = false;
            this.flags = new java.util.ArrayList<>();
        }
        
        // Builder pattern methods
        public EmailSearchCriteria folder(String folderName) {
            this.folderName = folderName;
            return this;
        }
        
        public EmailSearchCriteria unreadOnly(boolean unreadOnly) {
            this.unreadOnly = unreadOnly;
            return this;
        }
        
        public EmailSearchCriteria dateRange(LocalDateTime fromDate, LocalDateTime toDate) {
            this.fromDate = fromDate;
            this.toDate = toDate;
            return this;
        }
        
        public EmailSearchCriteria from(String fromAddress) {
            this.fromAddress = fromAddress;
            return this;
        }
        
        public EmailSearchCriteria to(String toAddress) {
            this.toAddress = toAddress;
            return this;
        }
        
        public EmailSearchCriteria subject(String subject) {
            this.subject = subject;
            return this;
        }
        
        public EmailSearchCriteria content(String content) {
            this.content = content;
            return this;
        }
        
        public EmailSearchCriteria limit(int maxResults) {
            this.maxResults = maxResults;
            return this;
        }
        
        public EmailSearchCriteria offset(int offset) {
            this.offset = offset;
            return this;
        }
        
        public EmailSearchCriteria sortBy(String sortBy, boolean ascending) {
            this.sortBy = sortBy;
            this.ascending = ascending;
            return this;
        }
        
        // Getters
        public String getFolderName() { return folderName; }
        public boolean isUnreadOnly() { return unreadOnly; }
        public LocalDateTime getFromDate() { return fromDate; }
        public LocalDateTime getToDate() { return toDate; }
        public String getFromAddress() { return fromAddress; }
        public String getToAddress() { return toAddress; }
        public String getSubject() { return subject; }
        public String getContent() { return content; }
        public List<String> getFlags() { return flags; }
        public int getMaxResults() { return maxResults; }
        public int getOffset() { return offset; }
        public String getSortBy() { return sortBy; }
        public boolean isAscending() { return ascending; }
    }
    
    /**
     * 邮件消息
     */
    class EmailMessage {
        private String messageId;
        private String subject;
        private String from;
        private List<String> to;
        private List<String> cc;
        private List<String> bcc;
        private String textContent;
        private String htmlContent;
        private List<EmailAttachment> attachments;
        private LocalDateTime receivedDate;
        private LocalDateTime sentDate;
        private boolean read;
        private boolean flagged;
        private List<String> flags;
        private Map<String, String> headers;
        private String folderName;
        
        public EmailMessage() {
            this.to = new java.util.ArrayList<>();
            this.cc = new java.util.ArrayList<>();
            this.bcc = new java.util.ArrayList<>();
            this.attachments = new java.util.ArrayList<>();
            this.flags = new java.util.ArrayList<>();
            this.headers = new java.util.HashMap<>();
        }
        
        // Getters and Setters
        public String getMessageId() { return messageId; }
        public void setMessageId(String messageId) { this.messageId = messageId; }
        
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        
        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }
        
        public List<String> getTo() { return to; }
        public void setTo(List<String> to) { this.to = to; }
        
        public List<String> getCc() { return cc; }
        public void setCc(List<String> cc) { this.cc = cc; }
        
        public List<String> getBcc() { return bcc; }
        public void setBcc(List<String> bcc) { this.bcc = bcc; }
        
        public String getTextContent() { return textContent; }
        public void setTextContent(String textContent) { this.textContent = textContent; }
        
        public String getHtmlContent() { return htmlContent; }
        public void setHtmlContent(String htmlContent) { this.htmlContent = htmlContent; }
        
        public List<EmailAttachment> getAttachments() { return attachments; }
        public void setAttachments(List<EmailAttachment> attachments) { this.attachments = attachments; }
        
        public LocalDateTime getReceivedDate() { return receivedDate; }
        public void setReceivedDate(LocalDateTime receivedDate) { this.receivedDate = receivedDate; }
        
        public LocalDateTime getSentDate() { return sentDate; }
        public void setSentDate(LocalDateTime sentDate) { this.sentDate = sentDate; }
        
        public boolean isRead() { return read; }
        public void setRead(boolean read) { this.read = read; }
        
        public boolean isFlagged() { return flagged; }
        public void setFlagged(boolean flagged) { this.flagged = flagged; }
        
        public List<String> getFlags() { return flags; }
        public void setFlags(List<String> flags) { this.flags = flags; }
        
        public Map<String, String> getHeaders() { return headers; }
        public void setHeaders(Map<String, String> headers) { this.headers = headers; }
        
        public String getFolderName() { return folderName; }
        public void setFolderName(String folderName) { this.folderName = folderName; }
        
        public boolean hasAttachments() {
            return attachments != null && !attachments.isEmpty();
        }
    }
    
    /**
     * 邮件附件
     */
    class EmailAttachment {
        private String fileName;
        private String contentType;
        private long size;
        private byte[] content;
        private String contentId;
        private boolean inline;
        
        public EmailAttachment() {}
        
        public EmailAttachment(String fileName, String contentType, byte[] content) {
            this.fileName = fileName;
            this.contentType = contentType;
            this.content = content;
            this.size = content != null ? content.length : 0;
        }
        
        // Getters and Setters
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        
        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
        
        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }
        
        public byte[] getContent() { return content; }
        public void setContent(byte[] content) { 
            this.content = content;
            this.size = content != null ? content.length : 0;
        }
        
        public String getContentId() { return contentId; }
        public void setContentId(String contentId) { this.contentId = contentId; }
        
        public boolean isInline() { return inline; }
        public void setInline(boolean inline) { this.inline = inline; }
    }
    
    /**
     * 邮件文件夹
     */
    class EmailFolder {
        private String name;
        private String fullName;
        private int messageCount;
        private int unreadCount;
        private boolean canHoldMessages;
        private boolean canHoldFolders;
        private List<EmailFolder> subFolders;
        
        public EmailFolder() {
            this.subFolders = new java.util.ArrayList<>();
        }
        
        public EmailFolder(String name, String fullName) {
            this();
            this.name = name;
            this.fullName = fullName;
        }
        
        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        
        public int getMessageCount() { return messageCount; }
        public void setMessageCount(int messageCount) { this.messageCount = messageCount; }
        
        public int getUnreadCount() { return unreadCount; }
        public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }
        
        public boolean isCanHoldMessages() { return canHoldMessages; }
        public void setCanHoldMessages(boolean canHoldMessages) { this.canHoldMessages = canHoldMessages; }
        
        public boolean isCanHoldFolders() { return canHoldFolders; }
        public void setCanHoldFolders(boolean canHoldFolders) { this.canHoldFolders = canHoldFolders; }
        
        public List<EmailFolder> getSubFolders() { return subFolders; }
        public void setSubFolders(List<EmailFolder> subFolders) { this.subFolders = subFolders; }
    }
    
    /**
     * 邮件监听器
     */
    interface EmailListener {
        /**
         * 新邮件到达时触发
         * 
         * @param message 新邮件
         */
        void onNewEmail(EmailMessage message);
        
        /**
         * 邮件被删除时触发
         * 
         * @param messageId 被删除的邮件ID
         */
        void onEmailDeleted(String messageId);
        
        /**
         * 邮件状态变化时触发
         * 
         * @param messageId 邮件ID
         * @param oldFlags 旧标志
         * @param newFlags 新标志
         */
        void onEmailFlagsChanged(String messageId, List<String> oldFlags, List<String> newFlags);
        
        /**
         * 连接丢失时触发
         * 
         * @param exception 异常信息
         */
        void onConnectionLost(Exception exception);
        
        /**
         * 监听错误时触发
         * 
         * @param exception 异常信息
         */
        void onListenError(Exception exception);
    }
    
    /**
     * 邮件监听配置
     */
    class EmailListenConfig {
        private String folderName;
        private boolean includeSubFolders;
        private int checkInterval;
        private boolean autoReconnect;
        private int maxReconnectAttempts;
        private int reconnectDelay;
        private List<String> eventTypes;
        
        public EmailListenConfig() {
            this.folderName = "INBOX";
            this.includeSubFolders = false;
            this.checkInterval = 30000; // 30秒
            this.autoReconnect = true;
            this.maxReconnectAttempts = 5;
            this.reconnectDelay = 5000; // 5秒
            this.eventTypes = java.util.Arrays.asList("NEW", "DELETE", "FLAGS_CHANGED");
        }
        
        // Getters and Setters
        public String getFolderName() { return folderName; }
        public void setFolderName(String folderName) { this.folderName = folderName; }
        
        public boolean isIncludeSubFolders() { return includeSubFolders; }
        public void setIncludeSubFolders(boolean includeSubFolders) { this.includeSubFolders = includeSubFolders; }
        
        public int getCheckInterval() { return checkInterval; }
        public void setCheckInterval(int checkInterval) { this.checkInterval = checkInterval; }
        
        public boolean isAutoReconnect() { return autoReconnect; }
        public void setAutoReconnect(boolean autoReconnect) { this.autoReconnect = autoReconnect; }
        
        public int getMaxReconnectAttempts() { return maxReconnectAttempts; }
        public void setMaxReconnectAttempts(int maxReconnectAttempts) { this.maxReconnectAttempts = maxReconnectAttempts; }
        
        public int getReconnectDelay() { return reconnectDelay; }
        public void setReconnectDelay(int reconnectDelay) { this.reconnectDelay = reconnectDelay; }
        
        public List<String> getEventTypes() { return eventTypes; }
        public void setEventTypes(List<String> eventTypes) { this.eventTypes = eventTypes; }
    }
    
    /**
     * 邮件接收器统计信息
     */
    class EmailReceiverStats {
        private String receiverName;
        private LocalDateTime startTime;
        private long totalEmailsReceived;
        private long totalEmailsProcessed;
        private long totalErrors;
        private long connectionCount;
        private long reconnectionCount;
        private LocalDateTime lastActivityTime;
        private boolean isListening;
        private Map<String, Object> customStats;
        
        public EmailReceiverStats() {
            this.startTime = LocalDateTime.now();
            this.customStats = new java.util.HashMap<>();
        }
        
        // Getters and Setters
        public String getReceiverName() { return receiverName; }
        public void setReceiverName(String receiverName) { this.receiverName = receiverName; }
        
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        
        public long getTotalEmailsReceived() { return totalEmailsReceived; }
        public void setTotalEmailsReceived(long totalEmailsReceived) { this.totalEmailsReceived = totalEmailsReceived; }
        
        public long getTotalEmailsProcessed() { return totalEmailsProcessed; }
        public void setTotalEmailsProcessed(long totalEmailsProcessed) { this.totalEmailsProcessed = totalEmailsProcessed; }
        
        public long getTotalErrors() { return totalErrors; }
        public void setTotalErrors(long totalErrors) { this.totalErrors = totalErrors; }
        
        public long getConnectionCount() { return connectionCount; }
        public void setConnectionCount(long connectionCount) { this.connectionCount = connectionCount; }
        
        public long getReconnectionCount() { return reconnectionCount; }
        public void setReconnectionCount(long reconnectionCount) { this.reconnectionCount = reconnectionCount; }
        
        public LocalDateTime getLastActivityTime() { return lastActivityTime; }
        public void setLastActivityTime(LocalDateTime lastActivityTime) { this.lastActivityTime = lastActivityTime; }
        
        public boolean isListening() { return isListening; }
        public void setListening(boolean listening) { isListening = listening; }
        
        public Map<String, Object> getCustomStats() { return customStats; }
        public void setCustomStats(Map<String, Object> customStats) { this.customStats = customStats; }
        
        public void incrementEmailsReceived() {
            this.totalEmailsReceived++;
            this.lastActivityTime = LocalDateTime.now();
        }
        
        public void incrementEmailsProcessed() {
            this.totalEmailsProcessed++;
            this.lastActivityTime = LocalDateTime.now();
        }
        
        public void incrementErrors() {
            this.totalErrors++;
            this.lastActivityTime = LocalDateTime.now();
        }
        
        public void incrementConnections() {
            this.connectionCount++;
        }
        
        public void incrementReconnections() {
            this.reconnectionCount++;
        }
    }
}