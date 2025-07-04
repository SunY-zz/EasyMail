package cn.sunyblog.easymail.mail;

import cn.sunyblog.easymail.exception.EasyMailProcessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.mail.*;
import javax.mail.internet.MimeUtility;
import java.io.*;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author suny
 * @version 1.0
 * @description: 邮件内容解析器
 * @date 2025/05/12 16:23
 */
@Slf4j
@Component
public class EasyMailContentParser {

    /**
     * 解析邮件内容
     *
     * @param message 邮件消息
     * @param saveDir 附件保存目录（可以为null，此时跳过附件保存）
     * @return 解析后的文本内容
     * @throws MessagingException 邮件异常
     * @throws IOException        IO异常
     */
    public String parseContent(Message message, String saveDir) throws MessagingException, IOException {
        long functionStartTime = System.currentTimeMillis();
        long parseInfoTime;
        long parseContentTime;
        StringBuilder contentBuilder = new StringBuilder();

        // 获取邮件基本信息
        String subject = "(无主题)";
        String fromStr = "(未知发件人)";
        Object content = null;
        String contentType = null;

        try {
            subject = message.getSubject() != null ? decodeText(message.getSubject()) : "(无主题)";

            // 获取发件人
            Address[] fromAddresses = message.getFrom();
            if (fromAddresses != null && fromAddresses.length > 0 && fromAddresses[0] != null) {
                fromStr = decodeText(fromAddresses[0].toString());
            }

            // 处理邮件内容
            content = message.getContent();
            contentType = message.getContentType();

        } catch (javax.mail.FolderClosedException e) {
            log.warn("邮件文件夹已关闭，无法获取邮件基本信息: {}", e.getMessage());
            parseInfoTime = System.currentTimeMillis();
            parseContentTime = System.currentTimeMillis();
            log.info("邮件处理详情 - 获取基本信息耗时: {}毫秒, 解析内容耗时: {}毫秒, 总耗时: {}毫秒",
                    parseInfoTime - functionStartTime,
                    parseContentTime - parseInfoTime,
                    parseContentTime - functionStartTime);
            return "(邮件文件夹已关闭，无法解析内容)";
        } catch (Exception e) {
            log.warn("获取邮件基本信息失败: {}", e.getMessage());
        }

        parseInfoTime = System.currentTimeMillis();
        log.info("处理邮件 - 主题: {}, 发件人: {}", subject, fromStr);

        if (content instanceof Multipart) {
            Multipart multipart = (Multipart) content;
            contentBuilder.append(parseMultipart(multipart, saveDir));
        } else if (content instanceof String) {
            String textContent = decodeText((String) content); // 解码文本内容
            if (contentType != null && contentType.toLowerCase().contains("html")) {
                // 处理HTML内容
                String plainText = extractTextFromHtml(textContent);
                log.info("邮件HTML内容: {}", plainText);
                contentBuilder.append(plainText);
            } else {
                // 纯文本内容
                log.info("邮件文本内容: {}", textContent);
                contentBuilder.append(textContent);
            }
        } else if (content != null) {
            log.info("邮件其他类型内容: {}", content);
            contentBuilder.append(content);
        } else {
            log.warn("邮件内容为空");
        }

        parseContentTime = System.currentTimeMillis();
        log.info("邮件处理详情 - 获取基本信息耗时: {}毫秒, 解析内容耗时: {}毫秒, 总耗时: {}毫秒",
                parseInfoTime - functionStartTime,
                parseContentTime - parseInfoTime,
                parseContentTime - functionStartTime);

        return contentBuilder.toString();
    }

    /**
     * 解析多部分邮件
     *
     * @param multipart 多部分邮件
     * @param saveDir   附件保存目录（可以为null，此时跳过附件保存）
     * @return 解析后的文本内容
     * @throws MessagingException 邮件异常
     * @throws IOException        IO异常
     */
    public String parseMultipart(Multipart multipart, String saveDir) throws MessagingException, IOException {
        int count = multipart.getCount();
        StringBuilder contentBuilder = new StringBuilder();

        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);

            try {
                if (bodyPart.isMimeType("text/plain")) {
                    Object content = bodyPart.getContent();
                    String textContent = content instanceof String ? decodeText((String) content) : "";
                    log.info("文本内容: {}", textContent);
                    contentBuilder.append(textContent);
                } else if (bodyPart.isMimeType("text/html")) {
                    // 获取HTML内容并转换为纯文本
                    String htmlContent = decodeText(bodyPart.getContent().toString());
                    String plainText = extractTextFromHtml(htmlContent);
                    log.info("HTML内容: {}", plainText);
                    contentBuilder.append(plainText);
                } else if (bodyPart.isMimeType("multipart/*")) {
                    contentBuilder.append(parseMultipart((Multipart) bodyPart.getContent(), saveDir));
                } else {
                    // 检查是否为附件（通过disposition或其他MIME类型）
                    String disposition = bodyPart.getDisposition();
                    if (disposition != null && (disposition.equalsIgnoreCase(BodyPart.ATTACHMENT) ||
                            disposition.equalsIgnoreCase(BodyPart.INLINE))) {
                        try {
                            parseAttachment(bodyPart, saveDir);
                        } catch (Exception e) {
                            log.warn("附件解析失败，跳过该附件: {}", e.getMessage());
                        }
                    } else if (bodyPart.isMimeType("application/*") ||
                            bodyPart.isMimeType("image/*") ||
                            bodyPart.isMimeType("audio/*") ||
                            bodyPart.isMimeType("video/*")) {
                        // 其他可能的附件类型
                        try {
                            parseAttachment(bodyPart, saveDir);
                        } catch (Exception e) {
                            log.warn("附件解析失败，跳过该附件: {}", e.getMessage());
                        }
                    } else {
                        log.debug("跳过未知类型的邮件部分: {}", bodyPart.getContentType());
                    }
                }
            } catch (javax.mail.FolderClosedException e) {
                log.warn("邮件文件夹已关闭，停止解析邮件内容部分 {}: {}", i, e.getMessage());
                break; // 文件夹关闭时停止解析
            } catch (Exception e) {
                log.warn("解析邮件内容部分 {} 时发生错误，跳过该部分: {}", i, e.getMessage());
                // 继续处理下一个部分
            }
        }

        return contentBuilder.toString();
    }

    /**
     * 解析附件
     *
     * @param bodyPart 邮件体部分
     * @param saveDir  保存目录
     * @throws MessagingException 邮件异常
     */
    private void parseAttachment(BodyPart bodyPart, String saveDir) throws MessagingException {
        String disposition = bodyPart.getDisposition();

        // 检查是否为附件（ATTACHMENT或INLINE类型，或者有文件名的部分）
        boolean isAttachment = (disposition != null &&
                (disposition.equalsIgnoreCase(BodyPart.ATTACHMENT) ||
                        disposition.equalsIgnoreCase(BodyPart.INLINE))) ||
                bodyPart.getFileName() != null;

        if (isAttachment) {
            String fileName = bodyPart.getFileName();
            if (fileName == null) fileName = "unknown_file";
            fileName = decodeText(fileName);

            // 处理saveDir为null的情况
            if (saveDir == null || saveDir.trim().isEmpty()) {
                log.warn("附件保存目录未配置，跳过附件保存: {}", fileName);
                return;
            }

            // 确保saveDir以文件分隔符结尾
            if (!saveDir.endsWith(File.separator)) {
                saveDir = saveDir + File.separator;
            }

            File outputDir = new File(saveDir);
            if (!outputDir.exists()) {
                if (outputDir.mkdirs()) {
                    log.info("创建附件保存目录: {}", saveDir);
                } else {
                    throw new EasyMailProcessException("创建附件保存目录失败: " + saveDir);
                }
            }

            File targetFile = new File(saveDir + fileName);
            log.info("保存附件: {}", targetFile.getAbsolutePath());

            try (InputStream is = bodyPart.getInputStream();
                 OutputStream os = Files.newOutputStream(targetFile.toPath())) {

                byte[] buffer = new byte[8192]; // 增大缓冲区
                int bytesRead;
                long total = 0;
                long startTime = System.currentTimeMillis();
                final long MAX_ATTACHMENT_SIZE = 50 * 1024 * 1024; // 50MB限制
                final long MAX_PROCESS_TIME = 30 * 1000; // 30秒超时

                while ((bytesRead = is.read(buffer)) != -1) {
                    // 检查文件大小限制
                    if (total + bytesRead > MAX_ATTACHMENT_SIZE) {
                        log.warn("附件 {} 超过大小限制 {}MB，停止保存", fileName, MAX_ATTACHMENT_SIZE / (1024 * 1024));
                        throw new EasyMailProcessException("附件超过大小限制: " + fileName);
                    }

                    // 检查处理时间限制
                    if (System.currentTimeMillis() - startTime > MAX_PROCESS_TIME) {
                        log.warn("附件 {} 保存超时，停止保存", fileName);
                        throw new EasyMailProcessException("附件保存超时: " + fileName);
                    }

                    os.write(buffer, 0, bytesRead);
                    total += bytesRead;
                }

                log.info("附件保存成功: {}, 大小: {} 字节", fileName, total);
            } catch (IOException e) {
                // 删除可能创建的不完整文件
                if (targetFile.exists()) {
                    try {
                        Files.delete(targetFile.toPath());
                        log.debug("已删除不完整的附件文件: {}", targetFile.getAbsolutePath());
                    } catch (IOException deleteEx) {
                        log.warn("删除不完整附件文件失败: {}", deleteEx.getMessage());
                    }
                }
                throw EasyMailProcessException.processingError("保存附件失败: " + fileName, e);
            }
        }
    }

    /**
     * 从HTML中提取纯文本
     *
     * @param html HTML文本
     * @return 纯文本
     */
    public String extractTextFromHtml(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }

        // 去除HTML标签
        String noHtml = html.replaceAll("<.*?>", "");

        // 处理HTML实体
        String noHtmlEntities = noHtml.replaceAll("&nbsp;", " ")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&amp;", "&")
                .replaceAll("&quot;", "\"")
                .replaceAll("&apos;", "'");

        // 去除多余的空白字符
        return noHtmlEntities.replaceAll("\\s+", " ").trim();
    }

    /**
     * 解码文本
     *
     * @param text 需要解码的文本
     * @return 解码后的文本
     */
    public String decodeText(String text) {
        if (text == null) return "(空文本)";

        try {
            // 首先尝试使用MimeUtility解码
            if (text.startsWith("=?")) {
                return MimeUtility.decodeText(text);
            }

            // 检测是否包含中文字符或其他非ASCII字符
            if (text.matches(".*[\u4e00-\u9fa5].*") || text.matches(".*[\\u0080-\\uFFFF].*")) {
                // 已经是正确编码的文本，直接返回
                return text;
            }


            // 尝试不同的编码方式
            String[] encodings = {"UTF-8", "GBK", "GB2312", "ISO8859-1"};
            for (String encoding : encodings) {
                try {
                    String decoded = new String(text.getBytes("ISO8859-1"), encoding);
                    // 检查解码后是否包含中文字符，如果是则认为解码成功
                    if (decoded.matches(".*[\u4e00-\u9fa5].*")) {
                        return decoded;
                    }
                } catch (Exception ignored) {
                    // 继续尝试下一种编码
                }
            }

            // 如果所有编码都失败，返回原文本
            return text;
        } catch (Exception e) {
            log.warn("解码文本失败，返回原文本: {}", text);
            return text; // 返回原文本而不是抛出异常
        }
    }

    /**
     * 提取验证码的方法
     *
     * @param content 邮件内容
     * @return 验证码，如果没有找到则返回null
     */
    public String extractVerificationCode(String content) {
        if (content == null || content.trim().isEmpty()) {
            return null;
        }

        // 排除时间戳和其他常见误识别内容的关键词
        String[] excludeKeywords = {
                "年", "月", "日", "时", "分", "秒",
                "GMT", "UTC", "CST", "EST", "PST",
                "发送时间", "接收时间", "创建时间", "更新时间",
                "订单号", "流水号", "交易号", "单号",
                "电话", "手机", "座机", "传真",
                "邮编", "区号", "身份证",
                "IP", "端口", "PORT"
        };

        // 常见的验证码模式：4-8位数字或字母数字组合
        String[] patterns = {
                "(?i)(?:验证码|verification\\s*code|auth\\s*code|code)[：:：\\s]*([A-Z0-9]{4,8})",  // 验证码：XXXX
                "(?i)(?:验证码|verification\\s*code|auth\\s*code|code)[：:：\\s]*([0-9]{4,8})",     // 验证码：1234
                "(?i)(?:动态码|dynamic\\s*code)[：:：\\s]*([A-Z0-9]{4,8})",                      // 动态码
                "(?i)(?:安全码|security\\s*code)[：:：\\s]*([A-Z0-9]{4,8})",                      // 安全码
                "\\b([0-9]{6})\\b",                                                            // 独立的6位数字
                "\\b([0-9]{4})\\b",                                                            // 独立的4位数字
                "\\b([A-Z0-9]{6})\\b",                                                         // 独立的6位字母数字
                "\\b([A-Z0-9]{4})\\b"                                                          // 独立的4位字母数字
        };

        for (String pattern : patterns) {
            Pattern regex = Pattern.compile(pattern);
            Matcher matcher = regex.matcher(content);
            while (matcher.find()) {
                String code = matcher.group(1);

                // 检查是否包含排除关键词
                boolean shouldExclude = false;
                String contextBefore = content.substring(Math.max(0, matcher.start() - 50), matcher.start());
                String contextAfter = content.substring(matcher.end(), Math.min(content.length(), matcher.end() + 50));
                String fullContext = contextBefore + code + contextAfter;

                for (String keyword : excludeKeywords) {
                    if (fullContext.contains(keyword)) {
                        shouldExclude = true;
                        log.debug("排除验证码候选 '{}' 因为包含关键词 '{}'", code, keyword);
                        break;
                    }
                }

                // 排除明显的年份（1900-2100）
                if (!shouldExclude && code.matches("^(19|20)\\d{2}$")) {
                    shouldExclude = true;
                    log.debug("排除验证码候选 '{}' 因为是年份格式", code);
                }

                // 排除明显的时间格式（如：2359, 1200等）
                if (!shouldExclude && code.matches("^([01]\\d|2[0-3])[0-5]\\d$")) {
                    shouldExclude = true;
                    log.debug("排除验证码候选 '{}' 因为是时间格式", code);
                }

                if (!shouldExclude) {
                    log.debug("使用模式 '{}' 提取到验证码: {}", pattern, code);
                    return code;
                }
            }
        }

        log.debug("未在内容中找到验证码: {}", content.substring(0, Math.min(100, content.length())));
        return null;
    }
}