package cn.sunyblog.javaemaildemo.util;

import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLSocketFactory;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;

/**
 *  * @author suny
 *  * @version 1.0
 * SSL信任工具类 - 用于处理SSL证书验证
 *  * @date 2025/05/12 16:22
 */
@Slf4j
public class SSLTrustUtil {

    // 创建一个静态的SSLSocketFactory实例，可以在JavaMail中使用
    public static final SSLSocketFactory TRUST_ALL_SOCKET_FACTORY;

    // 创建一个静态的TrustManager数组
    private static final TrustManager[] TRUST_ALL_CERTS = new TrustManager[]{
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }
    };

    // 静态初始化块，创建SSLSocketFactory
    static {
        SSLSocketFactory factory = null;
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, TRUST_ALL_CERTS, new SecureRandom());
            factory = sslContext.getSocketFactory();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            log.error("初始化SSL工厂失败: {}", e.getMessage(), e);
        }
        TRUST_ALL_SOCKET_FACTORY = factory;
    }

    /**
     * 初始化全局SSL信任管理器，信任所有证书
     */
    public static void trustAllCertificates() {
        try {
            // 创建SSLContext并初始化
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, TRUST_ALL_CERTS, new SecureRandom());

            // 设置默认的SSLSocketFactory
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

            // 设置默认的HostnameVerifier
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

            // 设置JavaMail特定的系统属性
            System.setProperty("mail.smtp.ssl.trust", "*");
            System.setProperty("mail.imap.ssl.trust", "*");
            System.setProperty("mail.pop3.ssl.trust", "*");

            // 禁用证书验证
            System.setProperty("com.sun.net.ssl.checkRevocation", "false");
            Security.setProperty("ocsp.enable", "false");

            // 设置JavaMail使用自定义SSLSocketFactory
            System.setProperty("mail.imap.ssl.socketFactory.class", "cn.sunyblog.javaemaildemo.util.SSLTrustUtil$TrustAllSSLSocketFactory");
            System.setProperty("mail.smtp.ssl.socketFactory.class", "cn.sunyblog.javaemaildemo.util.SSLTrustUtil$TrustAllSSLSocketFactory");

            // 设置全局SSL上下文
            SSLContext.setDefault(sslContext);

            log.info("已全局配置SSL信任所有证书");
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            log.error("配置SSL信任管理器失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 自定义SSLSocketFactory，信任所有证书
     */
    public static class TrustAllSSLSocketFactory extends SSLSocketFactory {
        @Override
        public Socket createSocket() throws IOException {
            return TRUST_ALL_SOCKET_FACTORY.createSocket();
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException {
            return TRUST_ALL_SOCKET_FACTORY.createSocket(host, port);
        }

        @Override
        public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
            return TRUST_ALL_SOCKET_FACTORY.createSocket(socket, host, port, autoClose);
        }

        @Override
        public String[] getDefaultCipherSuites() {
            return TRUST_ALL_SOCKET_FACTORY.getDefaultCipherSuites();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return TRUST_ALL_SOCKET_FACTORY.getSupportedCipherSuites();
        }

        @Override
        public Socket createSocket(String host, int port, java.net.InetAddress localAddress, int localPort) throws IOException {
            return TRUST_ALL_SOCKET_FACTORY.createSocket(host, port, localAddress, localPort);
        }

        @Override
        public Socket createSocket(java.net.InetAddress host, int port) throws IOException {
            return TRUST_ALL_SOCKET_FACTORY.createSocket(host, port);
        }

        @Override
        public Socket createSocket(java.net.InetAddress address, int port, java.net.InetAddress localAddress, int localPort) throws IOException {
            return TRUST_ALL_SOCKET_FACTORY.createSocket(address, port, localAddress, localPort);
        }
    }
}