# EasyMail IDE 配置自动提示说明

## 问题描述

在其他项目中引入 EasyMail 依赖后，在 IDEA 中编辑 `application.yml` 配置文件时，无法自动提示 EasyMail 的配置项，虽然配置依然能够正常读取和使用。

## 解决方案

通过创建 Spring Boot 配置元数据文件 `spring-configuration-metadata.json`，让 IDE 能够识别和自动提示自定义配置属性。

## 实现步骤

### 1. 创建配置元数据文件

在 `src/main/resources/META-INF/` 目录下创建 `spring-configuration-metadata.json` 文件，该文件包含：

- **groups**: 配置组定义，对应配置类
- **properties**: 具体的配置属性定义
- **hints**: 配置提示和建议值

### 2. 文件结构说明

```json
{
  "groups": [
    {
      "name": "mail.imap",
      "type": "cn.sunyblog.easymail.config.EasyMailConfig",
      "description": "EasyMail IMAP邮件监听配置"
    }
  ],
  "properties": [
    {
      "name": "mail.imap.server",
      "type": "java.lang.String",
      "sourceType": "cn.sunyblog.easymail.config.EasyMailConfig",
      "description": "IMAP邮件服务器地址，例如：imap.qq.com"
    }
  ],
  "hints": [
    {
      "name": "mail.imap.server",
      "values": [
        {
          "value": "imap.qq.com",
          "description": "QQ邮箱IMAP服务器"
        }
      ]
    }
  ]
}
```

### 3. 支持的配置项

#### IMAP 配置 (`mail.imap.*`)

- `mail.imap.server` - IMAP服务器地址
- `mail.imap.port` - IMAP服务器端口
- `mail.imap.protocol` - 邮件协议（imap/imaps）
- `mail.imap.username` - 邮件账户用户名
- `mail.imap.password` - 邮件账户密码或授权码
- `mail.imap.attachment-dir` - 附件存储目录
- `mail.imap.auto-start` - 是否自动启动

#### IMAP 连接配置 (`mail.imap.connection.*`)

- `mail.imap.connection.timeout` - 连接超时时间（毫秒）
- `mail.imap.connection.read-timeout` - 读取超时时间（毫秒）
- `mail.imap.connection.write-timeout` - 写入超时时间（毫秒）

#### IMAP 监控配置 (`mail.imap.monitor.*`)

- `mail.imap.monitor.idle-timeout` - IDLE状态超时时间（毫秒）
- `mail.imap.monitor.keep-alive-interval` - 保持连接间隔（秒）
- `mail.imap.monitor.reconnect-delay` - 重连延迟（秒）
- `mail.imap.monitor.short-delay` - 短延迟时间（秒）
- `mail.imap.monitor.long-delay` - 长延迟时间（秒）
- `mail.imap.monitor.task-timeout` - 任务超时时间（秒）

#### IMAP 监听配置 (`mail.imap.listener.*`)

- `mail.imap.listener.max-retries` - 最大重试次数

#### IMAP 日志配置 (`mail.imap.log.*`)

- `mail.imap.log.debug-enabled` - 是否启用调试日志

#### SMTP 配置 (`mail.smtp.*`)

- `mail.smtp.server` - SMTP服务器地址
- `mail.smtp.port` - SMTP服务器端口
- `mail.smtp.protocol` - 邮件协议（smtp/smtps）
- `mail.smtp.username` - SMTP账户用户名
- `mail.smtp.password` - SMTP账户密码或授权码

#### SMTP 连接配置 (`mail.smtp.connection.*`)

- `mail.smtp.connection.timeout` - SMTP连接超时时间（毫秒）
- `mail.smtp.connection.read-timeout` - SMTP读取超时时间（毫秒）
- `mail.smtp.connection.write-timeout` - SMTP写入超时时间（毫秒）

#### SMTP 属性配置 (`mail.smtp.properties.*`)

- `mail.smtp.properties.mail-smtp-auth` - 是否启用SMTP认证
- `mail.smtp.properties.mail-smtp-starttls-enable` - 是否启用STARTTLS加密

#### SMTP 重试配置 (`mail.smtp.retry.*`)

- `mail.smtp.retry.enabled` - 是否启用重试机制
- `mail.smtp.retry.max-retries` - 最大重试次数
- `mail.smtp.retry.initial-delay-ms` - 初始重试延迟（毫秒）
- `mail.smtp.retry.max-delay-ms` - 最大重试延迟（毫秒）
- `mail.smtp.retry.use-exponential-backoff` - 是否使用指数退避策略
- `mail.smtp.retry.backoff-multiplier` - 退避乘数

#### SMTP 日志配置 (`mail.smtp.log.*`)

- `mail.smtp.log.debug-enabled` - 是否启用SMTP调试日志

### 4. 智能提示功能

配置元数据文件还提供了智能提示功能：

#### 服务器地址提示
- QQ邮箱：`imap.qq.com` / `smtp.qq.com`
- 网易163：`imap.163.com` / `smtp.163.com`
- Gmail：`imap.gmail.com` / `smtp.gmail.com`
- Outlook：`outlook.office365.com` / `smtp.office365.com`

#### 端口号提示
- IMAP：`143`（非加密）/ `993`（SSL加密）
- SMTP：`25`（非加密）/ `587`（STARTTLS）/ `465`（SSL加密）

#### 协议提示
- IMAP：`imap` / `imaps`
- SMTP：`smtp` / `smtps`

## 使用效果

配置元数据文件创建后，在 IDEA 中编辑 `application.yml` 时将获得以下功能：

1. **自动补全**：输入 `mail.` 后会自动提示可用的配置项
2. **类型检查**：IDE 会根据配置项的类型进行验证
3. **文档提示**：鼠标悬停在配置项上会显示详细说明
4. **值建议**：对于某些配置项，IDE 会提供预设的值选项
5. **错误检查**：配置项名称错误时会有红色波浪线提示

## 示例配置

```yaml
mail:
  imap:
    server: imap.qq.com          # IDE会提示常用邮箱服务器
    port: 993                    # IDE会提示常用端口号
    protocol: imaps              # IDE会提示可用协议
    username: your@email.com     # 用户名
    password: your-auth-code     # 授权码
    auto-start: true             # 布尔值会有true/false提示
    connection:
      timeout: 15000             # 数值类型会进行类型检查
      read-timeout: 30000
      write-timeout: 30000
    monitor:
      idle-timeout: 20000
      keep-alive-interval: 240
      reconnect-delay: 15
    log:
      debug-enabled: false       # 布尔值提示
  
  smtp:
    server: smtp.163.com         # SMTP服务器提示
    port: 465                    # SMTP端口提示
    protocol: smtp               # 协议提示
    username: your@163.com
    password: your-auth-code
    properties:
      mail-smtp-auth: true       # 布尔值提示
      mail-smtp-starttls-enable: false
    retry:
      enabled: true
      max-retries: 3
      initial-delay-ms: 1000
      max-delay-ms: 10000
      use-exponential-backoff: true
      backoff-multiplier: 2.0
```

## 注意事项

1. **文件位置**：配置元数据文件必须放在 `src/main/resources/META-INF/` 目录下
2. **文件名称**：文件名必须是 `spring-configuration-metadata.json`
3. **JSON格式**：文件必须是有效的JSON格式，语法错误会导致IDE无法识别
4. **IDE重启**：创建或修改配置元数据文件后，可能需要重启IDE或重新导入项目
5. **依赖打包**：当项目作为依赖被其他项目引用时，这个文件会被包含在JAR包中，从而为使用方提供配置提示

## 验证方法

1. 在其他项目中引入 EasyMail 依赖
2. 打开 `application.yml` 文件
3. 输入 `mail.` 查看是否有自动提示
4. 输入完整的配置项名称，查看是否有文档提示
5. 输入错误的配置项名称，查看是否有错误提示

通过这种方式，可以大大提升 EasyMail 框架的使用体验，让开发者在配置时更加便捷和准确。