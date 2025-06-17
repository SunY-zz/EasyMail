package cn.sunyblog.easymail.exception;

import lombok.extern.slf4j.Slf4j;

import javax.mail.*;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 邮件异常处理工具类
 * 提供统一的异常处理和包装功能
 *
 * @author suny
 * @version 1.0
 * @date 2025/06/16
 */
@Slf4j
public class EasyMailExceptionHandler {

    /**
     * 邮箱地址正则表达式
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    /**
     * 包装和处理异常
     *
     * @param throwable 原始异常
     * @param context   异常上下文信息
     * @return 包装后的EmailException
     */
    public static EasyMailException wrapException(Throwable throwable, String context) {
        if (throwable == null) {
            return new EasyMailException("未知异常");
        }

        // 如果已经是EmailException，直接返回
        if (throwable instanceof EasyMailException) {
            return (EasyMailException) throwable;
        }

        // 根据异常类型进行包装
        if (throwable instanceof MessagingException) {
            return wrapMessagingException((MessagingException) throwable, context);
        }

        if (throwable instanceof IOException) {
            return wrapIOException((IOException) throwable, context);
        }

        if (throwable instanceof RuntimeException) {
            return wrapRuntimeException((RuntimeException) throwable, context);
        }

        // 默认包装
        return new EasyMailException("EMAIL_UNKNOWN_ERROR",
                "未知异常: " + throwable.getMessage(),
                context, throwable);
    }

    /**
     * 包装MessagingException
     *
     * @param ex      MessagingException
     * @param context 上下文信息
     * @return EmailException
     */
    public static EasyMailException wrapMessagingException(MessagingException ex, String context) {
        String message = ex.getMessage();

        // 连接相关异常
        if (isConnectionException(ex)) {
            return new EasyMailConnectionException(
                    "EMAIL_CONNECTION_FAILED",
                    "邮件服务器连接失败: " + message,
                    extractServerFromContext(context),
                    extractPortFromContext(context),
                    ex
            );
        }

        // 认证相关异常
        if (isAuthenticationException(ex)) {
            return new EasyMailConnectionException(
                    "EMAIL_AUTH_FAILED",
                    "邮件服务器认证失败: " + message,
                    extractServerFromContext(context),
                    extractPortFromContext(context),
                    ex
            );
        }

        // 发送相关异常
        if (ex instanceof SendFailedException) {
            SendFailedException sendEx = (SendFailedException) ex;
            return new EasyMailSendException(
                    "EMAIL_SEND_FAILED",
                    "邮件发送失败: " + message + getInvalidAddressesInfo(sendEx),
                    context,
                    ex
            );
        }

        // 其他MessagingException
        return new EasyMailSendException(
                "EMAIL_MESSAGING_ERROR",
                "邮件处理异常: " + message,
                context,
                ex
        );
    }

    /**
     * 包装IOException
     *
     * @param ex      IOException
     * @param context 上下文信息
     * @return EmailException
     */
    public static EasyMailException wrapIOException(IOException ex, String context) {
        String message = ex.getMessage();

        if (ex instanceof ConnectException) {
            return new EasyMailConnectionException(
                    "EMAIL_CONNECTION_REFUSED",
                    "连接被拒绝: " + message,
                    extractServerFromContext(context),
                    extractPortFromContext(context),
                    ex
            );
        }

        if (ex instanceof SocketTimeoutException) {
            return new EasyMailConnectionException(
                    "EMAIL_CONNECTION_TIMEOUT",
                    "连接超时: " + message,
                    extractServerFromContext(context),
                    extractPortFromContext(context),
                    ex
            );
        }

        if (ex instanceof UnknownHostException) {
            return new EasyMailConnectionException(
                    "EMAIL_UNKNOWN_HOST",
                    "未知主机: " + message,
                    extractServerFromContext(context),
                    null,
                    ex
            );
        }

        return new EasyMailException(
                "EMAIL_IO_ERROR",
                "IO异常: " + message,
                context,
                ex
        );
    }

    /**
     * 包装RuntimeException
     *
     * @param ex      RuntimeException
     * @param context 上下文信息
     * @return EmailException
     */
    private static EasyMailException wrapRuntimeException(RuntimeException ex, String context) {
        String message = ex.getMessage();

        if (ex instanceof IllegalArgumentException) {
            return new EasyMailValidationException(
                    "EMAIL_INVALID_ARGUMENT",
                    "参数无效: " + message,
                    null
            );
        }

        if (ex instanceof NullPointerException) {
            return new EasyMailValidationException(
                    "EMAIL_NULL_POINTER",
                    "空指针异常: " + message,
                    null
            );
        }

        return new EasyMailException(
                "EMAIL_RUNTIME_ERROR",
                "运行时异常: " + message,
                context,
                ex
        );
    }

    /**
     * 判断是否为连接异常
     *
     * @param ex MessagingException
     * @return 是否为连接异常
     */
    private static boolean isConnectionException(MessagingException ex) {
        String message = ex.getMessage();
        if (message == null) {
            return false;
        }

        String lowerMessage = message.toLowerCase();
        return lowerMessage.contains("connection") ||
                lowerMessage.contains("connect") ||
                lowerMessage.contains("timeout") ||
                lowerMessage.contains("refused") ||
                lowerMessage.contains("unreachable");
    }

    /**
     * 判断是否为认证异常
     *
     * @param ex MessagingException
     * @return 是否为认证异常
     */
    private static boolean isAuthenticationException(MessagingException ex) {
        String message = ex.getMessage();
        if (message == null) {
            return false;
        }

        String lowerMessage = message.toLowerCase();
        return lowerMessage.contains("authentication") ||
                lowerMessage.contains("login") ||
                lowerMessage.contains("password") ||
                lowerMessage.contains("username") ||
                lowerMessage.contains("credential") ||
                lowerMessage.contains("535") || // SMTP认证失败代码
                lowerMessage.contains("534"); // SMTP认证失败代码
    }

    /**
     * 获取无效地址信息
     *
     * @param sendEx SendFailedException
     * @return 无效地址信息
     */
    private static String getInvalidAddressesInfo(SendFailedException sendEx) {
        StringBuilder sb = new StringBuilder();

        Address[] invalid = sendEx.getInvalidAddresses();
        if (invalid != null && invalid.length > 0) {
            sb.append(", 无效地址: ");
            for (int i = 0; i < invalid.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(invalid[i].toString());
            }
        }

        Address[] validUnsent = sendEx.getValidUnsentAddresses();
        if (validUnsent != null && validUnsent.length > 0) {
            sb.append(", 未发送地址: ");
            for (int i = 0; i < validUnsent.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(validUnsent[i].toString());
            }
        }

        return sb.toString();
    }

    /**
     * 从上下文中提取服务器地址
     *
     * @param context 上下文信息
     * @return 服务器地址
     */
    private static String extractServerFromContext(String context) {
        if (context == null) {
            return null;
        }

        // 简单的服务器地址提取逻辑
        if (context.contains("服务器:")) {
            String[] parts = context.split("服务器:");
            if (parts.length > 1) {
                return parts[1].split(",")[0].trim();
            }
        }

        return null;
    }

    /**
     * 从上下文中提取端口号
     *
     * @param context 上下文信息
     * @return 端口号
     */
    private static Integer extractPortFromContext(String context) {
        if (context == null) {
            return null;
        }

        // 简单的端口号提取逻辑
        if (context.contains("端口:")) {
            try {
                String[] parts = context.split("端口:");
                if (parts.length > 1) {
                    String portPart = parts[1].split(",")[0].trim();
                    return Integer.parseInt(portPart);
                }
            } catch (NumberFormatException e) {
                // 忽略解析错误
            }
        }

        return null;
    }

    /**
     * 验证邮箱地址格式
     *
     * @param email 邮箱地址
     * @return 是否有效
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * 验证邮箱地址列表
     *
     * @param emails 邮箱地址列表
     * @return 无效的邮箱地址列表
     */
    public static List<String> validateEmails(List<String> emails) {
        List<String> invalidEmails = new ArrayList<>();

        if (emails != null) {
            for (String email : emails) {
                if (!isValidEmail(email)) {
                    invalidEmails.add(email);
                }
            }
        }

        return invalidEmails;
    }

    /**
     * 记录异常日志
     *
     * @param exception 异常
     * @param operation 操作描述
     */
    public static void logException(EasyMailException exception, String operation) {
        if (exception == null) {
            return;
        }

        log.error("[{}] 邮件操作异常: {}", operation, exception.getFullErrorMessage(), exception);

        // 记录详细的异常信息
        if (exception.getCause() != null) {
            log.debug("[{}] 原始异常: {}", operation, exception.getCause().getMessage(), exception.getCause());
        }
    }
}