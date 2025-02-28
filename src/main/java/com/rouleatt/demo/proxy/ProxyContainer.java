package com.rouleatt.demo.proxy;

import com.rouleatt.demo.utils.EnvLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ProxyContainer {

    private static final int PROXY_PORT = Integer.parseInt(EnvLoader.get("PROXY_PORT"));
    private static final String PROXY_USERNAME = EnvLoader.get("PROXY_USERNAME");
    private static final String PROXY_PASSWORD = EnvLoader.get("PROXY_PASSWORD");

    public static final List<ProxyConfig> PROXY_CONFIGS = new ArrayList<>();

    public ProxyContainer() {
        for (int i = 1; i <= 17; i++) { // 대한민국 행정구역 17개
            PROXY_CONFIGS.add(new ProxyConfig(
                    EnvLoader.get("PROXY_IP_" + i),
                    PROXY_PORT,
                    PROXY_USERNAME,
                    PROXY_PASSWORD)
            );
        }
    }

    private static final AtomicInteger PROXY_INDEX = new AtomicInteger(0); // 안전한 라운드 로빈

    public static ProxyConfig getNextProxyConfig() {

        return PROXY_CONFIGS.get(PROXY_INDEX.getAndIncrement() % PROXY_CONFIGS.size());
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
    }
}
