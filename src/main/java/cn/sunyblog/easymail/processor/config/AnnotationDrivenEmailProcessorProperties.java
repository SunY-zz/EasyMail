package cn.sunyblog.easymail.processor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 注解驱动邮件处理器配置属性
 *
 * @author suny
 * @version 1.0
 * @date 2025/06/14
 */
@Data
@ConfigurationProperties(prefix = "annotation-driven-email-processor")
public class AnnotationDrivenEmailProcessorProperties {

    /**
     * 是否启用注解驱动邮件处理器
     */
    private boolean enabled = true;

    /**
     * 处理器扫描包路径
     */
    private ScanConfig scan = new ScanConfig();

    /**
     * 扫描配置
     */
    @Data
    public static class ScanConfig {

        /**
         * 扫描的包路径
         */
        private String[] packages = {};
    }
}