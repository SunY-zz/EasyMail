package cn.sunyblog.easymail.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 邮件异常处理测试类
 * 验证自定义异常体系的功能
 *
 * @author suny
 * @version 1.0.0
 */
public class EmailExceptionTest {

    @Test
    public void testEmailValidationException() {
        // 测试空收件人异常
        EmailValidationException emptyRecipientsException = EmailValidationException.emptyRecipients();
        assertEquals("EMAIL_EMPTY_RECIPIENTS", emptyRecipientsException.getErrorCode());
        assertTrue(emptyRecipientsException.getFullErrorMessage().contains("收件人列表不能为空"));

        // 测试无效邮箱地址异常
        String invalidEmail = "invalid-email";
        EmailValidationException invalidEmailException = EmailValidationException.invalidEmailAddresses(Collections.singletonList(invalidEmail));
        assertEquals("EMAIL_INVALID_ADDRESS", invalidEmailException.getErrorCode());
        assertTrue(invalidEmailException.getFullErrorMessage().contains(invalidEmail));

        // 测试多个验证错误
        List<String> errors = Arrays.asList("错误1", "错误2", "错误3");
        EmailValidationException multipleErrorsException = new EmailValidationException("多个验证错误", errors);
        String fullMessage = multipleErrorsException.getFullErrorMessage();
        assertTrue(fullMessage.contains("错误1"));
        assertTrue(fullMessage.contains("错误2"));
        assertTrue(fullMessage.contains("错误3"));
    }

    @Test
    public void testEmailTemplateException() {
        // 测试模板不存在异常
        String templateId = "template-001";
        EmailTemplateException templateNotFoundException = EmailTemplateException.templateNotFound(templateId);
        assertEquals("EMAIL_TEMPLATE_NOT_FOUND", templateNotFoundException.getErrorCode());
        assertEquals(templateId, templateNotFoundException.getTemplateId());
        assertTrue(templateNotFoundException.getFullErrorMessage().contains(templateId));

        // 测试模板渲染错误
        Exception cause = new RuntimeException("渲染失败");
        EmailTemplateException renderErrorException = EmailTemplateException.renderError(templateId, cause);
        assertEquals("EMAIL_TEMPLATE_RENDER_ERROR", renderErrorException.getErrorCode());
        assertEquals(templateId, renderErrorException.getTemplateId());
        assertEquals(cause, renderErrorException.getCause());
    }

    @Test
    public void testEmailConnectionException() {
        // 测试连接异常
        String server = "smtp.example.com";
        int port = 587;
        String message = "连接超时";

        EmailConnectionException connectionException = new EmailConnectionException(
            message, server, port);

        assertEquals("EMAIL_CONNECTION_ERROR", connectionException.getErrorCode());
        assertEquals(port, connectionException.getPort());
        assertTrue(connectionException.getFullErrorMessage().contains(server));
        assertTrue(connectionException.getFullErrorMessage().contains(String.valueOf(port)));
    }

    @Test
    public void testEmailSendException() {
        // 测试发送异常
        String message = "发送失败";
        String errorCode = "SEND_001";
        String errorDetail = "SMTP服务器拒绝连接";

        EmailSendException sendException = new EmailSendException(
            message, errorCode, errorDetail);

        assertEquals("发送失败", sendException.getErrorCode());
        assertEquals(errorDetail, sendException.getErrorDetail());
        assertTrue(sendException.getFullErrorMessage().contains(message));
        assertTrue(sendException.getFullErrorMessage().contains(errorDetail));
    }

    @Test
    public void testEmailConfigException() {
        // 测试配置异常
        String configKey = "smtp.host";
        String configValue = "invalid-host";

        EmailConfigException configException = EmailConfigException.invalidConfig(configKey, configValue);

        assertEquals("EMAIL_CONFIG_INVALID", configException.getErrorCode());
        assertEquals(configKey, configException.getConfigKey());
        assertEquals(configValue, configException.getConfigValue());
        assertTrue(configException.getFullErrorMessage().contains(configKey));
        assertTrue(configException.getFullErrorMessage().contains(configValue));
    }

    @Test
    public void testEmailProcessException() {
        // 测试处理异常
        String messageId = "msg-001";
        String handlerName = "TestHandler";
        Exception cause = new RuntimeException("处理失败");

        EmailProcessException processException = EmailProcessException.handlerExecutionError(
            handlerName, messageId, cause);

        assertEquals("EMAIL_HANDLER_EXECUTION_ERROR", processException.getErrorCode());
        assertEquals(messageId, processException.getMessageId());
        assertEquals(handlerName, processException.getHandlerName());
        assertEquals(cause, processException.getCause());
    }

    @Test
    public void testEmailExceptionHandler() {
        // 测试邮箱地址验证
        assertTrue(EmailExceptionHandler.isValidEmail("test@example.com"));
        assertTrue(EmailExceptionHandler.isValidEmail("user.name+tag@domain.co.uk"));
        assertFalse(EmailExceptionHandler.isValidEmail("invalid-email"));
        assertFalse(EmailExceptionHandler.isValidEmail("@domain.com"));
        assertFalse(EmailExceptionHandler.isValidEmail("user@"));
        assertFalse(EmailExceptionHandler.isValidEmail(null));
        assertFalse(EmailExceptionHandler.isValidEmail(""));

        // 测试异常包装
        MessagingException messagingException = new MessagingException("SMTP错误");
        EmailException wrappedException = EmailExceptionHandler.wrapMessagingException(
            messagingException, "发送邮件");

        assertTrue(wrappedException instanceof EmailSendException);
        assertEquals(messagingException, wrappedException.getCause());
        assertTrue(wrappedException.getFullErrorMessage().contains("SMTP错误"));

        // 测试IO异常包装
        IOException ioException = new IOException("文件读取错误");
        EmailException wrappedIOException = EmailExceptionHandler.wrapIOException(
            ioException, "读取附件");

        assertTrue(wrappedIOException instanceof EmailException);
        assertEquals(ioException, wrappedIOException.getCause());

        // 测试通用异常包装
        RuntimeException runtimeException = new RuntimeException("未知错误");
        EmailException wrappedRuntimeException = EmailExceptionHandler.wrapException(
            runtimeException, "邮件处理");

        assertTrue(wrappedRuntimeException instanceof EmailException);
        assertEquals(runtimeException, wrappedRuntimeException.getCause());
    }

    @Test
    public void testExceptionChaining() {
        // 测试异常链
        Exception rootCause = new IOException("网络连接失败");
        MessagingException messagingException = new MessagingException("SMTP连接失败", rootCause);
        EmailException emailException = EmailExceptionHandler.wrapMessagingException(
            messagingException, "发送邮件");

        // 验证异常链
        assertEquals(messagingException, emailException.getCause());
        assertEquals(rootCause, emailException.getCause().getCause());

        // 验证错误信息包含所有层级的信息
        String fullMessage = emailException.getFullErrorMessage();
        assertTrue(fullMessage.contains("SMTP连接失败"));
    }

    @Test
    public void testErrorCodeConsistency() {
        // 验证错误代码的一致性
        assertEquals("EMAIL_EMPTY_RECIPIENTS", EmailValidationException.emptyRecipients().getErrorCode());
        assertEquals("EMAIL_TEMPLATE_NOT_FOUND", EmailTemplateException.templateNotFound("test").getErrorCode());
        assertEquals("EMAIL_CONNECTION_ERROR", new EmailConnectionException("test").getErrorCode());
        assertEquals("EMAIL_SEND_ERROR", new EmailSendException("test").getErrorCode());
        assertEquals("EMAIL_CONFIG_ERROR", EmailConfigException.missingConfig("test").getErrorCode());
        assertEquals("EMAIL_PROCESS_ERROR", EmailProcessException.parsingError("test", new Exception()).getErrorCode());
    }
}