package cn.sunyblog.javaemaildemo.mail;
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
public class MailContentParser {

    /**
     * 解析邮件内容
     *
     * @param message 邮件消息
     * @param saveDir 附件保存目录
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
        String subject = message.getSubject() != null ? message.getSubject() : "(无主题)";

        // 获取发件人
        String fromStr = "(未知发件人)";
        Address[] fromAddresses = message.getFrom();
        if (fromAddresses != null && fromAddresses.length > 0 && fromAddresses[0] != null) {
            fromStr = decodeText(fromAddresses[0].toString());
        }

        parseInfoTime = System.currentTimeMillis();
        log.info("处理邮件 - 主题: {}, 发件人: {}", subject, fromStr);

        // 处理邮件内容
        Object content = message.getContent();

        if (content instanceof Multipart) {
            Multipart multipart = (Multipart) content;
            contentBuilder.append(parseMultipart(multipart, saveDir));
        } else if (content instanceof String) {
            String textContent = (String) content;
            if (message.getContentType().toLowerCase().contains("html")) {
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
     * @param saveDir   附件保存目录
     * @return 解析后的文本内容
     * @throws MessagingException 邮件异常
     * @throws IOException        IO异常
     */
    public String parseMultipart(Multipart multipart, String saveDir) throws MessagingException, IOException {
        int count = multipart.getCount();
        StringBuilder contentBuilder = new StringBuilder();

        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);

            if (bodyPart.isMimeType("text/plain")) {
                Object content = bodyPart.getContent();
                String textContent = content instanceof String ? (String) content : "";
                log.info("文本内容: {}", textContent);
                contentBuilder.append(textContent);
            } else if (bodyPart.isMimeType("text/html")) {
                // 获取HTML内容并转换为纯文本
                String htmlContent = bodyPart.getContent().toString();
                String plainText = extractTextFromHtml(htmlContent);
                log.info("HTML内容: {}", plainText);
                contentBuilder.append(plainText);
            } else if (bodyPart.isMimeType("multipart/*")) {
                contentBuilder.append(parseMultipart((Multipart) bodyPart.getContent(), saveDir));
            } else if (bodyPart.isMimeType("application/octet-stream")) {
                parseAttachment(bodyPart, saveDir);
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
     * @throws IOException        IO异常
     */
    private void parseAttachment(BodyPart bodyPart, String saveDir) throws MessagingException, IOException {
        String disposition = bodyPart.getDisposition();

        if (disposition != null && disposition.equalsIgnoreCase(BodyPart.ATTACHMENT)) {
            String fileName = bodyPart.getFileName();
            if (fileName == null) fileName = "unknown_file";
            fileName = decodeText(fileName);

            File outputDir = new File(saveDir);
            if (!outputDir.exists()) {
                if (outputDir.mkdirs()) {
                    log.info("创建附件保存目录: {}", saveDir);
                } else {
                    log.error("创建附件保存目录失败: {}", saveDir);
                    return;
                }
            }

            File targetFile = new File(saveDir + fileName);
            log.info("保存附件: {}", targetFile.getAbsolutePath());

            try (InputStream is = bodyPart.getInputStream();
                 OutputStream os = Files.newOutputStream(targetFile.toPath())) {

                byte[] buffer = new byte[8192]; // 增大缓冲区
                int bytesRead;
                long total = 0;

                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                    total += bytesRead;
                }

                log.info("附件保存成功: {}, 大小: {} 字节", fileName, total);
            } catch (IOException e) {
                log.error("保存附件失败: {}, 原因: {}", fileName, e.getMessage(), e);
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
        String noHtml = html.replaceAll("\\<.*?\\>", "");

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
            if (text.startsWith("=?GB") || text.startsWith("=?gb") ||
                    text.startsWith("=?utf") || text.startsWith("=?UTF")) {
                return MimeUtility.decodeText(text);
            } else {
                return new String(text.getBytes("ISO8859_1"));
            }
        } catch (Exception e) {
            log.error("解码文本失败: {}", e.getMessage(), e);
            return text;
        }
    }

    /**
     * 提取验证码的方法
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