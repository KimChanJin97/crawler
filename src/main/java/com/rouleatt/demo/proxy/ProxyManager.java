package com.rouleatt.demo.proxy;

import com.rouleatt.demo.utils.EnvLoader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ProxyManager {

    private static final String PROXY_A_IP = EnvLoader.get("PROXY_A_IP");
    private static final String PROXY_B_IP = EnvLoader.get("PROXY_B_IP");
    private static final String PROXY_C_IP = EnvLoader.get("PROXY_C_IP");
    private static final String PROXY_D_IP = EnvLoader.get("PROXY_D_IP");
    private static final String PROXY_E_IP = EnvLoader.get("PROXY_E_IP");
    private static final String PROXY_F_IP = EnvLoader.get("PROXY_F_IP");
    private static final String PROXY_G_IP = EnvLoader.get("PROXY_G_IP");
    private static final String PROXY_H_IP = EnvLoader.get("PROXY_H_IP");
    private static final int PROXY_PORT = Integer.parseInt(EnvLoader.get("PROXY_PORT"));
    private static final String PROXY_USERNAME = EnvLoader.get("PROXY_USERNAME");
    private static final String PROXY_PASSWORD = EnvLoader.get("PROXY_PASSWORD");

    public static final List<ProxyConfig> PROXY_CONFIGS = Arrays.asList(
            new ProxyConfig(PROXY_A_IP, PROXY_PORT, PROXY_USERNAME, PROXY_PASSWORD),
            new ProxyConfig(PROXY_B_IP, PROXY_PORT, PROXY_USERNAME, PROXY_PASSWORD),
            new ProxyConfig(PROXY_C_IP, PROXY_PORT, PROXY_USERNAME, PROXY_PASSWORD),
            new ProxyConfig(PROXY_D_IP, PROXY_PORT, PROXY_USERNAME, PROXY_PASSWORD),
            new ProxyConfig(PROXY_E_IP, PROXY_PORT, PROXY_USERNAME, PROXY_PASSWORD),
            new ProxyConfig(PROXY_F_IP, PROXY_PORT, PROXY_USERNAME, PROXY_PASSWORD),
            new ProxyConfig(PROXY_G_IP, PROXY_PORT, PROXY_USERNAME, PROXY_PASSWORD),
            new ProxyConfig(PROXY_H_IP, PROXY_PORT, PROXY_USERNAME, PROXY_PASSWORD)
    );

    private static final AtomicInteger proxyIndex = new AtomicInteger(0); // 안전한 라운드 로빈

    public static ProxyConfig getNextProxyConfig() {
        return PROXY_CONFIGS.get(proxyIndex.getAndIncrement() % PROXY_CONFIGS.size());
    }

    public static class ProxyConfig {
        public final String ip;
        public final int port;
        public final String username;
        public final String password;

        public ProxyConfig(String ip, int port, String username, String password) {
            this.ip = ip;
            this.port = port;
            this.username = username;
            this.password = password;
        }

        public Proxy toProxy() {
            return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ip, port));
        }
    }
}
