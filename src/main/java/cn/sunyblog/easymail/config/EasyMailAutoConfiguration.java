package cn.sunyblog.easymail.config;

import cn.sunyblog.easymail.annotation.EnableEasyMail;
import cn.sunyblog.easymail.mail.EasyMailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

import javax.annotation.PostConstruct;
import java.util.Map;

/**
 * EasyMail自动配置类
 *
 * <p>此配置类是EasyMail框架的核心配置入口，通过{@link EnableEasyMail}注解导入。</p>
 * <p>根据注解配置自动导入相应的子配置类，并提供统一的配置管理。</p>
 *
 * @author suny
 * @see EnableEasyMail
 * @since 1.0.0
 */
@Slf4j
@Configuration
@Import({
        EasyMailListenerAutoConfiguration.class,
        EasyMailSenderAutoConfiguration.class,
        EasyMailConfigCompatibilityAutoConfiguration.class,
        EasyMailThreadPoolConfig.class
})
public class EasyMailAutoConfiguration implements ImportAware {

    private AnnotationAttributes enableEasyMailAttributes;

    @Autowired(required = false)
    private EasyMailService easyMailService;

    @Override
    public void setImportMetadata(AnnotationMetadata importMetadata) {
        Map<String, Object> attributeMap = importMetadata.getAnnotationAttributes(EnableEasyMail.class.getName());
        this.enableEasyMailAttributes = AnnotationAttributes.fromMap(attributeMap);
    }

    @PostConstruct
    public void init() {
        log.info("=== EasyMail 邮件服务框架启动 ===");

        if (enableEasyMailAttributes != null) {
            boolean autoStart = enableEasyMailAttributes.getBoolean("autoStart");
            boolean enableSender = enableEasyMailAttributes.getBoolean("enableSender");
            boolean enableListener = enableEasyMailAttributes.getBoolean("enableListener");
            boolean enableProcessor = enableEasyMailAttributes.getBoolean("enableProcessor");
            String[] scanPackages = enableEasyMailAttributes.getStringArray("scanPackages");
            String configPrefix = enableEasyMailAttributes.getString("configPrefix");

            log.info("EasyMail配置信息:");
            log.info("  - 自动启动: {}", autoStart);
            log.info("  - 启用发送服务: {}", enableSender);
            log.info("  - 启用监听服务: {}", enableListener);
            log.info("  - 启用处理器: {}", enableProcessor);
            log.info("  - 扫描包路径: {}", scanPackages.length > 0 ? String.join(", ", scanPackages) : "默认扫描所有包");
            log.info("  - 配置前缀: {}", configPrefix);

            // 如果启用自动启动且邮件服务可用，则启动邮件监听
            if (autoStart && easyMailService != null) {
                try {
                    // 设置自动启动标志
                    easyMailService.setAutoStart(true);
                    // 调用初始化方法，这会根据autoStart标志决定是否启动
                    easyMailService.init();
                    log.info("EasyMail邮件服务已自动启动");
                } catch (Exception e) {
                    log.warn("EasyMail邮件服务自动启动失败，可能需要检查配置: {}", e.getMessage());
                    log.debug("详细错误信息:", e);
                }
            } else if (!autoStart) {
                log.info("EasyMail邮件服务已配置但未自动启动，需要手动调用startMailMonitoring()方法");
            }
        }

        log.info("=== EasyMail 邮件服务框架配置完成 ===");
    }

    /**
     * 配置EasyMail启动器
     * 提供程序化的服务控制接口
     */
    @Bean
    @ConditionalOnBean(EasyMailService.class)
    public EasyMailStarter easyMailStarter(EasyMailService easyMailService) {
        return new EasyMailStarter(easyMailService, enableEasyMailAttributes);
    }

    /**
     * EasyMail启动器
     * 提供便捷的服务控制方法
     */
    public static class EasyMailStarter {

        private final EasyMailService easyMailService;
        private final AnnotationAttributes attributes;

        public EasyMailStarter(EasyMailService easyMailService, AnnotationAttributes attributes) {
            this.easyMailService = easyMailService;
            this.attributes = attributes;
        }

        /**
         * 手动启动邮件服务
         *
         * @return 是否启动成功
         */
        public boolean start() {
            log.info("手动启动EasyMail邮件服务");
            return easyMailService.startMailMonitoring();
        }

        /**
         * 停止邮件服务
         */
        public void stop() {
            log.info("停止EasyMail邮件服务");
            easyMailService.stopMailMonitoring();
        }

        /**
         * 重启邮件服务
         *
         * @return 是否重启成功
         */
        public boolean restart() {
            log.info("重启EasyMail邮件服务");
            stop();
            try {
                Thread.sleep(2000); // 等待2秒确保完全停止
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return start();
        }

        /**
         * 检查服务状态
         *
         * @return 是否正在运行
         */
        public boolean isRunning() {
            return easyMailService.isMailServiceRunning();
        }

        /**
         * 获取配置信息
         *
         * @return 配置属性
         */
        public AnnotationAttributes getConfiguration() {
            return attributes;
        }

        /**
         * 获取邮件服务实例
         *
         * @return EasyMailService实例
         */
        public EasyMailService getEasyMailService() {
            return easyMailService;
        }
    }
}