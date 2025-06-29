package cn.sunyblog.easymail.config;

import cn.sunyblog.easymail.util.EasyMailSSLTrustUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * SSL信任初始化器 - 在应用启动时配置SSL信任
 */
@Slf4j
@Configuration
public class EasyMailSSLTrustInitializer {

    @PostConstruct
    public void init() {
        log.debug("初始化SSL信任设置...");
        EasyMailSSLTrustUtil.trustAllCertificates();
    }
}