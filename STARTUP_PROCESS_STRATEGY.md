# 邮件服务启动时处理策略配置

## 概述

为了提高邮件服务的灵活性和性能，我们新增了启动时处理策略配置，允许用户根据实际需求选择如何处理启动时的旧未读邮件。

## 配置选项

### startup-process-strategy

在 `application.yml` 中的 `mail.imap.listener` 节点下配置：

```yaml
mail:
  imap:
    listener:
      startup-process-strategy: MARK_AS_READ_ONLY
```

### 可选策略

| 策略 | 描述 | 性能影响 | 适用场景 |
|------|------|----------|----------|
| `IGNORE_EXISTING` | 不处理旧邮件，只监听新邮件 | 无 | 只关心新邮件 |
| `MARK_AS_READ_ONLY` | 只标记旧邮件为已读，不处理内容 | 低 | 避免重复处理，节省资源 |
| `FULL_PROCESS` | 完全处理旧邮件（包括内容解析和业务处理） | 高 | 需要处理所有历史邮件 |

## 配置示例

### 1. 只监听新邮件（推荐用于生产环境重启）

```yaml
mail:
  imap:
    listener:
      startup-process-strategy: IGNORE_EXISTING
```

### 2. 标记旧邮件为已读但不处理内容（默认，平衡性能）

```yaml
mail:
  imap:
    listener:
      startup-process-strategy: MARK_AS_READ_ONLY
```

### 3. 完全处理所有旧邮件（适用于首次启动或需要处理历史邮件）

```yaml
mail:
  imap:
    listener:
      startup-process-strategy: FULL_PROCESS
```



## 性能优化建议

1. **生产环境重启**：使用 `IGNORE_EXISTING` 或 `MARK_AS_READ_ONLY`
2. **首次部署**：使用 `FULL_PROCESS` 处理历史邮件
3. **测试环境**：使用 `MARK_AS_READ_ONLY` 避免重复处理
4. **邮件量大的场景**：优先使用 `MARK_AS_READ_ONLY` 或 `IGNORE_EXISTING`

## 日志输出

不同策略会产生不同的日志输出：

- `IGNORE_EXISTING`: "配置为忽略现有未读邮件，跳过处理"
- `MARK_AS_READ_ONLY`: "开始标记X封未读邮件为已读"
- `FULL_PROCESS`: "开始处理X封未读邮件"

## 注意事项

1. 标记为已读的操作是不可逆的
2. `FULL_PROCESS` 策略在邮件量大时可能导致启动时间较长
3. 建议根据实际业务需求选择合适的策略
4. 可以通过日志监控处理进度和性能表现