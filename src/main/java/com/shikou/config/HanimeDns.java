package com.shikou.config;

import okhttp3.Dns;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自定义 DNS 解析器
 * <p>对特定域名使用内置 IP 映射，其余域名委托系统 DNS 解析</p>
 */
public class HanimeDns implements Dns {

    private final Map<String, List<InetAddress>> customMappings;

    /**
     * 使用默认映射创建 DNS 解析器
     */
    public HanimeDns() {
        this(getDefaultMappings());
    }

    /**
     * 使用自定义映射创建 DNS 解析器
     *
     * @param customHosts 域名→IP 映射
     */
    public HanimeDns(Map<String, String> customHosts) {
        this.customMappings = new HashMap<>();
        if (customHosts != null) {
            for (Map.Entry<String, String> entry : customHosts.entrySet()) {
                try {
                    InetAddress addr = InetAddress.getByName(entry.getValue());
                    customMappings.put(entry.getKey(), Collections.singletonList(addr));
                } catch (UnknownHostException e) {
                    throw new IllegalArgumentException("无效的IP地址: " + entry.getValue(), e);
                }
            }
        }
    }

    /**
     * 获取默认的内置 DNS 映射
     */
    private static Map<String, String> getDefaultMappings() {
        Map<String, String> mappings = new HashMap<>();
        mappings.put("hanimeone.me", "104.21.43.14");
        return mappings;
    }

    @Override
    public List<InetAddress> lookup(String hostname) throws UnknownHostException {
        List<InetAddress> custom = customMappings.get(hostname);
        if (custom != null) {
            return custom;
        }
        return Dns.SYSTEM.lookup(hostname);
    }
}
