package cn.sunyblog.javaemaildemo.mail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.mail.*;
import javax.mail.internet.MimeUtility;
import java.io.*;
import java.nio.file.Files;

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
}