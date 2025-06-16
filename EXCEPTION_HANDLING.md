# 邮件异常处理系统

本文档介绍了EasyMail Spring Boot Starter中新增的异常处理系统，提供了完整的异常分类、处理和错误信息管理机制。

## 异常体系结构

### 基础异常类

#### EmailException
- **包路径**: `cn.sunyblog.easymail.exception.EmailException`
- **作用**: 所有邮件相关异常的基类
- **特性**:
  - 继承自 `RuntimeException`
  - 包含错误代码 (`errorCode`) 和详细错误信息 (`errorDetail`)
  - 提供 `getFullErrorMessage()` 方法获取完整错误描述

### 专用异常类

#### 1. EmailValidationException
- **错误代码**: `EMAIL_VALIDATION_ERROR`
- **用途**: 邮件内容和格式验证错误
- **常见场景**:
  - 收件人列表为空
  - 邮件主题为空
  - 邮件内容为空
  - 邮箱地址格式无效
  - 附件文件不存在或无法读取

**静态工厂方法**:
```java
// 空收件人
EmailValidationException.emptyRecipients()

// 空主题
EmailValidationException.emptySubject()

// 空内容
EmailValidationException.emptyContent()

// 无效邮箱地址
EmailValidationException.invalidEmailAddress(String email)

// 缺失附件
EmailValidationException.missingAttachment(String filePath)
```

#### 2. EmailSendException
- **错误代码**: `EMAIL_SEND_ERROR`
- **用途**: 邮件发送过程中的错误
- **常见场景**:
  - SMTP连接失败
  - 认证失败
  - 邮件被拒绝
  - 网络超时

#### 3. EmailConnectionException
- **错误代码**: `EMAIL_CONNECTION_ERROR`
- **用途**: SMTP连接和认证相关错误
- **特殊字段**:
  - `serverAddress`: SMTP服务器地址
  - `port`: SMTP端口号

#### 4. EmailTemplateException
- **错误代码**: `EMAIL_TEMPLATE_ERROR`
- **用途**: 邮件模板处理错误
- **特殊字段**:
  - `templateId`: 模板ID
- **常见场景**:
  - 模板不存在
  - 模板解析错误
  - 模板变量缺失
  - 模板渲染失败

**静态工厂方法**:
```java
// 模板不存在
EmailTemplateException.templateNotFound(String templateId)

// 模板解析错误
EmailTemplateException.parsingError(String templateId, Exception cause)

// 缺失模板变量
EmailTemplateException.missingVariables(String templateId, List<String> missingVars)

// 模板渲染错误
EmailTemplateException.renderError(String templateId, Exception cause)
```

#### 5. EmailConfigException
- **错误代码**: `EMAIL_CONFIG_ERROR`
- **用途**: 邮件配置相关错误
- **特殊字段**:
  - `configKey`: 配置键
  - `configValue`: 配置值

**静态工厂方法**:
```java
// 缺失配置
EmailConfigException.missingConfig(String configKey)

// 无效配置
EmailConfigException.invalidConfig(String configKey, String configValue)

// 无效SMTP服务器配置
EmailConfigException.invalidSmtpServer(String server, int port)

// 无效认证配置
EmailConfigException.invalidAuthentication(String username)
```

#### 6. EmailProcessException
- **错误代码**: `EMAIL_PROCESS_ERROR`
- **用途**: 邮件接收、解析和处理错误
- **特殊字段**:
  - `messageId`: 邮件ID
  - `handlerName`: 处理器名称

**静态工厂方法**:
```java
// 邮件解析错误
EmailProcessException.parsingError(String messageId, Exception cause)

// 处理器执行错误
EmailProcessException.handlerExecutionError(String handlerName, String messageId, Exception cause)

// 处理器未找到
EmailProcessException.handlerNotFound(String handlerName, String messageId)
```

## 异常处理工具类

### EmailExceptionHandler
- **包路径**: `cn.sunyblog.easymail.exception.EmailExceptionHandler`
- **作用**: 提供异常包装、邮箱验证和日志记录功能

**主要方法**:
```java
// 包装MessagingException
EmailException wrapMessagingException(MessagingException e, String context)

// 包装IOException
EmailException wrapIOException(IOException e, String context)

// 包装通用异常
EmailException wrapException(Exception e, String context)

// 验证邮箱地址格式
boolean isValidEmail(String email)

// 记录异常日志
void logException(EmailException e, String operation)
```

## 使用示例

### 1. 在邮件发送服务中使用

```java
@Service
public class EmailService {
    
    public SendResult sendEmail(EmailRequest request) {
        try {
            // 验证请求并抛出异常
            request.validateAndThrow();
            
            // 发送邮件逻辑
            return doSendEmail(request);
            
        } catch (EmailException e) {
            // 已经是EmailException，直接处理
            EmailExceptionHandler.logException(e, "邮件发送");
            return SendResult.failure(request.getToList(), request.getSubject(), 
                    e.getFullErrorMessage(), e.getErrorCode(), duration);
                    
        } catch (Exception e) {
            // 包装其他异常
            EmailException wrappedException = 
                    EmailExceptionHandler.wrapException(e, "邮件发送");
            EmailExceptionHandler.logException(wrappedException, "邮件发送");
            return SendResult.failure(request.getToList(), request.getSubject(), 
                    wrappedException.getFullErrorMessage(), wrappedException.getErrorCode(), duration);
        }
    }
}
```

### 2. 在模板处理中使用

```java
@Component
public class EmailTemplateProcessor {
    
    public String processTemplate(String templateId, Map<String, Object> variables) {
        try {
            EmailTemplate template = getTemplate(templateId);
            if (template == null) {
                throw EmailTemplateException.templateNotFound(templateId);
            }
            
            return template.render(variables);
            
        } catch (TemplateRenderException e) {
            throw EmailTemplateException.renderError(templateId, e);
        }
    }
}
```

### 3. 在配置验证中使用

```java
@Configuration
public class EmailConfigValidator {
    
    @PostConstruct
    public void validateConfig() {
        if (smtpHost == null || smtpHost.trim().isEmpty()) {
            throw EmailConfigException.missingConfig("smtp.host");
        }
        
        if (smtpPort < 1 || smtpPort > 65535) {
            throw EmailConfigException.invalidConfig("smtp.port", String.valueOf(smtpPort));
        }
    }
}
```

## 错误代码参考

| 异常类型 | 错误代码 | 描述 |
|---------|---------|------|
| EmailValidationException | EMAIL_VALIDATION_ERROR | 邮件验证错误 |
| EmailSendException | EMAIL_SEND_ERROR | 邮件发送错误 |
| EmailConnectionException | EMAIL_CONNECTION_ERROR | 连接错误 |
| EmailTemplateException | EMAIL_TEMPLATE_ERROR | 模板错误 |
| EmailConfigException | EMAIL_CONFIG_ERROR | 配置错误 |
| EmailProcessException | EMAIL_PROCESS_ERROR | 处理错误 |

## 最佳实践

### 1. 异常处理原则
- 在最接近错误源的地方抛出具体的异常类型
- 使用静态工厂方法创建常见异常
- 在服务边界处统一处理和包装异常
- 记录详细的错误日志用于调试

### 2. 错误信息设计
- 提供清晰、具体的错误描述
- 包含足够的上下文信息
- 避免暴露敏感信息（如密码）
- 支持国际化（如需要）

### 3. 异常链保持
- 始终保持原始异常作为cause
- 在包装异常时添加有用的上下文信息
- 避免丢失重要的堆栈跟踪信息

### 4. 性能考虑
- 异常创建有性能开销，避免在正常流程中使用
- 合理使用异常缓存（如果适用）
- 在高频操作中优先使用返回值而非异常

## 集成测试

项目包含完整的异常处理测试用例，位于：
`src/test/java/cn/sunyblog/easymail/exception/EmailExceptionTest.java`

运行测试：
```bash
mvn test -Dtest=EmailExceptionTest
```

## 扩展指南

### 添加新的异常类型
1. 继承 `EmailException` 基类
2. 定义特定的错误代码
3. 添加相关的字段和构造方法
4. 实现静态工厂方法（如需要）
5. 更新 `EmailExceptionHandler` 的包装逻辑
6. 添加相应的测试用例

### 自定义错误处理
可以通过实现自定义的异常处理器来扩展错误处理逻辑：

```java
@Component
public class CustomEmailExceptionHandler {
    
    public void handleEmailException(EmailException e) {
        // 自定义处理逻辑
        // 例如：发送告警、记录指标等
    }
}
```

这个异常处理系统为EasyMail提供了完整、类型安全的错误处理机制，有助于提高系统的可靠性和可维护性。