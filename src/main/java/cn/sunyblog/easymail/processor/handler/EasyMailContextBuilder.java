package cn.sunyblog.easymail.processor.handler;

import cn.sunyblog.easymail.mail.EasyMailContentParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import cn.sunyblog.easymail.exception.EasyMailProcessException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 邮件上下文构建器
 * 负责将原始邮件信息转换为EmailContext对象
 *
 * @author suny
 * @version 1.0
 * @date 2025/06/14
 */
@Slf4j
@Component
public class EasyMailContextBuilder {

    @Resource
    private EasyMailContentParser contentParser;

    /**
     * 验证码提取正则表达式
     */
    private static final List<Pattern> VERIFICATION_CODE_PATTERNS = Arrays.asList(
            // 4-8位数字验证码，避免匹配时间戳
            Pattern.compile("(?<!\\d)([0-9]{4,8})(?!\\d)(?![年月日时分秒])"),
            // 明确标识的验证码
            Pattern.compile("(?:验证码|verification code|code)[：:]*\\s*([0-9]{4,8})", Pattern.CASE_INSENSITIVE),
            // 括号中的数字
            Pattern.compile("\\(([0-9]{4,8})\\)"),
            // 引号中的数字
            Pattern.compile("[\"']([0-9]{4,8})[\"']"),
            // 字母数字混合验证码
            Pattern.compile("(?:验证码|verification code|code)[：:]*\\s*([A-Z0-9]{4,8})", Pattern.CASE_INSENSITIVE)
    );

    /**
     * 时间相关关键词，用于排除时间戳被误识别为验证码
     */
    private static final Set<String> TIME_KEYWORDS = new HashSet<>(Arrays.asList(
            "年", "月", "日", "时", "分", "秒", "GMT", "UTC", "CST", "PST", "EST"
    ));

    /**
     * 构建邮件上下文
     *
     * @param message       原始邮件消息
     * @param attachmentDir 附件保存目录
     * @return 邮件上下文
     */
    public EasyMailContext buildContext(Message message, String attachmentDir) {
        try {
            EasyMailContext.EasyMailContextBuilder builder = EasyMailContext.builder();

            // 设置原始消息
            builder.message(message);

            // 解析基本信息
            parseBasicInfo(message, builder);

            // 解析内容
            parseContent(message, attachmentDir, builder);

            // 解析附件
            parseAttachments(message, builder);

            // 提取标签
            extractTags(message, builder);

            // 设置时间信息
            parseTimeInfo(message, builder);

            // 设置其他属性
            parseOtherProperties(message, builder);

            // 初始化扩展属性
            builder.attributes(new HashMap<>());

            return builder.build();

        } catch (Exception e) {
            EasyMailProcessException processEx = EasyMailProcessException.parsingError("构建邮件上下文失败", e);
            log.error(processEx.getFullErrorMessage(), processEx);
            throw processEx;
        }
    }

    /**
     * 解析基本信息
     */
    private void parseBasicInfo(Message message, EasyMailContext.EasyMailContextBuilder builder) {

        try {
            // 主题
            String subject = message.getSubject();
            builder.subject(subject != null ? contentParser.decodeText(subject) : "(无主题)");
        } catch (javax.mail.FolderClosedException e) {
            log.warn("邮件文件夹已关闭，无法获取主题: {}", e.getMessage());
            builder.subject("[文件夹已关闭]");
        } catch (Exception e) {
            log.warn("获取邮件主题失败: {}", e.getMessage());
            builder.subject("(主题获取失败)");
        }

        try {
            // 发件人
            Address[] fromAddresses = message.getFrom();
            if (fromAddresses != null && fromAddresses.length > 0) {
                String from = contentParser.decodeText(fromAddresses[0].toString());
                builder.from(from);
            } else {
                builder.from("(未知发件人)");
            }
        } catch (javax.mail.FolderClosedException e) {
            log.warn("邮件文件夹已关闭，无法获取发件人: {}", e.getMessage());
            builder.from("unknown@folder.closed");
        } catch (Exception e) {
            log.warn("获取发件人失败: {}", e.getMessage());
            builder.from("(发件人获取失败)");
        }

        try {
            // 收件人
            builder.to(parseAddresses(message.getRecipients(Message.RecipientType.TO)));
        } catch (javax.mail.FolderClosedException e) {
            log.warn("邮件文件夹已关闭，无法获取收件人: {}", e.getMessage());
            builder.to(new ArrayList<>());
        } catch (Exception e) {
            log.warn("获取收件人失败: {}", e.getMessage());
            builder.to(new ArrayList<>());
        }

        try {
            // 抄送
            builder.cc(parseAddresses(message.getRecipients(Message.RecipientType.CC)));
        } catch (javax.mail.FolderClosedException e) {
            log.warn("邮件文件夹已关闭，无法获取抄送: {}", e.getMessage());
            builder.cc(new ArrayList<>());
        } catch (Exception e) {
            log.warn("获取抄送失败: {}", e.getMessage());
            builder.cc(new ArrayList<>());
        }

        try {
            // 密送
            builder.bcc(parseAddresses(message.getRecipients(Message.RecipientType.BCC)));
        } catch (javax.mail.FolderClosedException e) {
            log.warn("邮件文件夹已关闭，无法获取密送: {}", e.getMessage());
            builder.bcc(new ArrayList<>());
        } catch (Exception e) {
            log.warn("获取密送失败: {}", e.getMessage());
            builder.bcc(new ArrayList<>());
        }
    }

    /**
     * 解析地址列表
     */
    private List<String> parseAddresses(Address[] addresses) {
        if (addresses == null) {
            return new ArrayList<>();
        }

        List<String> result = new ArrayList<>();
        for (Address address : addresses) {
            if (address instanceof InternetAddress) {
                InternetAddress internetAddress = (InternetAddress) address;
                String personal = internetAddress.getPersonal();
                String email = internetAddress.getAddress();

                if (personal != null && !personal.isEmpty()) {
                    result.add(personal + " <" + email + ">");
                } else {
                    result.add(email);
                }
            } else {
                result.add(address.toString());
            }
        }
        return result;
    }

    /**
     * 解析内容
     */
    private void parseContent(Message message, String attachmentDir,
                              EasyMailContext.EasyMailContextBuilder builder) {

        try {
            // 只调用一次parseContent，避免重复解析
            String content = contentParser.parseContent(message, attachmentDir);

            // 设置文本内容和通用内容字段
            builder.textContent(content);
            builder.content(content); // 设置兼容性字段

            // 如果是HTML内容，也保存HTML版本
            if (message.isMimeType("text/html")) {
                builder.htmlContent(message.getContent().toString());
            } else if (message.isMimeType("multipart/*")) {
                // 从多部分邮件中提取HTML内容
                String htmlContent = extractHtmlFromMultipart((Multipart) message.getContent());
                if (htmlContent != null) {
                    builder.htmlContent(htmlContent);
                }
            }
        } catch (javax.mail.FolderClosedException e) {
            log.warn("邮件文件夹已关闭，无法解析内容: {}", e.getMessage());
            builder.textContent("[邮件内容解析失败：文件夹已关闭]");
            builder.content("[邮件内容解析失败：文件夹已关闭]");
        } catch (Exception e) {
            log.warn("邮件内容解析失败: {}", e.getMessage());
            builder.textContent("[邮件内容解析失败]");
            builder.content("[邮件内容解析失败]");
        }
    }

    /**
     * 从多部分邮件中提取HTML内容
     */
    private String extractHtmlFromMultipart(Multipart multipart) {
        try {
            int count = multipart.getCount();
            for (int i = 0; i < count; i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                if (bodyPart.isMimeType("text/html")) {
                    return bodyPart.getContent().toString();
                } else if (bodyPart.isMimeType("multipart/*")) {
                    String htmlContent = extractHtmlFromMultipart((Multipart) bodyPart.getContent());
                    if (htmlContent != null) {
                        return htmlContent;
                    }
                }
            }
        } catch (javax.mail.FolderClosedException e) {
            log.warn("邮件文件夹已关闭，无法提取HTML内容: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.warn("提取HTML内容失败: {}", e.getMessage());
            return null;
        }
        return null;
    }

    /**
     * 解析附件
     */
    private void parseAttachments(Message message, EasyMailContext.EasyMailContextBuilder builder) {

        List<EasyMailContext.AttachmentInfo> attachments = new ArrayList<>();

        try {
            if (message.isMimeType("multipart/*")) {
                Multipart multipart = (Multipart) message.getContent();
                extractAttachmentsFromMultipart(multipart, attachments);
            }
        } catch (javax.mail.FolderClosedException e) {
            log.warn("邮件文件夹已关闭，无法解析附件: {}", e.getMessage());
            // 不抛出异常，继续处理其他部分
        } catch (Exception e) {
            log.warn("解析附件失败: {}", e.getMessage());
            // 不抛出异常，继续处理其他部分
        }

        builder.attachments(attachments);
    }

    /**
     * 从多部分邮件中提取附件信息
     */
    private void extractAttachmentsFromMultipart(Multipart multipart,
                                                 List<EasyMailContext.AttachmentInfo> attachments) {

        try {
            int count = multipart.getCount();
            for (int i = 0; i < count; i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);

                String disposition = bodyPart.getDisposition();
                if (disposition != null && disposition.equalsIgnoreCase(BodyPart.ATTACHMENT)) {
                    String fileName = bodyPart.getFileName();
                    if (fileName != null) {
                        fileName = contentParser.decodeText(fileName);

                        EasyMailContext.AttachmentInfo attachmentInfo = EasyMailContext.AttachmentInfo.builder()
                                .fileName(fileName)
                                .fileSize(bodyPart.getSize())
                                .contentType(bodyPart.getContentType())
                                .inline(false)
                                .build();

                        attachments.add(attachmentInfo);
                    }
                } else if (bodyPart.isMimeType("multipart/*")) {
                    extractAttachmentsFromMultipart((Multipart) bodyPart.getContent(), attachments);
                }
            }
        } catch (javax.mail.FolderClosedException e) {
            log.warn("邮件文件夹已关闭，无法提取附件信息: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("提取附件信息失败: {}", e.getMessage());
        }
    }

    /**
     * 提取标签
     */
    private void extractTags(Message message, EasyMailContext.EasyMailContextBuilder builder) {

        List<String> tags = new ArrayList<>();

        try {
            // 从主题中提取标签
            String subject = message.getSubject();
            if (subject != null) {
                // 提取方括号中的标签
                Pattern tagPattern = Pattern.compile("\\[([^\\]]+)\\]");
                Matcher matcher = tagPattern.matcher(subject);
                while (matcher.find()) {
                    tags.add(matcher.group(1).trim());
                }

                // 根据主题内容添加分类标签
                addCategoryTags(subject, tags);
            }

            // 从邮件头中提取标签
            String[] xLabels = message.getHeader("X-Label");
            if (xLabels != null) {
                Collections.addAll(tags, xLabels);
            }
        } catch (javax.mail.FolderClosedException e) {
            log.warn("邮件文件夹已关闭，无法提取标签信息: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("提取标签信息失败: {}", e.getMessage());
        }

        builder.tags(tags);
    }

    /**
     * 根据主题内容添加分类标签
     */
    private void addCategoryTags(String subject, List<String> tags) {
        String lowerSubject = subject.toLowerCase();

        if (lowerSubject.contains("验证码") || lowerSubject.contains("verification") ||
                lowerSubject.contains("code")) {
            tags.add("验证码");
        }

        if (lowerSubject.contains("通知") || lowerSubject.contains("notification")) {
            tags.add("通知");
        }

        if (lowerSubject.contains("订单") || lowerSubject.contains("order")) {
            tags.add("订单");
        }

        if (lowerSubject.contains("密码") || lowerSubject.contains("password")) {
            tags.add("密码");
        }

        if (lowerSubject.contains("账单") || lowerSubject.contains("bill") ||
                lowerSubject.contains("invoice")) {
            tags.add("账单");
        }
    }

    /**
     * 解析时间信息
     */
    private void parseTimeInfo(Message message, EasyMailContext.EasyMailContextBuilder builder) {

        try {
            // 接收时间
            Date receivedDate = message.getReceivedDate();
            if (receivedDate != null) {
                builder.receivedTime(LocalDateTime.ofInstant(
                        receivedDate.toInstant(), ZoneId.systemDefault()));
            }

            // 发送时间
            Date sentDate = message.getSentDate();
            if (sentDate != null) {
                builder.sentTime(LocalDateTime.ofInstant(
                        sentDate.toInstant(), ZoneId.systemDefault()));
            }
        } catch (javax.mail.FolderClosedException e) {
            log.warn("邮件文件夹已关闭，无法获取时间信息: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("获取时间信息失败: {}", e.getMessage());
        }
    }

    /**
     * 解析其他属性
     */
    private void parseOtherProperties(Message message, EasyMailContext.EasyMailContextBuilder builder) {

        try {
            // 邮件大小
            builder.size(message.getSize());

            // 优先级
            String[] priority = message.getHeader("X-Priority");
            if (priority != null && priority.length > 0) {
                builder.priority(priority[0]);
            }

            // 编码
            String contentType = message.getContentType();
            if (contentType != null) {
                String encoding = extractEncoding(contentType);
                builder.encoding(encoding);
            }
        } catch (javax.mail.FolderClosedException e) {
            log.warn("邮件文件夹已关闭，无法获取其他属性: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("获取其他属性失败: {}", e.getMessage());
        }
    }

    /**
     * 从Content-Type中提取编码
     */
    private String extractEncoding(String contentType) {
        Pattern pattern = Pattern.compile("charset=([^;\\s]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(contentType);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "UTF-8";
    }

    /**
     * 智能提取验证码
     * 改进的验证码提取逻辑，避免将时间戳误识别为验证码
     *
     * @param content 邮件内容
     * @return 验证码，如果没有找到则返回null
     */
    public String extractVerificationCode(String content) {
        if (content == null || content.trim().isEmpty()) {
            return null;
        }

        // 按优先级尝试不同的正则表达式
        for (Pattern pattern : VERIFICATION_CODE_PATTERNS) {
            Matcher matcher = pattern.matcher(content);
            while (matcher.find()) {
                String code = matcher.group(1);

                // 验证是否为有效的验证码
                if (isValidVerificationCode(code, content, matcher.start(), matcher.end())) {
                    log.debug("提取到验证码: {} (使用模式: {})", code, pattern.pattern());
                    return code;
                }
            }
        }

        return null;
    }

    /**
     * 验证是否为有效的验证码
     */
    private boolean isValidVerificationCode(String code, String content, int start, int end) {
        // 检查长度
        if (code.length() < 4 || code.length() > 8) {
            return false;
        }

        // 检查周围的上下文，排除时间戳
        String context = getContext(content, start, end, 20);
        for (String keyword : TIME_KEYWORDS) {
            if (context.contains(keyword)) {
                log.debug("排除时间相关数字: {} (上下文包含: {})", code, keyword);
                return false;
            }
        }

        // 排除明显的年份
        if (code.matches("^(19|20)\\d{2}$")) {
            log.debug("排除年份: {}", code);
            return false;
        }

        // 排除明显的日期格式
        if (code.matches("^(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])$")) {
            log.debug("排除日期格式: {}", code);
            return false;
        }

        return true;
    }

    /**
     * 获取指定位置周围的上下文
     */
    private String getContext(String content, int start, int end, int contextLength) {
        int contextStart = Math.max(0, start - contextLength);
        int contextEnd = Math.min(content.length(), end + contextLength);
        return content.substring(contextStart, contextEnd);
    }
}