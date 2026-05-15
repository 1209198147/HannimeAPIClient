package com.shikou.config;

import java.io.File;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

/**
 * Hanime客户端全局配置
 */
@Data
@Builder
public class HanimeConfig {

    /** 默认Base URL */
    public static final String DEFAULT_BASE_URL = "https://hanime1.me/";

    /** 默认Base URL HK */
    public static final String DEFAULT_BASE_URL_HK = "https://hanimeone.me/";

    /** 默认User-Agent */
    public static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    /** 默认连接超时（秒） */
    public static final int DEFAULT_CONNECT_TIMEOUT = 15;

    /** 默认读取超时（秒） */
    public static final int DEFAULT_READ_TIMEOUT = 30;

    /** 默认写入超时（秒） */
    public static final int DEFAULT_WRITE_TIMEOUT = 15;

    /** 默认缓存大小（100MB） */
    public static final long DEFAULT_CACHE_SIZE = 100 * 1024 * 1024;

    /** 默认最大空闲连接数 */
    public static final int DEFAULT_MAX_IDLE_CONNECTIONS = 10;

    /** 默认连接保活时间（分钟） */
    public static final int DEFAULT_KEEP_ALIVE_MINUTES = 10;

    /** 下载连接超时（秒） */
    public static final int DOWNLOAD_CONNECT_TIMEOUT = 120;

    /** 下载读取超时（秒） */
    public static final int DOWNLOAD_READ_TIMEOUT = 120;

    /** 下载写入超时（秒） */
    public static final int DOWNLOAD_WRITE_TIMEOUT = 120;

    /** 下载缓冲区大小（字节） */
    public static final int DOWNLOAD_BUFFER_SIZE = 8192;

    /** 下载分段大小（10MB） */
    public static final int DOWNLOAD_CHUNK_SIZE = 10 * 1024 * 1024;

    /** 缓存目录（null 表示不启用缓存） */
    @Builder.Default
    private final String cacheDir = getDefaultCacheDir();

    /** 缓存大小（字节） */
    @Builder.Default
    private final long cacheSize = DEFAULT_CACHE_SIZE;

    /** 最大空闲连接数 */
    @Builder.Default
    private final int maxIdleConnections = DEFAULT_MAX_IDLE_CONNECTIONS;

    /** 连接保活时间（分钟） */
    @Builder.Default
    private final int keepAliveMinutes = DEFAULT_KEEP_ALIVE_MINUTES;

    /** 自定义Base URL */
    @Builder.Default
    private final String baseUrl = DEFAULT_BASE_URL;

    /** 自定义User-Agent */
    @Builder.Default
    private final String userAgent = DEFAULT_USER_AGENT;

    /** 连接超时（秒） */
    @Builder.Default
    private final int connectTimeout = DEFAULT_CONNECT_TIMEOUT;

    /** 读取超时（秒） */
    @Builder.Default
    private final int readTimeout = DEFAULT_READ_TIMEOUT;

    /** 写入超时（秒） */
    @Builder.Default
    private final int writeTimeout = DEFAULT_WRITE_TIMEOUT;

    /**
     * 创建默认配置
     */
    public static HanimeConfig defaultConfig() {
        return HanimeConfig.builder().build();
    }

    /**
     * 创建默认配置（香港）
     */
    public static HanimeConfig defaultConfigHK() {
        return HanimeConfig.builder().baseUrl(DEFAULT_BASE_URL_HK).build();
    }

    /**
     * 用户语言偏好
     * zht: 繁体中文
     * zhs: 简体中文
     */
    @Builder.Default
    private final String userLang = "zhs";

    /**
     * 获取默认缓存目录
     */
    private static String getDefaultCacheDir() {
        String tmpDir = System.getProperty("java.io.tmpdir");
        if (tmpDir == null) {
            return null;
        }
        return tmpDir + File.separator + "hanime_api_cache";
    }
}
