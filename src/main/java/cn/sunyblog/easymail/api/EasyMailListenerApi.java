package cn.sunyblog.easymail.api;

import javax.mail.Message;

/**
 * 邮件监听器API接口
 * 开发者可以实现此接口来处理接收到的邮件
 */
public interface EasyMailListenerApi {

    /**
     * 处理接收到的邮件
     *
     * @param message 原始邮件消息
     * @param content 解析后的邮件内容
     * @param subject 邮件主题
     * @param from    发件人
     * @return 处理结果，true表示处理成功，false表示处理失败
     */
    boolean processEmail(Message message, String content, String subject, String from);

    /**
     * 获取处理器名称
     *
     * @return 处理器名称
     */
    default String getProcessorName() {
        return this.getClass().getSimpleName();
    }
}