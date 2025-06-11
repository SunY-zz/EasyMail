package cn.sunyblog.javaemaildemo.example;

import cn.sunyblog.javaemaildemo.mail.MailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.mail.Message;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 邮件处理器示例类 - 工具类方式
 * 这个示例展示了如何使用工具类方式处理邮件，而不是通过实现接口
 */
@Slf4j
@Component
public class EmailProcessorExample {

    @Autowired
    private MailService mailService;
    
    /**
     * 处理验证码邮件
     * 
     * @param message 邮件消息
     * @param content 邮件内容
     * @param subject 邮件主题
     * @param from 发件人
     * @return 提取的验证码，如果没有找到则返回null
     */
    public String processVerificationEmail(Message message, String content, String subject, String from) {
        log.info("工具类方式处理邮件: 主题={}, 发件人={}", subject, from);
        
        // 提取验证码
        String verificationCode = extractVerificationCode(content);
        if (verificationCode != null) {
            log.info("提取到验证码: {}", verificationCode);
            // 在这里可以将验证码存储到数据库或缓存中，供其他服务使用
        }
        
        return verificationCode;
    }
    
    /**
     * 从邮件内容中提取验证码
     * 
     * @param content 邮件内容
     * @return 验证码，如果没有找到则返回null
     */
    private String extractVerificationCode(String content) {
        if (content == null) {
            return null;
        }
        
        // 使用正则表达式匹配4-6位数字验证码
        Pattern pattern = Pattern.compile("\\b(\\d{4,6})\\b");
        Matcher matcher = pattern.matcher(content);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }
    
    /**
     * 启动邮件监听服务
     * 
     * @return 是否成功启动
     */
    public boolean startEmailListener() {
        return mailService.startMailMonitoring();
    }
    
    /**
     * 停止邮件监听服务
     */
    public void stopEmailListener() {
        mailService.stopMailMonitoring();
    }
    
    /**
     * 获取邮件处理统计信息
     * 
     * @return 统计信息
     */
    public String getEmailStats() {
        return mailService.getMailProcessingStats();
    }
}