//package cn.sunyblog.javaemaildemo;
//
//import cn.sunyblog.javaemaildemo.mail.EmailTemplate;
//import cn.sunyblog.javaemaildemo.mail.EmailTemplateManager;
//import lombok.extern.slf4j.Slf4j;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//
//import java.util.HashMap;
//import java.util.Map;
//
///**
// * 邮件模板测试类
// * 测试邮件模板管理和变量替换功能
// *
// * @author JavaEmailSpringBoot
// * @version 1.0.0
// */
//@Slf4j
//@SpringBootTest
//class EmailTemplateTest {
//
//    @Autowired
//    private EmailTemplateManager templateManager;
//
//    /**
//     * 测试模板注册和获取
//     */
//    @Test
//    public void testTemplateRegistration() {
//        log.info("=== 测试模板注册和获取 ===");
//
//        // 创建测试模板
//        EmailTemplate testTemplate = EmailTemplate.builder()
//                .templateId("test-registration")
//                .templateName("注册测试模板")
//                .subjectTemplate("欢迎注册 - ${siteName}")
//                .contentTemplate(""
//                        + "<html><body>"
//                        + "<h2>欢迎 ${username} 注册 ${siteName}!</h2>"
//                        + "<p>您的账号信息:</p>"
//                        + "<ul>"
//                        + "<li>用户名: ${username}</li>"
//                        + "<li>邮箱: ${email}</li>"
//                        + "<li>注册时间: ${registerTime}</li>"
//                        + "</ul>"
//                        + "<p>请点击以下链接激活账号:</p>"
//                        + "<a href='${activationLink}'>激活账号</a>"
//                        + "</body></html>")
//                .isHtml(true)
//                .description("用户注册欢迎邮件模板")
//                .version("1.0")
//                .createTime(System.currentTimeMillis())
//                .build();
//
//        // 注册模板
//        boolean registered = templateManager.registerTemplate(testTemplate);
//        log.info("模板注册结果: {}", registered ? "成功" : "失败");
//
//        // 获取模板
//        EmailTemplate retrieved = templateManager.getTemplate("test-registration");
//        log.info("模板获取结果: {}", retrieved != null ? "成功" : "失败");
//
//        if (retrieved != null) {
//            log.info("模板信息: ID={}, 名称={}, 描述={}",
//                    retrieved.getTemplateId(),
//                    retrieved.getTemplateName(),
//                    retrieved.getDescription());
//        }
//
//        // 列出所有模板
//        log.info("当前所有模板: {}", templateManager.getAllTemplates().keySet());
//    }
//
//    /**
//     * 测试变量替换功能
//     */
//    @Test
//    public void testVariableReplacement() {
//        log.info("=== 测试变量替换功能 ===");
//
//        // 准备测试变量
//        Map<String, Object> variables = new HashMap<>();
//        variables.put("username", "张三");
//        variables.put("siteName", "JavaEmail测试站点");
//        variables.put("email", "zhangsan@example.com");
//        variables.put("registerTime", "2024-01-15 10:30:00");
//        variables.put("activationLink", "https://example.com/activate?token=abc123");
//
//        // 获取模板
//        EmailTemplate template = templateManager.getTemplate("test-registration");
//        if (template == null) {
//            log.warn("模板不存在，先注册模板");
//            testTemplateRegistration();
//            template = templateManager.getTemplate("test-registration");
//        }
//
//        if (template != null) {
//            // 生成主题
//            String subject = template.generateSubject(variables);
//            log.info("生成的主题: {}", subject);
//
//            // 生成内容
//            String content = template.generateContent(variables);
//            log.info("生成的内容: {}", content);
//
//            // 测试静态方法
//            String testTemplate = "Hello ${name}, welcome to ${site}! Your score is ${score}.";
//            Map<String, Object> testVars = new HashMap<>();
//            testVars.put("name", "李四");
//            testVars.put("site", "测试网站");
//            testVars.put("score", 95);
//
//            String result = EmailTemplate.replaceVariables(testTemplate, testVars);
//            log.info("静态替换结果: {}", result);
//        }
//    }
//
//    /**
//     * 测试模板验证功能
//     */
//    @Test
//    public void testTemplateValidation() {
//        log.info("=== 测试模板验证功能 ===");
//
//        // 测试有效模板
//        EmailTemplate validTemplate = EmailTemplate.builder()
//                .templateId("valid-test")
//                .templateName("有效测试模板")
//                .subjectTemplate("测试主题")
//                .contentTemplate("测试内容")
//                .isHtml(false)
//                .build();
//
//        // 准备验证变量
//        Map<String, Object> validationVars = new HashMap<>();
//        validationVars.put("testVar", "testValue");
//
//        EmailTemplateManager.TemplateValidationResult validResult = templateManager.validateTemplate("valid-test", validationVars);
//        log.info("有效模板验证结果: {}", validResult.isValid() ? "通过" : "失败");
//
//        // 测试无效模板（缺少必要字段）
//        EmailTemplate invalidTemplate = EmailTemplate.builder()
//                .templateId("")
//                .templateName("无效测试模板")
//                .subjectTemplate("")
//                .contentTemplate("测试内容")
//                .build();
//
//        // 注册无效模板用于测试
//        templateManager.registerTemplate(invalidTemplate);
//        EmailTemplateManager.TemplateValidationResult invalidResult = templateManager.validateTemplate("", validationVars);
//        log.info("无效模板验证结果: {}", invalidResult.isValid() ? "通过" : "失败");
//    }
//
//    /**
//     * 测试模板更新功能
//     */
//    @Test
//    public void testTemplateUpdate() {
//        log.info("=== 测试模板更新功能 ===");
//
//        String templateId = "update-test";
//
//        // 创建初始模板
//        EmailTemplate originalTemplate = EmailTemplate.builder()
//                .templateId(templateId)
//                .templateName("更新测试模板")
//                .subjectTemplate("原始主题")
//                .contentTemplate("原始内容")
//                .isHtml(false)
//                .version("1.0")
//                .build();
//
//        templateManager.registerTemplate(originalTemplate);
//        log.info("原始模板已注册");
//
//        // 更新模板
//        EmailTemplate updatedTemplate = EmailTemplate.builder()
//                .templateId(templateId)
//                .templateName("更新测试模板 - 已更新")
//                .subjectTemplate("更新后的主题 - ${version}")
//                .contentTemplate("更新后的内容，版本: ${version}")
//                .isHtml(true)
//                .version("2.0")
//                .updateTime(System.currentTimeMillis())
//                .build();
//
//        boolean updateResult = templateManager.updateTemplate(updatedTemplate);
//        log.info("模板更新结果: {}", updateResult ? "成功" : "失败");
//
//        // 验证更新结果
//        EmailTemplate retrieved = templateManager.getTemplate(templateId);
//        if (retrieved != null) {
//            log.info("更新后模板信息: 名称={}, 版本={}, HTML={}",
//                    retrieved.getTemplateName(),
//                    retrieved.getVersion(),
//                    retrieved.isHtml());
//
//            // 测试更新后的变量替换
//            Map<String, Object> vars = new HashMap<>();
//            vars.put("version", "2.0");
//
//            String subject = retrieved.generateSubject(vars);
//            String content = retrieved.generateContent(vars);
//
//            log.info("更新后生成的主题: {}", subject);
//            log.info("更新后生成的内容: {}", content);
//        }
//    }
//
//    /**
//     * 测试模板删除功能
//     */
//    @Test
//    public void testTemplateRemoval() {
//        log.info("=== 测试模板删除功能 ===");
//
//        String templateId = "delete-test";
//
//        // 创建测试模板
//        EmailTemplate testTemplate = EmailTemplate.builder()
//                .templateId(templateId)
//                .templateName("删除测试模板")
//                .subjectTemplate("测试主题")
//                .contentTemplate("测试内容")
//                .build();
//
//        // 注册模板
//        templateManager.registerTemplate(testTemplate);
//        log.info("测试模板已注册");
//
//        // 验证模板存在
//        EmailTemplate retrieved = templateManager.getTemplate(templateId);
//        log.info("删除前模板存在: {}", retrieved != null);
//
//        // 删除模板
//        boolean deleteResult = templateManager.removeTemplate(templateId);
//        log.info("模板删除结果: {}", deleteResult ? "成功" : "失败");
//
//        // 验证模板已删除
//        EmailTemplate afterDelete = templateManager.getTemplate(templateId);
//        log.info("删除后模板存在: {}", afterDelete != null);
//    }
//
//    /**
//     * 测试模板统计功能
//     */
//    @Test
//    public void testTemplateStatistics() {
//        log.info("=== 测试模板统计功能 ===");
//
//        // 获取模板统计
//        String stats = templateManager.getTemplateStats();
//
//        log.info("模板统计信息: {}", stats);
//
//        // 获取所有模板信息
//        Map<String, EmailTemplate> allTemplates = templateManager.getAllTemplates();
//        log.info("当前模板总数: {}", allTemplates.size());
//
//        allTemplates.forEach((id, template) -> {
//            log.info("模板 {}: 名称={}, HTML={}, 版本={}",
//                    id,
//                    template.getTemplateName(),
//                    template.isHtml(),
//                    template.getVersion());
//        });
//    }
//
//    /**
//     * 测试复杂变量替换
//     */
//    @Test
//    public void testComplexVariableReplacement() {
//        log.info("=== 测试复杂变量替换 ===");
//
//        // 创建包含复杂变量的模板
//        EmailTemplate complexTemplate = EmailTemplate.builder()
//                .templateId("complex-test")
//                .templateName("复杂变量测试模板")
//                .subjectTemplate("${type} - ${title} (${priority})")
//                .contentTemplate(""
//                        + "<html><body>"
//                        + "<h1>${title}</h1>"
//                        + "<p>类型: ${type}</p>"
//                        + "<p>优先级: ${priority}</p>"
//                        + "<p>描述: ${description}</p>"
//                        + "<div style='background: ${bgColor}; padding: 10px;'>"
//                        + "<p>用户: ${user.name} (${user.email})</p>"
//                        + "<p>部门: ${user.department}</p>"
//                        + "</div>"
//                        + "<p>数量: ${count}</p>"
//                        + "<p>金额: ${amount}</p>"
//                        + "<p>时间: ${timestamp}</p>"
//                        + "</body></html>")
//                .isHtml(true)
//                .build();
//
//        templateManager.registerTemplate(complexTemplate);
//
//        // 准备复杂变量
//        Map<String, Object> complexVars = new HashMap<>();
//        complexVars.put("type", "系统通知");
//        complexVars.put("title", "重要系统更新");
//        complexVars.put("priority", "高");
//        complexVars.put("description", "系统将在今晚进行重要更新，请提前保存工作。");
//        complexVars.put("bgColor", "#f0f8ff");
//        complexVars.put("user.name", "王五");
//        complexVars.put("user.email", "wangwu@example.com");
//        complexVars.put("user.department", "技术部");
//        complexVars.put("count", 100);
//        complexVars.put("amount", 1234.56);
//        complexVars.put("timestamp", new java.util.Date());
//
//        // 生成内容
//        String subject = complexTemplate.generateSubject(complexVars);
//        String content = complexTemplate.generateContent(complexVars);
//
//        log.info("复杂模板生成的主题: {}", subject);
//        log.info("复杂模板生成的内容: {}", content);
//    }
//}